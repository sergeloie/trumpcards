package ru.anseranser.engine;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameSurfaceTest {

    @Test
    void thePublicSurfaceIsExactlyThreeMethods() {
        List<Method> publicMethods = Arrays.stream(Game.class.getDeclaredMethods())
                .filter(method -> Modifier.isPublic(method.getModifiers()))
                .toList();

        assertEquals(3, publicMethods.size());
        assertEquals(Set.of("start", "legalMoves", "step"),
                publicMethods.stream().map(Method::getName).collect(Collectors.toSet()));
    }
}
