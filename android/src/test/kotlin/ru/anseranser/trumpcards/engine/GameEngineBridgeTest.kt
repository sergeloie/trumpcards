package ru.anseranser.trumpcards.engine

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.anseranser.engine.Card
import ru.anseranser.engine.RankSet
import ru.anseranser.trumpcards.domain.GamePhase

class GameEngineBridgeTest {

    @Test
    fun aWholeGameIsPlayedToItsEnd() = runBlocking {
        val bridge = GameEngineBridge(Card.Suit.SPADES, botMoveDelayMs = 0)
        bridge.startGame(RankSet.SIX_TO_ACE)
        try {
            withTimeout(PLAY_TIMEOUT_MS) {
                while (true) {
                    val state = bridge.state.value
                    if (state.phase == GamePhase.GAME_OVER) {
                        break
                    }
                    val choice = state.choice
                    if (choice != null) {
                        val card = choice.playableCards.firstOrNull()
                        when {
                            card != null -> bridge.choose(MoveChoice.PlayCard(card))
                            choice.canTakePot -> bridge.choose(MoveChoice.TakePot)
                        }
                    }
                    delay(POLL_MS)
                }
            }
        } finally {
            bridge.stop()
        }

        val state = bridge.state.value
        assertEquals(GamePhase.GAME_OVER, state.phase)
        assertNotNull(state.winner)
        assertTrue(state.humanHand.isEmpty())
    }

    @Test
    fun theGameOffersTheCardsThatMayBePlayed() = runBlocking {
        val bridge = GameEngineBridge(Card.Suit.SPADES, botMoveDelayMs = 0)
        bridge.startGame(RankSet.SIX_TO_ACE)
        try {
            withTimeout(PLAY_TIMEOUT_MS) {
                while (bridge.state.value.choice == null) {
                    delay(POLL_MS)
                }
            }
        } finally {
            bridge.stop()
        }

        val choice = bridge.state.value.choice
        assertNotNull(choice)
        assertTrue(choice!!.playableCards.isNotEmpty() || choice.canTakePot)
    }

    private companion object {
        const val PLAY_TIMEOUT_MS = 60_000L
        const val POLL_MS = 5L
    }
}
