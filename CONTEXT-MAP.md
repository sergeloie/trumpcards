# Context Map

One domain context ([Trump Cards](./CONTEXT.md)) implemented as a set of Gradle modules
with a strictly one-way dependency graph. The modules are layers and tools around the
domain, not separate contexts — so this document is the architectural index: what lives
where, what depends on what, and where you enter the system.

> This describes the target shape. The migration is under way: the rules currently
> live in the old `core` module, and the new engine is being built in an `engine`
> module beside it. See [plan.md](./docs/plan.md) for the steps and the gate that
> keeps the two implementations honest.


## The rule

**`core` depends on nothing but the JDK.** It has no dependency on any other module, on
the JDK's extensions, on a UI framework, on a machine-learning runtime, or on a file
system. Every other arrow in the graph points *towards* `core`.

The build enforces this: `core`'s build file declares no dependencies. A future change
that adds one is visible in a diff of one file.

## Modules

| Module | Responsibility | Depends on | Entry points |
|---|---|---|---|
| `core` | The rules. Value types, state, legal moves, transitions. | *(JDK only)* | `Game.start`, `Game.legalMoves`, `Game.step` |
| `bots` | Fixed policies. The rule-based bot that leads and defends with the weakest legal card. | `core` | `RuleBot.choose(state, moves)` |
| `ml` | Learned policies. Turns a state into an observation, runs an ONNX model, chooses among legal moves. | `core`, ONNX Runtime | `Vector.of(state, seat, moves)`, `OnnxPolicy.choose(...)` |
| `tools/exporter` | Writes training episodes to JSONL. | `core`, `bots`, `ml` | `main` |
| `tools/verify` | Runs many episodes and asserts the game's invariants. | `core`, `bots` | `main` |
| `tools/eval` | Measures a learned policy against the rule bot; asserts invariants under a learned policy. | `core`, `bots`, `ml` | `main` |
| `console` | Text front end. | `core`, `bots` | `ConsoleLauncher.main` |
| `desktop-libgdx` | Desktop front end. | `core`, `bots`, `ml` | `DesktopLauncher.main` |
| `android` | Android front end. | `core`, `bots`, `ml` | `MainActivity` |

```
   console      desktop-libgdx      android
       └──────┬──────┴────────┬────────┘
              │               │
              ▼               ▼
          ┌───────┐      ┌───────┐
          │ core │◄─────│  ml   │
          └───────┘      └───────┘
              ▲
              │
          ┌───────┐
          │ bots  │
          └───────┘
              ▲
              │
        ┌───────────┐
        │   tools   │   exporter · verify · eval
        └───────────┘
```

## The core's public surface

Three methods. Everything else is internal to `core`.

```java
Transition start(Setup setup);               // first state + what happened (deal, exchange)
List<Move> legalMoves(State state);          // empty when the game is over
Transition step(State state, int moveIndex); // index into legalMoves
```

The value types that cross the boundary: `Setup`, `State`, `Seat`, `Move`, `Transition`,
`Event`, `Card`, `RankSet`.

The rules themselves are internal to the module and organised by rule area, not by
class: `Dealing`, `Exchange`, `Trick`, `Ladders`, `EndRound`, `Seats`, `Rng`. They are
package-private, so the module's API surface stays small enough to read in one screen.

## Data flow

```
Setup ──▶ start ──▶ State ──▶ legalMoves ──▶ [policy picks an index]
                     ▲                                 │
                     └──────── step(state, index) ◀────┘
                                  │
                                  └──▶ Transition(state, events)
                                            │
        ┌───────────────────────────────────┼───────────────────────┐
        ▼                                   ▼                       ▼
   front end renders                  action log appended      observation recorded
   the events                          (save / resume / replay)  (training episode)
```

## Where the state lives

Exactly one place: the `State` value. Nothing else keeps a copy.

- The rules hold no state between calls; every rule function takes a state and returns
  a new one.
- A front end holds the state it was last handed, to render it.
- An episode holds the setup and the list of chosen moves, not a state.
- The random generator's position lives inside the state, so a game is a pure function
  of its setup and its moves.

## How the front ends drive a game

All three front ends run the same loop; none of them owns a rule.

```java
State state = Game.start(setup).state();
while (!Game.legalMoves(state).isEmpty()) {
    List<Move> moves = Game.legalMoves(state);
    int index = policy.choose(state, moves);   // a person, RuleBot, or OnnxPolicy
    Transition t = Game.step(state, index);
    state = t.state();
    render(t.events());                         // deal, exchange, cards played, round end
}
```

The human's move is chosen the same way as a machine's: the front end shows the legal
moves, the person picks one, and the front end passes the index.

## Decisions behind this shape

Recorded in [`docs/adr/`](./docs/adr):

- [0001 — The core is a pure environment](./docs/adr/0001-core-is-a-pure-environment.md)
- [0002 — A legal move is identified by its index](./docs/adr/0002-a-legal-move-is-identified-by-its-index.md)
- [0003 — Saving a game is saving its action log](./docs/adr/0003-saving-a-game-is-saving-its-action-log.md)
- [0004 — A step returns a transition, not just a state](./docs/adr/0004-a-step-returns-a-transition-not-just-a-state.md)
- [0005 — The deck contains only the suits in play](./docs/adr/0005-the-deck-contains-only-the-suits-in-play.md)
- [0006 — Neural players are trained in Python and run in the JVM](./docs/adr/0006-neural-players-are-trained-in-python-and-run-in-the-jvm.md)
