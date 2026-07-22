package ru.anseranser.trumpcards.engine

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import ru.anseranser.event.GameEvent
import ru.anseranser.input.HumanDecisionStrategy
import ru.anseranser.model.AiDecisionStrategy
import ru.anseranser.model.Card
import ru.anseranser.model.DeckSize
import ru.anseranser.model.Game
import ru.anseranser.model.GameState
import ru.anseranser.model.Player
import java.util.concurrent.LinkedBlockingQueue

class GameEngineBridge {

    private val _state = MutableStateFlow(ru.anseranser.trumpcards.domain.GameState())
    val state: StateFlow<ru.anseranser.trumpcards.domain.GameState> = _state

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<GameEvent> = _events

    val inputProvider = AndroidInputProvider()
    private val listener = AndroidGameListener()

    private var gameThread: Thread? = null

    @Volatile
    private var running = false

    // Queue for human card choices: UI thread puts, game thread takes
    private val cardQueue = LinkedBlockingQueue<Card>()

    fun startGame(deckSize: DeckSize) {
        stop()
        cardQueue.clear()

        val players = mutableListOf<Player>()
        for (suit in Card.Suit.entries) {
            val strategy = if (suit == Card.Suit.SPADES) {
                HumanDecisionStrategy(inputProvider)
            } else {
                AiDecisionStrategy()
            }
            players.add(Player(suit, strategy))
        }

        val g = Game(players, deckSize)
        g.setListener(listener)

        val d = g.createDriver()
        d.setAsyncInput(true)

        running = true
        gameThread = Thread({
            try {
                runGameLoop(d, deckSize)
            } catch (_: InterruptedException) {
                // Graceful shutdown
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                running = false
            }
        }, "game-loop").apply {
            isDaemon = false
            start()
        }
    }

    private fun runGameLoop(d: Game.GameDriver, deckSize: DeckSize) {
        d.startGame()
        captureState(d, deckSize)

        while (running && !d.isGameOver) {
            // Run one round: keep stepping until the round ends
            var stepResult = true
            while (running && !d.isGameOver && stepResult) {
                stepResult = d.step()

                if (!running || d.isGameOver) break

                // Human may need to make multiple choices in a row (e.g. defend then lead)
                while (running && d.isAwaitingHumanInput) {
                    captureState(d, deckSize)
                    val card = cardQueue.take()
                    if (!running) break
                    d.resume(card)
                }

                if (!running || d.isGameOver) break

                // Capture state and small delay for AI moves visibility
                captureState(d, deckSize)
                if (stepResult) Thread.sleep(300)
            }

            // Round ended — resolve and start next round
            if (running && !d.isGameOver) {
                captureState(d, deckSize)
                d.finishRound()
                captureState(d, deckSize)
            }
        }

        if (running) {
            captureState(d, deckSize)
        }
    }

    private fun captureState(d: Game.GameDriver, deckSize: DeckSize) {
        val snapshot = GameState.of(d.game)
        val opponents = buildOpponents(snapshot)
        val humanPlayer = snapshot.players().find { it.human() }
        val humanHand = (humanPlayer?.hand() ?: emptyList()).filterNotNull()

        val inputRequest = if (d.isAwaitingHumanInput) {
            val req = d.pendingInput()
            ru.anseranser.trumpcards.domain.InputRequestInfo(
                kind = when (req.kind()) {
                    Game.GameDriver.InputRequest.Kind.LEAD -> ru.anseranser.trumpcards.domain.InputRequestKind.LEAD
                    Game.GameDriver.InputRequest.Kind.DEFEND -> ru.anseranser.trumpcards.domain.InputRequestKind.DEFEND
                },
                validCards = req.validCards().filterNotNull(),
            )
        } else null

        val phase = if (d.isGameOver) {
            ru.anseranser.trumpcards.domain.GamePhase.GAME_OVER
        } else {
            ru.anseranser.trumpcards.domain.GamePhase.PLAYING
        }

        _state.value = ru.anseranser.trumpcards.domain.GameState(
            phase = phase,
            deckSize = deckSize,
            trump = snapshot.trump(),
            scoreboard = snapshot.scoreboard().mapValues { (_, cards) -> cards.filterNotNull() },
            pot = snapshot.pot().filterNotNull(),
            humanHand = humanHand,
            opponents = opponents,
            isWaitingForInput = d.isAwaitingHumanInput,
            inputRequest = inputRequest,
            winner = if (d.isGameOver) d.game.winner?.trump else null,
        )
    }

    private fun buildOpponents(snapshot: GameState): List<ru.anseranser.trumpcards.domain.OpponentState> {
        return snapshot.players().filter { !it.human() }.map { ps ->
            ru.anseranser.trumpcards.domain.OpponentState(
                suit = ps.trump(),
                cardCount = ps.hand().size,
                isEliminated = !ps.gamer(),
            )
        }.sortedBy { it.suit.ordinal }
    }

    fun chooseCard(card: Card) {
        if (running) {
            cardQueue.offer(card)
        }
    }

    fun stop() {
        running = false
        gameThread?.interrupt()
        gameThread = null
        cardQueue.clear()
        // Offer a dummy to unblock take() if waiting
        cardQueue.offer(Card(Card.Suit.SPADES, Card.Rank.ACE))
    }

    fun destroy() {
        stop()
    }
}
