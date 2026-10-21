# Plan: the engine as a pure environment

The goal: replace the current `core` module with an engine that holds no state, knows no
policies, and is driven by one loop — and make learned players a first-class citizen of
that loop without teaching the rules about machine learning.

The target shape is described in [CONTEXT-MAP.md](../CONTEXT-MAP.md); the vocabulary is
in [CONTEXT.md](../CONTEXT.md); the decisions are in [`adr/`](./adr).

## How the migration is kept safe

The new engine is built **in a new Gradle module (`engine`) next to the old `core`**,
which is not touched until the front ends are switched. Two gates protect the move:

1. **The golden gate.** For the four-player game, the old engine and the new engine are
   run with the same seed and the same policy, and their **action logs are compared
   entry by entry**. Any difference is a bug in the new engine, not a difference in
   behaviour to be accepted. The existing golden scenarios (the exchange, "cannot beat →
   takes the pot", "beats → leads the next trick", the round end and the board) are
   ported to the new API with their expectations unchanged.
2. **The invariant gate.** Many games are run and checked for card conservation, one
   winner, termination and reproducibility.

The old module is deleted only after every front end runs on the new engine and the
golden gate is green.

### Two details that will silently break the golden gate

- **The round's safety bound counts turns, not decisions.** In the old engine a turn may
  play two cards (beat, then lead) and still counts once. The new engine takes one
  decision per step, so the counter must be incremented where the old one was — when a
  turn ends: after a lead, after taking the pot, and after a defence that emptied the
  hand. If it counts steps instead, the bound fires at a different point and the four
  pathological seeds (12129, 24847, 30649, 49128) produce different games.
- **The policy must be the same policy.** The old `AiDecisionStrategy` picks the
  weakest non-trump to lead and the first of the sorted legal defences, and the sort is
  stable, so the hand's order matters. The new `RuleBot` must reproduce it exactly, and
  the deck must be dealt in the same order, or the logs diverge for reasons that have
  nothing to do with the rules.

## Wave 1 — the engine (sequential)

Each task is one branch and one pull request. `engine` has no dependencies but the JDK.

### T1. The `engine` module and the value types

**Goal:** an empty module with the whole vocabulary in place.
**Touches:** `engine/build.gradle.kts`, `Card`, `RankSet`, `Setup`, `State`, `Seat`,
`Move`, `Transition`, `Event`, `Game` (stubbed: `start` throws, `legalMoves` returns an
empty list, `step` throws).
**Done when:** the module compiles with no dependencies declared, and the value types
are immutable (records, `List.copyOf` on the way in).

### T2. The setup, the deck and the boards

**Goal:** a setup produces the first state.
**Touches:** `RankSet` (SIX_TO_ACE, TWO_TO_ACE, `baseRank`, `cardCount(seats)`),
`Setup` (rank set, 2–4 distinct trump suits, seed, optional explicit deck order),
`Dealing` (build the deck from the suits in play, take one base card per suit onto the
seat's board, shuffle the rest with the ported `Rng`), the `rngState` inside `State`.
**Done when:** a four-player setup with the classic rank set produces 36 cards, 4 on the
boards and 32 dealt, and two players produce 18 cards, 2 on the boards and 16 dealt.

### T3. The obligatory exchange

**Goal:** the exchange rule, with no "nobody owns this suit" case.
**Touches:** `Exchange` — for every card of another seat's trump suit that equals the
next rank required by that seat's board, pass it to that seat; aces are never passed;
no chain.
**Done when:** the exchange runs once at the start of a round, card count is unchanged,
and every seat holds the base rank of its own trump.

### T4. The trick: legal moves and stepping

**Goal:** the core loop of the game.
**Touches:** `Trick` (`canBeat`, the legal moves for the seat to move: lead with any
card on a fresh trick, otherwise the cards that beat the top card, otherwise take the
pot), `leadRequired` in the state (a seat that has just beaten leads the next trick),
the round's **turn** counter.
**Done when:** the four cases of "what may I do now" are pinned by tests, an illegal
move is unrepresentable (there is no code path that accepts one), and the turn counter
increments exactly where the old engine's did.

### T5. The end of the round, the board and elimination

**Goal:** rounds end and games end.
**Touches:** `EndRound` (the seat still holding cards loses the round; its lowest trump
goes onto its board; the rest of its cards and the pot return to the deck; a completed
board eliminates the seat), the next round's setup, the game's end.
**Done when:** a round that ends hands the right card to the right board, a completed
board eliminates its seat, and the game ends with exactly one seat left.

### T6. Events and transitions

**Goal:** every step reports what happened.
**Touches:** `Event` (game started, round started, cards dealt, cards exchanged, card
played, card beaten, pot taken, round ended, seat eliminated, game ended) and the
`Transition` returned by `start` and `step`.
**Done when:** a front end can show the deal and the exchange from the transition alone,
and no event is stored in the state.

### T7. The golden scenarios, ported

**Goal:** the old behaviour is pinned in the new engine.
**Touches:** the five golden scenarios, rewritten against the new API with their
expectations unchanged, plus the golden gate itself (old engine vs new engine, same
seed, same policy, compare the action logs).
**Done when:** the golden gate is green for the four-player game across a wide seed
range, and the four pathological seeds still cap at the same point.

### T8. The invariants, ported

**Goal:** many games, checked.
**Touches:** a simulator over the new engine: card conservation, exactly one winner,
termination, reproducibility from the seed; the old `GameSimulatorTest` is the model.
**Done when:** the invariant suite is green over the same seed range the old one used.

## Wave 2 — the periphery (parallel, once T1–T5 are stable)

### T9. `bots`: the rule bot

**Goal:** the fixed policy, outside the rules.
**Touches:** a new module `bots` depending on `engine`; `RuleBot.choose(state, moves)`
reproducing the old heuristic exactly (weakest non-trump to lead; the first of the
sorted legal defences).
**Done when:** the golden gate passes with the rule bot on both sides, and the old
`AiDecisionStrategy` is deleted.

### T10. `ml`: the observation and the vector

**Goal:** what a learned player sees.
**Touches:** a new module `ml` depending on `engine`; `Observation` (the state as seen
from the seat about to move); `Vector.of(state, seat, moves)` producing a fixed-size
encoding plus a mask over the legal moves.
**Done when:** the same seat and state always produce the same vector, and the vector
contains no information the seat could not see.

### T11. `ml`: running the model

**Goal:** a trained model chooses legal moves.
**Touches:** `OnnxPolicy` — an ONNX Runtime session (single-threaded), the masked
maximum over the legal moves.
**Done when:** the policy is bit-reproducible (the same weights and the same state give
the same choice), and a model that is not present fails loudly rather than silently
choosing at random.

### T12. `tools/exporter`: training episodes

**Goal:** episodes for a Python trainer.
**Touches:** a new module `tools/exporter`; the episode format (setup, per step the
observation, the action and the legal moves, and the winner); a writer producing JSONL.
**Done when:** a written episode can be replayed to the same outcome, and the file
contains everything the trainer needs — so that Python never reimplements the rules.

### T13. `tools/verify` and `tools/eval`

**Goal:** the checks around the training loop.
**Touches:** `tools/verify` (many episodes, assert the invariants) and `tools/eval`
(a learned policy against the rule bot over a seed range, reporting the win rate and
asserting the invariants hold under a learned policy too).
**Done when:** a bad policy loses more often but never breaks a rule.

## Wave 3 — the front ends (parallel, once the engine and the bots are stable)

### T14. `console`

**Goal:** the text front end on the new engine.
**Touches:** the launcher, the input (the person picks an index into the legal moves),
the event rendering, and the i18n bundle and card localiser, which move out of `core`
into this module.
**Done when:** a full game is played in the console; the Russian bundle has no key
missing from the English one, and a test proves it.

### T15. `desktop-libgdx`

**Goal:** the desktop front end on the new engine.
**Touches:** the launcher, the listener (the events come from the transition), the
input provider (the click resolves to an index into the legal moves), the card names
(the short ASCII codes move into this module), and the highlight of the legal cards,
which the current front end never actually paints.
**Done when:** a full game is played on the desktop and the legal cards are visibly
marked.

### T16. `android`

**Goal:** the Android front end on the new engine.
**Touches:** the bridge (the game loop moves to the front end's thread and asks the
engine for a move instead of queueing cards), the assets that the app loads and that
are currently not in the repository, the strings (the game-over screen is in English
while the start screen is in Russian), and the unused scaffolding (the room DAO with no
database, the settings store nobody injects, the animations nobody calls).
**Done when:** a full game is played on a device, the cards are drawn, and the module
has no dependency that no code uses.

## Wave 4 — the cleanup

### T17. The old module is removed

**Goal:** one engine, not two.
**Touches:** delete the old `core` module and its packages, move `engine` into its place
under the name `core`, update the front ends' imports, drop the Gson dependency, and
delete the tests of the removed concepts (`GameDriverTest`, `GameDriverAsyncTest`,
`SyncAsyncEquivalenceTest`, the saved-game tests, the scripted-dealer tests).
**Done when:** the whole build is green, no module depends on a removed class, and the
rules module declares no dependency at all.

### T18. Repository hygiene

**Goal:** the things the audit found that are not part of the migration.
**Touches:** `local.properties` (tracked, holding one machine's SDK path), the
`.gitignore`, the root task files that are not project documentation, the four Russian
engineering documents under `docs/` (the project's own language rule asks for English
there), and the missing architectural map, which this plan and `CONTEXT-MAP.md` now
provide.
**Done when:** a fresh clone builds on another machine, and no engineering document is
in Russian.

## Risks

- **Determinism drift.** The two counters described above are the realistic way to lose
  bit-for-bit compatibility. The golden gate is the only reliable detector, so it runs
  on every change to the engine, over a wide seed range, not only on the golden seeds.
- **Two engines at once.** For the duration of waves 1–3 the repository holds two
  implementations of the rules. The risk is someone fixing the wrong one. The engine
  module is named differently, and wave 4 is short and scheduled.
- **The front ends drift from the engine's API.** Each front end is ported separately and
  the old API stays alive until all three are done, so a front end can be ported
  against a moving target. The engine's API is frozen from T6 onwards, except for
  additions.
