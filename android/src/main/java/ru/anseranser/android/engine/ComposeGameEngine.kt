package ru.anseranser.android.engine

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import ru.anseranser.input.HumanDecisionStrategy
import ru.anseranser.model.AiDecisionStrategy
import ru.anseranser.model.Card
import ru.anseranser.model.DecisionStrategy
import ru.anseranser.model.Game
import ru.anseranser.model.Game.GameDriver
import ru.anseranser.model.Player

private const val TAG = "ComposeGameEngine"

class ComposeGameEngine {

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var game: Game? = null
    private var driver: GameDriver? = null

    val inputProvider = ComposeInputProvider()

    private val _snapshot = MutableStateFlow(ComposeSnapshot.empty())
    val snapshot: StateFlow<ComposeSnapshot> = _snapshot

    fun startGame() {
        Log.d(TAG, "startGame() called")
        try {
            val players = mutableListOf<Player>()
            for (suit in Card.Suit.entries) {
                val strategy: DecisionStrategy = if (suit == Card.Suit.SPADES) {
                    HumanDecisionStrategy(inputProvider)
                } else {
                    AiDecisionStrategy()
                }
                players.add(Player(suit, strategy))
            }
            Log.d(TAG, "Created ${players.size} players")

            val g = Game(players)
            game = g
            Log.d(TAG, "Game created")

            val l = ComposeGameListener(g, inputProvider)
            g.setListener(l)

            // Wire up the repaint callback so UI updates when validChoices changes
            inputProvider.onValidChoicesChanged = Runnable {
                Log.d(TAG, "validChoices changed, requesting repaint")
                l.requestRepaint()
            }
            Log.d(TAG, "Listener set, callback wired")

            scope.launch {
                l.snapshot.collect { snap ->
                    _snapshot.value = snap
                }
            }

            scope.launch {
                try {
                    val d = g.createDriver()
                    driver = d
                    Log.d(TAG, "Driver created, starting game")
                    d.startGame()
                    Log.d(TAG, "Game started, entering loop")

                    while (!d.isGameOver) {
                        val current = d.current
                        val wasHuman = current?.isHuman ?: false
                        Log.d(TAG, "Step: current=${current?.trump}, isHuman=$wasHuman")
                        val more = d.step()
                        if (!more) {
                            Log.d(TAG, "Round ended, finishing")
                            d.finishRound()
                            continue
                        }
                        // Use the saved wasHuman flag, not current.isHuman
                        // because advanceAfterTurn() already changed current
                        if (!wasHuman) {
                            delay(AI_PAUSE_MS)
                        }
                    }
                    Log.d(TAG, "Game over")
                } catch (e: InterruptedException) {
                    Log.d(TAG, "Game loop interrupted (expected on shutdown)")
                } catch (e: Exception) {
                    Log.e(TAG, "Game loop error", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "startGame() failed", e)
        }
    }

    fun selectCard(card: Card) {
        inputProvider.selectCard(card)
    }

    fun stop() {
        scope.cancel()
    }

    companion object {
        private const val AI_PAUSE_MS = 450L
    }
}
