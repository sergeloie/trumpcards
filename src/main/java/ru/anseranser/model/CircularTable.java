package ru.anseranser.model;

import ru.anseranser.utils.CircularDoublyLinkedList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static ru.anseranser.model.Card.*;

public class CircularTable {
    private final CircularDoublyLinkedList<Player> players;
    private final List<Card> deck;
    private final Map<Suit, Deque<Card>> scoreboard;
    private Player dealer;

    public CircularTable() {
        players = new CircularDoublyLinkedList<>();
        for (Suit suit : Suit.values()) {
            Player player = new Player(suit);
            player.setTable(players);
            players.addLast(player);
        }

        deck = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                deck.add(new Card(suit, rank));
            }
        }

        scoreboard = new HashMap<>();
        for (Card.Suit suit : Card.Suit.values()) {
            scoreboard.put(suit, new ArrayDeque<>());
        }
        initScoreboard();

        dealer = players.getRandom();
    }

    private void initScoreboard() {
        for (Card.Suit suit : Card.Suit.values()) {
            Card six = extractCard(suit, Card.Rank.SIX);
            scoreboard.get(suit).push(six);
        }
    }

    private Card extractCard(Card.Suit suit, Card.Rank rank) {
        Iterator<Card> it = deck.iterator();
        while (it.hasNext()) {
            Card c = it.next();
            if (c.suit() == suit && c.rank() == rank) {
                it.remove();
                return c;
            }
        }
        throw new IllegalStateException("Card " + rank + " of " + suit + " not found in the deck");
    }
}
