package ru.anseranser.trumpcards.domain

import ru.anseranser.engine.Card
import ru.anseranser.engine.Move
import ru.anseranser.engine.RankSet
import ru.anseranser.engine.State

/**
 * The mapping of the engine's state into the state the screens are painted
 * from.
 *
 * <p>The board of the engine is bottom-up, so it is turned over here: the
 * screens show the top of the ladder first.
 */
object GameStateMapper {

    fun of(
        state: State,
        moves: List<Move>,
        humanSeat: Card.Suit,
        rankSet: RankSet,
    ): GameState {
        val over = moves.isEmpty()
        return GameState(
            phase = if (over) GamePhase.GAME_OVER else GamePhase.PLAYING,
            rankSet = rankSet,
            trump = state.dealer().trump(),
            scoreboard = scoreboard(state),
            pot = state.pot(),
            humanHand = hand(state, humanSeat),
            opponents = opponents(state, humanSeat),
            choice = choice(state, moves, humanSeat),
            winner = if (over) winner(state) else null,
            humanSeat = humanSeat,
        )
    }

    private fun scoreboard(state: State): Map<Card.Suit, List<Card>> =
        state.seats().associate { seat -> seat.trump() to seat.board().reversed() }

    private fun hand(state: State, humanSeat: Card.Suit): List<Card> =
        state.seats().firstOrNull { it.trump() == humanSeat }?.hand() ?: emptyList()

    private fun opponents(state: State, humanSeat: Card.Suit): List<OpponentState> =
        state.seats()
            .filter { it.trump() != humanSeat }
            .map { seat -> OpponentState(seat.trump(), seat.hand().size, seat.out()) }
            .sortedBy { it.suit.ordinal }

    private fun choice(state: State, moves: List<Move>, humanSeat: Card.Suit): PlayerChoice? {
        if (moves.isEmpty() || state.turn().trump() != humanSeat) {
            return null
        }
        return PlayerChoice(
            playableCards = moves.filterIsInstance<Move.Play>().map { it.card() },
            canTakePot = moves.any { it is Move.TakePot },
        )
    }

    private fun winner(state: State): Card.Suit? =
        state.seats().firstOrNull { !it.out() }?.trump()
}
