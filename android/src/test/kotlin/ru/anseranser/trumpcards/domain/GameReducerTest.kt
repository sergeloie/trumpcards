package ru.anseranser.trumpcards.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.anseranser.engine.Card
import ru.anseranser.engine.RankSet

class GameReducerTest {

    private val card = Card(Card.Suit.SPADES, Card.Rank.SEVEN)

    @Test
    fun startingAGameMovesTheStateIntoPlaying() {
        val reduced = gameReducer(GameState(), GameIntent.StartGame(RankSet.TWO_TO_ACE))

        assertEquals(GamePhase.PLAYING, reduced.phase)
        assertEquals(RankSet.TWO_TO_ACE, reduced.rankSet)
    }

    @Test
    fun choosingACardClearsTheChoice() {
        val state = GameState(
            phase = GamePhase.PLAYING,
            choice = PlayerChoice(playableCards = listOf(card), canTakePot = false),
        )

        val reduced = gameReducer(state, GameIntent.CardChosen(card))

        assertNull(reduced.choice)
        assertEquals(GamePhase.PLAYING, reduced.phase)
    }

    @Test
    fun takingThePotClearsTheChoice() {
        val state = GameState(
            phase = GamePhase.PLAYING,
            choice = PlayerChoice(playableCards = emptyList(), canTakePot = true),
        )

        val reduced = gameReducer(state, GameIntent.TakePotChosen)

        assertNull(reduced.choice)
    }

    @Test
    fun aNewGameReturnsToTheMenu() {
        val state = GameState(
            phase = GamePhase.GAME_OVER,
            rankSet = RankSet.SIX_TO_ACE,
            winner = Card.Suit.SPADES,
            choice = PlayerChoice(playableCards = listOf(card), canTakePot = true),
        )

        val reduced = gameReducer(state, GameIntent.NewGame)

        assertEquals(GamePhase.MENU, reduced.phase)
        assertNull(reduced.rankSet)
        assertNull(reduced.winner)
        assertNull(reduced.choice)
        assertTrue(reduced.humanHand.isEmpty())
    }

    @Test
    fun goingToTheMenuReturnsToTheMenu() {
        val state = GameState(phase = GamePhase.PLAYING, rankSet = RankSet.SIX_TO_ACE)

        val reduced = gameReducer(state, GameIntent.GoToMenu)

        assertEquals(GamePhase.MENU, reduced.phase)
        assertNull(reduced.rankSet)
    }
}
