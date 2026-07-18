package ru.anseranser.android.engine

import android.util.Log
import ru.anseranser.input.InputProvider
import ru.anseranser.model.Card
import ru.anseranser.model.Player
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException

private const val TAG = "ComposeInputProvider"

class ComposeInputProvider : InputProvider {

    @Volatile
    private var pending: CompletableFuture<Card>? = null

    @Volatile
    var validChoices: List<Card> = emptyList()
        private set

    /** Called when validChoices changes, so the UI can update highlights. */
    var onValidChoicesChanged: Runnable? = null

    fun selectCard(card: Card) {
        val future = pending ?: return
        if (validChoices.contains(card)) {
            Log.d(TAG, "selectCard: ${card.rank} of ${card.suit}")
            future.complete(card)
        }
    }

    override fun chooseLeadCard(player: Player, hand: List<Card>): Card {
        Log.d(TAG, "chooseLeadCard: ${hand.size} cards in hand")
        return awaitChoices(hand)
    }

    override fun chooseDefense(player: Player, attacking: Card, validDefenses: List<Card>): Card {
        Log.d(TAG, "chooseDefense: vs ${attacking.rank} of ${attacking.suit}, ${validDefenses.size} options")
        return awaitChoices(validDefenses)
    }

    private fun awaitChoices(choices: List<Card>): Card {
        validChoices = choices.toList()
        Log.d(TAG, "awaitChoices: ${choices.size} valid choices set, notifying UI")
        onValidChoicesChanged?.run()
        val future = CompletableFuture<Card>()
        pending = future
        return try {
            future.get()
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RuntimeException("Interrupted while awaiting card choice", e)
        } catch (e: ExecutionException) {
            throw RuntimeException(e)
        } finally {
            pending = null
            validChoices = emptyList()
        }
    }
}
