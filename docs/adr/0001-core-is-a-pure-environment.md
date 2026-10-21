# The core is a pure environment

The rules module holds no state and knows nothing about who is playing: it computes
states from states. Every policy — a person, the fixed rule bot, a trained network —
lives outside it and is reached only through the list of legal moves.

## Considered options

- **Keep the strategy interface inside the core** (`DecisionStrategy`, as before). It
  keeps the rules and the bots in one place, but the core then knows what a player is,
  and the same rules cannot be driven by a trainer, a replay, or a UI without going
  through that interface.
- **Keep the "live object plus snapshot" shape.** A mutable game object with copies for
  the UI means two representations of the same game exist at once, and they drift.

## Consequences

- The core's whole API is three methods; everything else is package-private.
- A policy is any function from `(state, legal moves)` to an index. There is no
  interface to implement, no registry, and no place for a policy to be wired in wrongly.
- The core cannot block, cannot do I/O, and cannot be given a UI thread. Front ends run
  it in whatever loop they like.
- The core is testable without any front end, bot or model.
