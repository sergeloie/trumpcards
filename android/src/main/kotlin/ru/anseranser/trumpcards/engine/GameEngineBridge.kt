package ru.anseranser.trumpcards.engine

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.anseranser.bots.RuleBot
import ru.anseranser.engine.Card
import ru.anseranser.engine.Game
import ru.anseranser.engine.Move
import ru.anseranser.engine.RankSet
import ru.anseranser.engine.Setup
import ru.anseranser.engine.State
import ru.anseranser.trumpcards.domain.GameState
import ru.anseranser.trumpcards.domain.GameStateMapper

/**
 * The bridge between the engine and the screens.
 *
 * <p>The game is played on the thread of the front end: the engine is asked
 * for the legal moves, the person at the table picks one of them and the
 * bridge hands the index of that move on. The other seats are played by the
 * rule bot.</p>
 */
class GameEngineBridge(
    private val humanSeat: Card.Suit,
    private val botMoveDelayMs: Long = BOT_MOVE_DELAY_MS,
) {

    companion object {
        /** The suits in play, one per seat, in the seating order. */
        private val TRUMPS = listOf(
            Card.Suit.SPADES,
            Card.Suit.CLUBS,
            Card.Suit.DIAMONDS,
            Card.Suit.HEARTS,
        )

        /** The pause after a move of the bot, so that the person sees it. */
        private const val BOT_MOVE_DELAY_MS = 300L
    }

    private val _state = MutableStateFlow(GameState())
    val state: StateFlow<GameState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var gameJob: Job? = null

    @Volatile
    private var pendingChoice: CompletableDeferred<Int>? = null

    @Volatile
    private var pendingMoves: List<Move> = emptyList()

    /** Starts a game of the rank set given, and plays it to its end. */
    fun startGame(rankSet: RankSet) {
        stop()
        val setup = Setup(rankSet, TRUMPS, System.nanoTime())
        gameJob = scope.launch { play(setup, rankSet) }
    }

    /** Answers the question the engine asked: the move the person picked. */
    fun choose(choice: MoveChoice) {
        val index = MoveChoices.indexOf(pendingMoves, choice)
        if (index >= 0) {
            pendingChoice?.complete(index)
        }
    }

    /** Stops the game that is being played, if there is one. */
    fun stop() {
        gameJob?.cancel()
        gameJob = null
        pendingChoice?.cancel()
        pendingChoice = null
        pendingMoves = emptyList()
    }

    private suspend fun play(setup: Setup, rankSet: RankSet) {
        var state = Game.start(setup).state()
        while (true) {
            val moves = Game.legalMoves(state)
            if (moves.isEmpty()) {
                publish(state, moves, rankSet)
                return
            }
            val index = if (state.turn().trump() == humanSeat) {
                awaitHumanMove(state, moves, rankSet)
            } else {
                publish(state, moves, rankSet)
                delay(botMoveDelayMs)
                RuleBot.choose(state, moves)
            }
            state = Game.step(state, index).state()
        }
    }

    private suspend fun awaitHumanMove(state: State, moves: List<Move>, rankSet: RankSet): Int {
        val choice = CompletableDeferred<Int>()
        pendingChoice = choice
        pendingMoves = moves
        publish(state, moves, rankSet)
        try {
            return choice.await()
        } finally {
            pendingChoice = null
            pendingMoves = emptyList()
        }
    }

    private fun publish(state: State, moves: List<Move>, rankSet: RankSet) {
        _state.value = GameStateMapper.of(state, moves, humanSeat, rankSet)
    }
}
