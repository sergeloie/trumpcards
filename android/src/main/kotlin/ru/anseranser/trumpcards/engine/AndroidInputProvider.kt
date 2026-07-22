package ru.anseranser.trumpcards.engine

import ru.anseranser.input.InputProvider
import ru.anseranser.model.Card
import ru.anseranser.model.Player

/**
 * InputProvider for Android. Not used in the async input flow
 * (asyncInput=true uses resume(card) directly), but kept as the
 * strategy dependency for HumanDecisionStrategy.
 */
class AndroidInputProvider : InputProvider {

    override fun chooseLeadCard(player: Player, hand: List<Card>): Card {
        throw UnsupportedOperationException("Use async input flow with resume()")
    }

    override fun chooseDefense(player: Player, attacking: Card, validDefenses: List<Card>): Card {
        throw UnsupportedOperationException("Use async input flow with resume()")
    }
}
