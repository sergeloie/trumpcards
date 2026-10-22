package ru.anseranser.trumpcards.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.anseranser.engine.Card
import ru.anseranser.engine.Move
import ru.anseranser.engine.RankSet
import ru.anseranser.engine.Seat
import ru.anseranser.engine.State

class GameStateMapperTest {

    private val six = Card(Card.Suit.SPADES, Card.Rank.SIX)
    private val seven = Card(Card.Suit.SPADES, Card.Rank.SEVEN)
    private val queen = Card(Card.Suit.CLUBS, Card.Rank.QUEEN)
    private val ace = Card(Card.Suit.HEARTS, Card.Rank.ACE)
    private val king = Card(Card.Suit.DIAMONDS, Card.Rank.KING)

    private val spades = Seat(Card.Suit.SPADES, listOf(six, seven), listOf(six, seven), false)
    private val clubs = Seat(Card.Suit.CLUBS, listOf(queen), listOf(), false)
    private val diamonds = Seat(Card.Suit.DIAMONDS, listOf(), listOf(), true)
    private val hearts = Seat(Card.Suit.HEARTS, listOf(ace), listOf(), false)

    private fun state(turn: Seat = spades): State = State(
        listOf(spades, clubs, diamonds, hearts),
        listOf(),
        listOf(),
        spades,
        turn,
        false,
        0,
        0L,
    )

    @Test
    fun theBoardOfTheEngineIsTurnedOver() {
        val mapped = GameStateMapper.of(state(), listOf(Move.Play(six)), Card.Suit.SPADES, RankSet.SIX_TO_ACE)

        assertEquals(listOf(seven, six), mapped.scoreboard[Card.Suit.SPADES])
    }

    @Test
    fun theHandOfThePersonIsTheHandOfTheirSeat() {
        val mapped = GameStateMapper.of(state(), listOf(Move.Play(six)), Card.Suit.SPADES, RankSet.SIX_TO_ACE)

        assertEquals(listOf(six, seven), mapped.humanHand)
        assertEquals(Card.Suit.SPADES, mapped.humanSeat)
    }

    @Test
    fun theOpponentsAreTheOtherSeatsWithTheirCardsAndTheirBoards() {
        val mapped = GameStateMapper.of(state(), listOf(Move.Play(six)), Card.Suit.SPADES, RankSet.SIX_TO_ACE)

        assertEquals(
            listOf(
                OpponentState(Card.Suit.CLUBS, cardCount = 1, isEliminated = false),
                OpponentState(Card.Suit.DIAMONDS, cardCount = 0, isEliminated = true),
                OpponentState(Card.Suit.HEARTS, cardCount = 1, isEliminated = false),
            ),
            mapped.opponents,
        )
    }

    @Test
    fun theChoiceHoldsTheCardsThatMayBePlayed() {
        val moves = listOf(Move.Play(seven), Move.Play(six))

        val mapped = GameStateMapper.of(state(), moves, Card.Suit.SPADES, RankSet.SIX_TO_ACE)

        assertEquals(PlayerChoice(playableCards = listOf(seven, six), canTakePot = false), mapped.choice)
    }

    @Test
    fun theChoiceHoldsThePotWhenItMayBeTaken() {
        val moves = listOf(Move.TakePot())

        val mapped = GameStateMapper.of(state(), moves, Card.Suit.SPADES, RankSet.SIX_TO_ACE)

        assertEquals(PlayerChoice(playableCards = emptyList(), canTakePot = true), mapped.choice)
    }

    @Test
    fun thereIsNoChoiceWhenItIsNotTheTurnOfThePerson() {
        val moves = listOf(Move.Play(queen))

        val mapped = GameStateMapper.of(state(turn = clubs), moves, Card.Suit.SPADES, RankSet.SIX_TO_ACE)

        assertNull(mapped.choice)
    }

    @Test
    fun theGameIsOverWhenThereAreNoLegalMoves() {
        val mapped = GameStateMapper.of(state(), emptyList(), Card.Suit.SPADES, RankSet.SIX_TO_ACE)

        assertEquals(GamePhase.GAME_OVER, mapped.phase)
        assertNull(mapped.choice)
    }

    @Test
    fun theWinnerIsTheSeatThatIsStillInTheGame() {
        val last = Seat(Card.Suit.HEARTS, listOf(ace), listOf(), false)
        val finished = State(
            listOf(
                Seat(Card.Suit.SPADES, listOf(), listOf(), true),
                Seat(Card.Suit.CLUBS, listOf(), listOf(), true),
                Seat(Card.Suit.DIAMONDS, listOf(), listOf(), true),
                last,
            ),
            listOf(),
            listOf(),
            last,
            last,
            false,
            3,
            0L,
        )

        val mapped = GameStateMapper.of(finished, emptyList(), Card.Suit.SPADES, RankSet.SIX_TO_ACE)

        assertEquals(GamePhase.GAME_OVER, mapped.phase)
        assertEquals(Card.Suit.HEARTS, mapped.winner)
    }

    @Test
    fun theTrumpIsTheSuitOfTheDealer() {
        val mapped = GameStateMapper.of(state(), listOf(Move.Play(six)), Card.Suit.SPADES, RankSet.SIX_TO_ACE)

        assertEquals(Card.Suit.SPADES, mapped.trump)
        assertEquals(RankSet.SIX_TO_ACE, mapped.rankSet)
    }

    @Test
    fun thePotIsThePotOfTheEngine() {
        val withPot = State(
            listOf(spades, clubs, diamonds, hearts),
            listOf(six, queen),
            listOf(),
            spades,
            spades,
            false,
            0,
            0L,
        )

        val mapped = GameStateMapper.of(withPot, listOf(Move.Play(six)), Card.Suit.SPADES, RankSet.SIX_TO_ACE)

        assertEquals(listOf(six, queen), mapped.pot)
        assertFalse(mapped.pot.isEmpty())
        assertTrue(mapped.opponents.isNotEmpty())
    }
}
