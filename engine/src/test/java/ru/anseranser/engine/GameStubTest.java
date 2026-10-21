package ru.anseranser.engine;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameStubTest {

    @Test
    void legalMovesAreEmptyWhileTheRulesAreNotWritten() {
        assertEquals(List.of(), Game.legalMoves(state()));
    }

    @Test
    void startIsNotWrittenYet() {
        assertThrows(UnsupportedOperationException.class, () -> Game.start(setup()));
    }

    @Test
    void stepIsNotWrittenYet() {
        assertThrows(UnsupportedOperationException.class, () -> Game.step(state(), 0));
    }

    @Test
    void thePublicSurfaceIsExactlyThreeMethods() {
        List<Method> publicMethods = Arrays.stream(Game.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();

        assertEquals(3, publicMethods.size());
        assertEquals(Set.of("start", "legalMoves", "step"),
                publicMethods.stream().map(Method::getName).collect(Collectors.toSet()));
    }

    private static Setup setup() {
        return new Setup(RankSet.SIX_TO_ACE, List.of(Card.Suit.SPADES, Card.Suit.HEARTS), 42L);
    }

    private static State state() {
        Seat seat = new Seat(Card.Suit.SPADES, List.of(), List.of(), false);
        return new State(List.of(seat), List.of(), List.of(), seat, seat, 0L);
    }
}
