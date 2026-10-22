# Trump Cards

A competitive card game for 2 to 4 players. Each player holds a trump suit, beats the
opponents' cards, and collects trump cards on their board until their ladder is complete
and they are out. The last player still holding cards in a round loses that round.

This context is the game's rules and vocabulary. It is the single source of truth for
what a card, a board, a round and a legal move mean. Nothing here describes how the
software is built.

## The table

**Seat**:
One participant at the table. A seat owns exactly one trump suit, one hand, one board
and one "out" flag.
_Avoid_: Player, seat position, chair

**Trump suit**:
The suit a seat beats with. Every suit in play belongs to exactly one seat, so a seat's
trump suit is the only route to beating a card of that suit.
_Avoid_: Master suit, suit of the player

**Deck**:
The set of cards in play for one game: the suits that are in play, each with the game's
rank set, minus the base card of every suit.
_Avoid_: Card pool, draw pile, remaining cards

**Rank set**:
The ranks a deck is built from. The classic 36-card rank set runs SIX to ACE; the 52-card
rank set runs TWO to ACE. The lowest rank of the set is the **base rank**.
_Avoid_: Deck size, card count

**Base card**:
The lowest card of a seat's trump suit. It starts on the seat's board and never returns
to play.
_Avoid_: Starter card, seed card

**Board**:
A seat's ladder of trump cards, running from the base card upward one rank at a time.
Completing the ladder to the ACE eliminates the seat.
_Avoid_: Scoreboard, ladder, scale, board of the player

**Hand**:
The cards a seat holds. A seat that empties its hand is out of the current round but
stays in the game until its board is complete.
_Avoid_: Cards, holding

**Pot**:
The pile of cards played in the current round. It grows with every trick and is taken
whole by a seat that cannot beat the top card.
_Avoid_: Bank, table pile, discard

**Trick**:
The unit of play inside a round: one card played, then each following seat either beats
it or the pot is taken.
_Avoid_: Pile, hand of cards, combination

## The round

**Round**:
One pass through dealing, obligatory exchange and play, ending when a single seat still
holds cards. The loser of the round pushes the trump of the rank their board requires
next onto their board, or their lowest trump when that card is not among their cards.
_Avoid_: Hand (of cards), deal

**Obligatory exchange**:
Once at the start of a round, every card of another seat's trump suit that equals the
next rank required by that seat's board must be passed to that seat. Aces are never
exchanged, and no chain of passes happens.
_Avoid_: Trade, swap, passing

**Lead**:
Playing a card into an empty pot, or playing again after having beaten the previous
card. A seat that has just beaten a card leads the next trick.
_Avoid_: Opening, first play

**Beat**:
Playing a card that outranks the top card of the pot: the same suit with a higher rank,
or the seat's own trump.
_Avoid_: Cover, overbid, respond

**Take the pot**:
The only move available when no card in the hand can beat the top card. The seat takes
every card in the pot into its hand and play continues with the next seat.
_Avoid_: Pass, fold, scoop

**Elimination**:
A seat whose board has reached the ACE leaves the game. The last seat remaining wins.
_Avoid_: Out, knocked out, finishing

**Capped round**:
A round stopped by the safety bound on decisions per round. Its loser is the seat
holding the most cards; ties are broken by seating order. A capped round is a defect
guard, never a normal outcome.
_Avoid_: Timeout, truncated round, limit hit

## The machine

**State**:
The complete, self-contained description of one game at one point in time: every seat,
the pot, the deck, the board, whose turn it is, and the random generator's position.
A state alone is enough to continue the game.
_Avoid_: Snapshot, board state, game state

**Legal move**:
One of the choices available to the seat whose turn it is. Playing a card and taking the
pot are both legal moves; an illegal one cannot be expressed.
_Avoid_: Action, option, choice

**Step**:
Applying one legal move to a state, producing the next state. A step is the only way
the game advances.
_Avoid_: Tick, update, turn

**Transition**:
The result of a step: the new state plus the things that happened on the way (dealing,
exchange, a card played, a round ending). The happenings are reported once and are not
kept.
_Avoid_: Event log, history, diff

**Action log**:
The ordered list of legal moves chosen in a game. Together with the setup it is enough
to reproduce a game exactly, and to continue one that was interrupted.
_Avoid_: Move history, replay file, save file

**Episode**:
One complete game from the first deal to the winner, recorded as setup, steps and
outcome.
_Avoid_: Game, run, session

**Policy**:
Anything that chooses legal moves: a person, a fixed rule, a trained network. The
rules do not know what a policy is.
_Avoid_: Agent, player, bot

**Observation**:
What a policy sees: the state as seen from the seat that is about to move.
_Avoid_: Features, vector, input

## Invariants

These hold in every state of every game.

1. Every card is in exactly one place: a hand, the pot, the deck, or a board.
2. A board only ever grows, and its cards never return to play.
3. The exchange happens once, at the start of a round.
4. Only seats that held cards when the round began take part in it.
5. The pot either grows, or is taken whole by one seat.
6. The rules of the game depend on nothing outside the state.

## How a game runs

A game is set up: the rank set, the trump suits, and the seed of the random generator.
The setup produces the first state and reports what happened — the deal and the exchange.

From then on the game is a loop. The seat whose turn it is has a list of legal moves;
something outside the rules picks one; applying it produces the next state and reports
what happened. A seat that empties its hand leaves the round. When only one seat still
holds cards, that seat loses the round: the trump of the rank its board requires next
goes onto its board — or its lowest trump, when that card is not among its cards — the
rest of its cards and the pot return to the deck, and a new round is dealt. When only
one seat is left in the game, the game is over.

A policy never sees the state directly. It sees an observation, it produces a choice
among the legal moves, and the rules do the rest.

## Example

> **Player:** "When I beat the spade, do I get to lead again?"
> **Rules:** Yes. Beating the top card means you lead the next trick.
>
> **Player:** "I hold the seven of hearts. The heart player's board shows the six. Do I
> have to give it to them?"
> **Rules:** At the start of the round, yes — that is the obligatory exchange. The ace is
> the one card never exchanged.
>
> **Player:** "I could not beat the queen, so I took the pot. Who plays next?"
> **Rules:** The seat after you. You led nothing, so the next seat leads the new trick.
