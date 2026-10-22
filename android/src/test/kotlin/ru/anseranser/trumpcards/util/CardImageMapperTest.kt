package ru.anseranser.trumpcards.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import ru.anseranser.engine.Card
import java.io.File

class CardImageMapperTest {

    @Test
    fun theNameOfAPictureHoldsTheSuitAndTheNumberOfTheRank() {
        assertEquals("cards/faces/spade_14.png", CardImageMapper.facePath(Card(Card.Suit.SPADES, Card.Rank.ACE)))
        assertEquals("cards/faces/heart_2.png", CardImageMapper.facePath(Card(Card.Suit.HEARTS, Card.Rank.TWO)))
        assertEquals("cards/backs/card_back_0.png", CardImageMapper.backPath())
    }

    @Test
    fun everyCardOfTheDeckHasAPictureInTheAssets() {
        val missing = Card.Suit.entries.flatMap { suit ->
            Card.Rank.entries
                .map { rank -> Card(suit, rank) }
                .map { card -> CardImageMapper.facePath(card) }
                .filter { path -> !File(ASSETS, path).isFile }
        }

        assertTrue(missing.isEmpty(), "the assets are missing: $missing")
    }

    @Test
    fun theBackOfTheCardsIsInTheAssets() {
        assertTrue(File(ASSETS, CardImageMapper.backPath()).isFile, "the back of the cards is missing")
    }

    private companion object {
        const val ASSETS = "src/main/assets"
    }
}
