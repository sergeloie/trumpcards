package ru.anseranser.trumpcards.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import ru.anseranser.engine.Card
import ru.anseranser.engine.Move

class MoveChoicesTest {

    private val seven = Card(Card.Suit.SPADES, Card.Rank.SEVEN)
    private val ace = Card(Card.Suit.SPADES, Card.Rank.ACE)
    private val queen = Card(Card.Suit.HEARTS, Card.Rank.QUEEN)

    private val moves: List<Move> = listOf(Move.Play(seven), Move.Play(ace), Move.TakePot())

    @Test
    fun aCardResolvesToTheIndexOfTheMoveThatPlaysIt() {
        assertEquals(1, MoveChoices.indexOf(moves, MoveChoice.PlayCard(ace)))
    }

    @Test
    fun takingThePotResolvesToTheIndexOfThatMove() {
        assertEquals(2, MoveChoices.indexOf(moves, MoveChoice.TakePot))
    }

    @Test
    fun aCardThatIsNotLegalResolvesToNothing() {
        assertEquals(-1, MoveChoices.indexOf(moves, MoveChoice.PlayCard(queen)))
    }

    @Test
    fun takingThePotIsNotLegalWhenTheEngineDoesNotOfferIt() {
        val withoutTakePot = listOf(Move.Play(seven), Move.Play(ace))

        assertEquals(-1, MoveChoices.indexOf(withoutTakePot, MoveChoice.TakePot))
    }

    @Test
    fun anEmptyListOfMovesResolvesToNothing() {
        assertEquals(-1, MoveChoices.indexOf(emptyList(), MoveChoice.TakePot))
    }
}
