# The deck contains only the suits in play

A deck is built from the suits that are in play, each with the game's rank set, minus
one base card per suit. With two players the deck holds two suits, the two base cards
sit on the two boards, and sixteen cards are dealt. With four players it is the familiar
thirty-two.

## Considered options

- **Always a full four-suit deck, with the unused suits' base cards parked on boards
  nobody owns** (the previous behaviour). It keeps the four-player game identical, but
  the two-player and three-player variants deal fewer cards than the rules imply, and
  the boards for suits nobody plays are state that can never move.
- **A deck with a variable number of suits** (the generalisation where `k` seats and
  `k` suits). It is uniform, but it invents a game the classic deck does not have.

## Consequences

- Every suit in the deck belongs to exactly one seat, so the exchange rule has no
  "nobody owns this suit" case. The branch that handled it, and its test, are gone.
- The board belongs to a seat rather than to a suit, and there are exactly as many
  boards as seats.
- The four-player game is unchanged, down to the last card and the order of play, so
  the existing golden scenarios stay valid.
- The two- and three-player variants change: fewer cards are dealt, and the exchange
  involves only the suits that are actually in play.
