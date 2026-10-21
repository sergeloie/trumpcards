# A legal move is identified by its index

A step takes the *index* of a legal move, not a card. The rules list what is allowed;
something outside chooses a position in that list. An illegal move therefore has no
representation and needs no validation.

## Considered options

- **Pass the card itself** (a seat and a card). The rules then have to check that the
  card is in the hand and that it beats the top card, and reject the rest. This is
  exactly how the previous implementation leaked: a defence could be applied with a
  card that was not in the hand, leaving that card in the hand *and* in the pot.
- **Pass a fixed card slot** (0..51) plus a mask of legal slots. A fixed action space
  suits a network, but it pushes the slot numbering and the mask into the rules, and
  the rules would still have to translate a slot back into a card.

## Consequences

- There is no "illegal move" error path in the rules. An out-of-range index is an
  `IndexOutOfBoundsException`, the same as any other bad argument.
- The action log records the move itself, so it stays readable and survives a change in
  the order the legal moves are listed.
- A learned policy picks an index into a list whose length varies; the adapter in `ml`
  turns that into a fixed-size masked vector, so the rules never learn what a tensor is.
