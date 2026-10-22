package ru.anseranser.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.DIAMONDS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

/**
 * A whole game, played by a policy that always takes the first legal move.
 */
class GameLoopTest {

    @Test
    void aWholeGameRunsToOneSurvivingSeat() {
        State state = Game.start(new Setup(RankSet.SIX_TO_ACE,
                List.of(SPADES, CLUBS, DIAMONDS, HEARTS), 42L)).state();

        int cards = RankSet.SIX_TO_ACE.cardCount(4);
        int moves = 0;
        int rounds = 1;
        int turns = state.turns();
        while (!Game.legalMoves(state).isEmpty()) {
            State next = Game.step(state, 0).state();
            if (next.turns() < turns) {
                rounds++;
            }
            turns = next.turns();
            state = next;
            moves++;
            if (Seats.countInGame(state.seats()) > 1) {
                assertFalse(state.turn().hand().isEmpty(),
                        "the seat to move can act while the game is not over");
            }
        }

        assertEquals(1, Seats.countInGame(state.seats()), "one seat is left in the game");
        assertEquals(cards, cardsInPlay(state), "every card is in exactly one place");
        assertTrue(rounds > 1, "the game lasts more than one round");
        assertTrue(moves > 100, "the game lasts more than a hundred moves");
    }

    private static int cardsInPlay(State state) {
        int total = state.pot().size() + state.deck().size();
        for (Seat seat : state.seats()) {
            total += seat.hand().size() + seat.board().size();
        }
        return total;
    }
}
