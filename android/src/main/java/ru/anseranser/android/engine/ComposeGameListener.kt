package ru.anseranser.android.engine

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import ru.anseranser.event.GameEvent
import ru.anseranser.event.GameListener
import ru.anseranser.i18n.CardLocalizer
import ru.anseranser.model.Card
import ru.anseranser.model.Game
import ru.anseranser.model.GameState

/**
 * GameListener that updates a ComposeStateFlow whenever the engine emits events.
 * Captures GameState snapshots on the engine thread (thread-safe) and exposes
 * them as StateFlow for Compose to observe.
 */
class ComposeGameListener(
    private val game: Game,
    private val inputProvider: ComposeInputProvider
) : GameListener {

    private val _snapshot = MutableStateFlow(ComposeSnapshot.empty())
    val snapshot: StateFlow<ComposeSnapshot> = _snapshot.asStateFlow()

    private val log = mutableListOf<String>()
    private val localizer = CardLocalizer(CardLocalizer.Style.LETTERS)

    @Volatile
    private var isGameOver = false

    @Volatile
    private var winnerSuit: Card.Suit? = null

    fun requestRepaint() {
        updateSnapshot(isHumanTurn = inputProvider.validChoices.isNotEmpty())
    }

    override fun onEvent(event: GameEvent) {
        when (event) {
            is GameEvent.GameStarted -> {
                record("Game started.")
            }
            is GameEvent.AwaitingHumanInput -> {
                record("${seat(event.player)} — your turn")
                updateSnapshot(isHumanTurn = true)
                return
            }
            is GameEvent.RoundStarted -> {
                val sb = StringBuilder("Round started. Hands: ")
                for (p in game.players) {
                    if (p.isGamer) sb.append("${p.trump.name}=${p.hand.size} ")
                }
                record(sb.toString())
            }
            is GameEvent.CardPlayed -> {
                record("${seat(event.player)} plays ${card(event.card())}")
            }
            is GameEvent.CardBeaten -> {
                record("${seat(event.player)} beats ${card(event.attacking())} with ${card(event.beating())}")
            }
            is GameEvent.PotTaken -> {
                record("${seat(event.player)} takes the pot (${event.potSize()} cards)")
            }
            is GameEvent.RoundEnded -> {
                event.pushedToScoreboard()?.let {
                    record("${seat(event.loser())} adds ${card(it)} to scoreboard")
                }
                if (event.eliminated()) {
                    record("${seat(event.loser())} is eliminated")
                }
            }
            is GameEvent.GameEnded -> {
                record("Game over. Winner: ${seat(event.winner())}")
                isGameOver = true
                winnerSuit = event.winner()
            }
        }
        updateSnapshot(isHumanTurn = false)
    }

    private fun updateSnapshot(isHumanTurn: Boolean) {
        val state = GameState.of(game)
        _snapshot.value = ComposeSnapshot.capture(
            state = state,
            validChoices = inputProvider.validChoices,
            log = log.toList(),
            isHumanTurn = isHumanTurn,
            isGameOver = isGameOver,
            winnerSuit = winnerSuit
        )
    }

    private fun record(line: String) {
        log.add(line)
        if (log.size > 200) log.removeAt(0)
    }

    private fun seat(suit: Card.Suit) = suit.name

    private fun card(c: Card) = localizer.cardName(c)
}
