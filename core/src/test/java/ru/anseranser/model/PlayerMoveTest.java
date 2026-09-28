package ru.anseranser.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins down {@link Player#nextActionKind(List, boolean)} — the single source of
 * truth for the lead/defend/take-pot branching after finding C1. Both
 * {@link Player#makeMove(List)} and {@link Game.GameDriver} consult this method,
 * so its four cases are locked here directly.
 */
class PlayerMoveTest {

    private static final Card CLUBS_EIGHT = new Card(Card.Suit.CLUBS, Card.Rank.EIGHT);
    private static final Card CLUBS_NINE = new Card(Card.Suit.CLUBS, Card.Rank.NINE);

    private static Player spadesWith(Card card) {
        Player p = new Player(Card.Suit.SPADES);
        p.addCard(card);
        return p;
    }

    @Test
    void emptyPot_isLead() {
        assertEquals(Player.ActionKind.LEAD,
                spadesWith(CLUBS_EIGHT).nextActionKind(List.of(), false));
    }

    @Test
    void potWithALegalDefense_isDefend() {
        assertEquals(Player.ActionKind.DEFEND,
                spadesWith(CLUBS_NINE).nextActionKind(List.of(CLUBS_EIGHT), false));
    }

    @Test
    void potWithNoLegalDefense_isTakePot() {
        // Same suit but a lower rank and not a trump → cannot beat CLUBS NINE.
        assertEquals(Player.ActionKind.TAKE_POT,
                spadesWith(CLUBS_EIGHT).nextActionKind(List.of(CLUBS_NINE), false));
    }

    @Test
    void freshLead_overridesANonEmptyPot() {
        // After a beat the same player leads a new trick even though the pot is not empty.
        assertEquals(Player.ActionKind.LEAD,
                spadesWith(CLUBS_EIGHT).nextActionKind(List.of(CLUBS_NINE), true));
    }
}
