# A step returns a transition, not just a state

Every step returns the new state *and* the list of things that happened while producing
it: cards dealt, cards exchanged, a card played, a round ended, a seat eliminated. The
initial setup returns the same kind of result.

## Considered options

- **A listener interface** (as before): the rules push events to whoever registered.
  This makes the rules depend on a callback shape, and it makes "what happened" live
  outside the result of the call that caused it.
- **Front ends diff two states themselves.** Then the rules say nothing about what
  happened, and every front end has to rediscover "a card was exchanged" from two
  snapshots.

## Consequences

- A front end receives the deal and the exchange, which is what it needs to show them,
  without the rules keeping a log of anything.
- The happenings are computed during the step and handed over; they are not stored, so
  there is no second copy of the game's history.
- The rules decide *when* something happened, never *how* it should look.
- A step may resolve several things at once (a round ends, a new round is dealt). The
  front end receives them as one ordered list and can animate them in order.
