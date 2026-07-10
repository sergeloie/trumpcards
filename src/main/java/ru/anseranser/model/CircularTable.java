package ru.anseranser.model;

import ru.anseranser.utils.CircularDoublyLinkedList;

import static ru.anseranser.model.Card.*;

public class CircularTable {
    private final CircularDoublyLinkedList<Player> players;

    public CircularTable() {
        players = new CircularDoublyLinkedList<>();
        for (Suit suit : Suit.values()) {
            Player player = new Player(suit);
            players.addLast(player);
        }
    }
}
