package ru.anseranser.trumpcards.engine

import ru.anseranser.engine.Card
import ru.anseranser.engine.Move

/**
 * The move the person at the table picks: a card to play, or the pot to take.
 */
sealed interface MoveChoice {

    data class PlayCard(val card: Card) : MoveChoice

    data object TakePot : MoveChoice
}

/**
 * The resolution of a choice into the index of a legal move.
 */
object MoveChoices {

    /** The index of the move the choice stands for, or -1 when it is not legal. */
    fun indexOf(moves: List<Move>, choice: MoveChoice): Int = when (choice) {
        is MoveChoice.PlayCard -> indexOfCard(moves, choice.card)
        MoveChoice.TakePot -> indexOfTakePot(moves)
    }

    /** The index of the move that plays the card, or -1 when it is not legal. */
    fun indexOfCard(moves: List<Move>, card: Card): Int {
        for (i in moves.indices) {
            val move = moves[i]
            if (move is Move.Play && move.card() == card) {
                return i
            }
        }
        return -1
    }

    /** The index of the move that takes the pot, or -1 when it is not legal. */
    fun indexOfTakePot(moves: List<Move>): Int {
        for (i in moves.indices) {
            if (moves[i] is Move.TakePot) {
                return i
            }
        }
        return -1
    }
}
