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

### Bit-for-bit requirements

The golden gate compares the old engine and the new engine move by move. The old
engine's behaviour is observable in ways that are not written down anywhere, and each of
them is enough to make the gate fail for a reason that has nothing to do with the rules.
These are the facts the new engine has to reproduce.

1. **The order of the random draws.** The old engine takes the base cards in the
   constructor (no draw), draws once in `startGame()` to pick the first dealer, and only
   then shuffles. The new `start(setup)` must consume the draws in the same order:
   first dealer, then shuffle. Shuffling before choosing the dealer shifts the whole
   stream.
2. **The deck is built in the canonical suit order.** The old `Dealer` builds the deck
   by iterating `Card.Suit.values()` and, within each suit, the ranks ascending. The
   seats are in seating order, but the *deck* is not built in the order the seats are
   listed. The new engine must build the deck in the canonical suit order whatever
   order the setup lists the trumps in, and the gate's setup must list the trumps in
   the canonical order too.
3. **The order of the cards in a hand is observable.** Cards are dealt one at a time
   from the seat to the left of the dealer, in seating order, and the cards passed in
   the exchange are *appended* to the receiver's hand. The rule bot's lead is the first
   card of the hand that minimises (is-trump, rank), so two cards of the same rank in
   different suits are told apart by their position in the hand. Deal order and
   exchange order are part of the behaviour.
4. **The order of the legal moves is observable.** The old engine lists the defences
   sorted by (is-trump, rank) with a stable sort, and the rule bot takes the first of
   them. The new engine must list the legal moves in that order for a defence, and in
   the hand's order for a lead, so that "take the first" reproduces the old choice.
5. **The safety bound counts turns, not decisions.** In the old engine a turn may play
   two cards (beat, then lead) and still counts once. The new engine takes one decision
   per step, so the counter must be incremented where the old one was — when a turn
   ends: after a lead, after taking the pot, and after a defence that emptied the
   hand. If it counts steps instead, the bound fires at a different point and the four
   pathological seeds (12129, 24847, 30649, 49128) produce different games.
6. **The gate compares events, not steps.** The old engine has no steps; its trace is
   its event stream (card played, card beaten, pot taken, round started, round ended,
   game ended), each naming the seat by its trump suit. The new engine's events must
   carry the same information, so the two traces can be compared one to one.
7. **The rank order.** The old `Card.Rank` carries a value counting from TWO = 2. The
   new `Card.Rank` has no value; its ordinal starts at TWO = 0. The two orderings are
   the same, but a number from one must never be compared with a number from the other:
   use the ordinal, or an accessor that adds two.
8. **No legal move means the game is over.** The state carries no phase. The engine
   must never leave a state in which the seat to move cannot act while the game is not
   over — a seat with an empty hand is out of the round, and the turn moves on.

Everything above is read off the old implementation, not invented. The generator is
ported verbatim — the same SplitMix64 constants and the same rejection sampling — not
re-implemented.

### What T1 established

- The module is `engine`, the package is `ru.anseranser.engine`, and the module's
  **main and runtime classpaths are empty** — JUnit is test-scoped only, and there is
  no Lombok.
- `Game` is a final class with a private constructor and three **static** methods:
  `start(Setup)`, `legalMoves(State)`, `step(State, int)`.
- `Move` is a sealed interface with `Move.Play(Card)` and `Move.TakePot()`, not a
  record with a kind and a nullable card.
- `Setup` validates its own shape: two to four distinct trump suits.
- `State` holds the seats, the pot, the deck, the dealer, the seat to move and the
  generator's position. The board belongs to the seat; the round counter and the
  "must lead" flag arrive with T4.

### What T2 established

- `Setup` has four components — rank set, trump suits, seed, and an optional explicit
  deck order — with a three-argument constructor for the ordinary case.
- **An explicit deck order replaces the deck and is not shuffled.** The generator is
  then asked only for the dealer, so the generator's position differs from the
  shuffled path.
- `Dealing` and `SplitMix64` are package-private in the engine. The generator has no
  interface: the seed is in the setup and nothing needs injecting.
- `Game.start` returns the state **before** the obligatory exchange; the exchange
  arrives with T3, and the events with T6.

Two traps that the next tasks inherit:

9. **The generator's position has no universal closed form.** The ported `nextInt`
   takes one draw per call *almost* always, but its rejection path can fire when the
   intermediate arithmetic overflows — roughly one draw in ten million. A formula like
   "the state after the start is seed + (1 + deck − 1) × GOLDEN" is therefore true for
   the seeds that were measured and false for a handful of others. Do not assert the
   formula across a seed range; compare traces instead.
10. **The old engine cannot deal two or three suits** — its deck always holds four, and
    its base cards are taken from all four suits. So the golden gate can only compare
    the **four-player** game against the old engine. The two- and three-player
    variants are new behaviour, defined by the rules and by ADR 0005, with no old
    implementation to compare against.

### What T3 established

- `Exchange` is a package-private rule; `Game.start` applies it after the deal and
  returns the state after the exchange.
- **A board is a list ordered bottom-up.** The top of the ladder is its **last**
  element. A card pushed onto a board in T5 must be **appended**, or the required rank
  will be computed from the wrong card.
- The exchange is idempotent — applying it twice changes nothing — so "once per round"
  is not observable in the returned state. The round setup must call it exactly once.
- `State.dealer` and `State.turn` hold `Seat` **values**. Every rule that rebuilds the
  seats must re-point them at the new instances; a stale reference silently keeps the
  old hand.
- The state does not carry the rank set, and it does not need to: the rank a board
  requires next is the rank after the top of the board, and over the whole rank enum
  that is the same answer the rank set gives, because the sets are contiguous. The
  exchange's rank-set parameter goes away with T5.

### The contract of the loop

`legalMoves(state)` is empty **only when the game is over**. A round that ends is
resolved inside the step that ended it: the loser's card goes onto the board, the
cards return to the deck, the next round is dealt and the exchange runs — all in one
step, with the events reporting what happened. The harness never sees an intermediate
"the round is over" state, and it never has to ask whether the round or the game ended.

This is why the round end (T5) is built **before** the trick (T4): the step needs
something to call when the last card of the round is played.

### What T5 established

- `EndRound.apply(State)` resolves a finished round in one call and returns a **state**,
  not a transition: the loser's card onto its board, the elimination, the cards back
  into the deck, the next dealer, the next round dealt and the exchange run — or the
  game-over state. `EndRound.resolve(State)` stops after the resolution.
- **One rule picks the loser**: the seat holding the most cards, ties broken by the
  seating order. When exactly one seat holds cards that is the seat the old engine
  picks, and when several do it is the old engine's capped-round answer — so the trick
  needs no flag for a capped round.
- A seat is eliminated when the card pushed onto its board is the ace **or** when the
  pile holds no trump of its suit at all, because its ladder is then complete.
- The next round is dealt from the cards that came back, **on top of the hands that
  are left**, skipping the eliminated seats, starting at the seat to the left of the
  dealer, and continuing the same generator stream.
- `Exchange.apply(State)` no longer takes a rank set; `Ladders.nextRequiredRank`
  computes it over the whole rank enum, as the old `Scoreboard` does. `Seats` holds
  the seating queries.
- The trick (T4) adds two fields to `State` — the "must lead" flag and the round's
  turn counter — and every `new State(...)` in the engine then has to carry them:
  `Dealing`, `Exchange` and `EndRound`.
- For the events (T6): a rule that returns only a state leaves the events to be
  derived — the loser is the seat whose board grew, and the pushed card is the last
  card of that board. T6 either derives them that way or widens the seam.

### What T4 established

- `Trick` holds the trick rule; `Game.legalMoves` and `Game.step` delegate to it. The
  step returns the new state, and the events stay T6's business.
- `State` gained two fields: `leadRequired` and `turns`, the round's turn counter.
  Every rule that builds a state carries them.
- The turn counter increments exactly where the old engine's did: after a lead, after
  taking the pot, and after a defence that emptied the hand.
- `Seats` gained `countWithCards` and `nextWithCards`; the latter wraps around to the
  current seat, as the old `TurnOrder.nextActive` did.
- The step calls `EndRound.apply` when the round is over — at most one seat still
  holds cards — or when the bound fires.
- **The events can be derived from the old and the new state**: the new pot's last card
  is the card played; whether it was a lead or a beat is decided by the *old* state
  (`leadRequired || pot.isEmpty()`); an old pot that is not empty and a new pot that is
  means the pot was taken; a board that grew means the round ended; the counter back to
  zero means a new round was dealt.

### A live defect in the old engine

If a round starts with a **single card in play** — possible in the last rounds of a
long game, when the pool that came back is smaller than the number of seats — then the
old engine's `TurnOrder.nextActive` looks for a seat that can act, finds none, and
loops forever. The game freezes. The new engine fails loudly instead.

The old engine is what the console, the desktop and the Android apps run today, so
this is a defect in the shipped product, not only in the code being replaced. It is
recorded as its own task (T19) rather than folded into the migration.

### What T6 established

- **The seam was widened, not derived.** The rules return a `Transition` and report
  what they did: `Dealing` reports the round and the cards dealt, `Exchange` reports
  every transfer, `EndRound` reports the round that ended, the card pushed and the
  elimination, `Trick` reports the card played, the card beaten or the pot taken.
- Deriving the events from the old and the new state cannot work for two facts: the
  deal is not derivable at all, because the dealt state is gone once the exchange has
  run, and a round that ended without a card for the board is indistinguishable from a
  round that did not end.
- The vocabulary the golden gate compares: `CardPlayed`, `CardBeaten` (with the card
  that was beaten), `PotTaken` (with the top card and the size), `RoundEnded` (with
  the loser and the card pushed, absent when there was none), `SeatEliminated`,
  `GameEnded`, and for the setup `RoundStarted`, `CardsDealt`, `CardsExchanged`.
- The old stream differs in three ways the gate has to bridge: it has no
  `CardsDealt` and no `CardsExchanged`, it folds the elimination into `RoundEnded` as
  a flag, and it names the seat by its trump suit where the new events carry the
  `Seat`. The old `AwaitingHumanInput` has no counterpart and is filtered out.

### What T7 established

- **The gate is green: 50,000 seeds compared, not one divergence**, and the four
  pathological seeds hit the bound at the same point, with the same loser and the same
  number of card events.
- The gate found a real defect in the new engine, and that is what it is for. The
  trick and the round end skipped the turn of a dealer that holds no cards, so the
  new engine played a card in a round the old engine ends at once — 14 of the first
  200 seeds diverged. `Trick.beginRound` now ends that round the way the old engine
  does, and counts the empty turn, which is what keeps the counter aligned.
- A round that begins with a single seat holding cards now ends at once, and the round
  after it may end the same way.
- Where the two engines still differ: when **no** seat holds cards, the new engine
  fails loudly and the old one hangs (the T19 defect). The new engine deliberately
  does not reproduce the hang.
- The gate's helpers live in the `engine` module's test sources: `OldEngine`,
  `NewEngine`, `RulePolicy`, `Trace`, `ScriptedLayout`, and the test
  `testImplementation(project(":core"))` — the only place the new engine is allowed
  to see the old one. Both go away in T17.
- `RulePolicy` is the reference implementation of the rule bot for T9: the lead is the
  first hand card that minimises (is-trump, rank), keeping the first of the equal
  minima; the defence is the first legal move, and the engine already lists the
  defences sorted by (is-trump, rank) with a stable sort. Use the rank **ordinal** on
  the new side; the old `getValue()` is the ordinal plus two.
- The old scripted dealer could give a seat a different number of cards, and the new
  engine deals a deck, so the ported scenarios use layouts where every seat holds
  eight. One scenario could not be carried over at all — the old scripted dealer never
  shuffles the pool between rounds, so the old scripted game is not a game the new
  engine can play — and it plays the seeded game both engines play instead, freezing
  the old engine's winner for seed 42.

### What T8 established

- The invariants hold over **60,000 games**: card conservation at every step of every
  game (not only at the end), exactly one winner, termination, and reproducibility of
  the whole final state from the seed. The two- and three-seat tables are checked
  against the rules and ADR 0005, since the old engine has no behaviour for them.
- Exactly **four of 50,000** games hit the safety bound — the same four seeds the old
  engine has — and the detection is exact, neither over- nor under-reporting.
- **The new engine is about three times slower than the old one**: 72 s against 26 s
  for 50,000 games, because every step copies immutable lists into a new state. A
  whole game takes about 1.5 ms, which is nothing next to a game a person plays, so
  the price is paid knowingly.
- The safety bound has to be *nameable* for the tools to report honestly which rounds
  hit it. `Trick.MAX_ROUND_TURNS` is package-private today; it becomes a public
  constant on `Game` before `tools/verify` needs it. A constant, not a method — the
  engine's public surface stays three methods.
- `EngineSimulator` lives in the engine's test sources today; it moves with `bots` in
  T9, or `tools/verify` takes it over.

### What T9 and T10 established

- **`bots` is a module of its own** with `RuleBot.choose(state, moves)`: a lead takes
  the first hand card that minimises (is-trump, rank) and keeps the first of the equal
  minima, a defence takes the first legal move. The golden gate now drives the real
  bot on the new side, and stays green over 50,000 seeds.
- The engine's tests depend on `bots` **for the test classpath only**. Gradle accepts
  the project cycle because the task graph stays acyclic; the engine's main classpath
  is still empty.
- **`ml` is a module of its own** with `Observation` (the table as the seat to move sees
  it) and `Vector.of(state, seat, moves)`: 352 floats of a fixed length, plus a mask.
- **The mask is a mask over cards, not over positions in the legal-moves list.** Bit
  `suit.ordinal() * 13 + rank.ordinal()` is set when that card is legal, bit 52 when
  taking the pot is legal. The engine's list is in the hand's order for a lead and
  sorted for a defence, and the vector does not carry the hand's order — so a mask
  over positions would have left the model unable to tell which card a slot meant.
  The action space is therefore 53 wide: 52 cards and taking the pot, and the adapter
  turns the chosen action into the index the engine wants.
- The engine's `Ladders` became public so that `ml` does not keep a second copy of
  the rule "the rank a board requires next". The engine's public surface is still
  `Game` with three methods, plus that one class.
- The vector does not encode the rank set, so a model trained on the 36-card deck does
  not serve the 52-card one.

### The new engine's package

The new module is `engine` and its package is `ru.anseranser.engine`: the public value
types and the package-private rule classes side by side, the way the old module keeps
`RoundResolver` next to `Game`. In T17 the *module* takes the name `core`; the package
is not renamed, so no front end's imports change.

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
