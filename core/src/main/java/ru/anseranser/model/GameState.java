package ru.anseranser.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record GameState(
        Card.Suit trump,
        Map<Card.Suit, List<Card>> scoreboard,
        List<PlayerState> players,
        List<Card> pot) {

    public record PlayerState(Card.Suit trump, boolean gamer, boolean human, List<Card> hand) {}

    public static GameState of(Game game) {
        Map<Card.Suit, List<Card>> scoreboard = copyScoreboard(game.getScoreboard());

        List<PlayerState> players = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            players.add(new PlayerState(
                    p.getTrump(),
                    p.isGamer(),
                    p.isHuman(),
                    Collections.unmodifiableList(new ArrayList<>(p.getHand()))));
        }

        return new GameState(
                game.getTrump(),
                scoreboard,
                Collections.unmodifiableList(players),
                Collections.unmodifiableList(new ArrayList<>(game.getPot())));
    }

    private static Map<Card.Suit, List<Card>> copyScoreboard(Map<Card.Suit, List<Card>> source) {
        var copy = new LinkedHashMap<Card.Suit, List<Card>>();
        source.forEach((suit, stack) ->
                copy.put(suit, Collections.unmodifiableList(new ArrayList<>(stack))));
        return Collections.unmodifiableMap(copy);
    }
}
