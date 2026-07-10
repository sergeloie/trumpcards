package ru.anseranser.model;

import ru.anseranser.utils.CircularDoublyLinkedList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

public class Game {
    private final CircularDoublyLinkedList<Player> players;
    private final List<Card> deck;
    private final Map<Card.Suit, Deque<Card>> scoreboard;
    private Player dealer;

    public Game() {
        players = new CircularDoublyLinkedList<>();
        for (Card.Suit suit : Card.Suit.values()) {
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

    // ---------- Setup ----------

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

    // ---------- Dealing ----------

    public void shuffleAndDeal() {
        java.util.Collections.shuffle(deck);

        Player current = players.getNext(dealer);
        for (int i = 0; i < deck.size(); i++) {
            current.getHand().add(deck.get(i));
            current = players.getNext(current);
        }
        deck.clear();
    }

    // ---------- Obligatory-card exchange ----------

    private Card.Rank nextRequiredRank(Card.Suit suit) {
        Deque<Card> stack = scoreboard.get(suit);
        if (stack.isEmpty()) return null;
        Card.Rank lastRemoved = stack.peek().rank();
        Card.Rank[] ranks = Card.Rank.values();
        int nextOrdinal = lastRemoved.ordinal() + 1;
        return nextOrdinal >= ranks.length ? null : ranks[nextOrdinal];
    }

    private record Transfer(Player from, Card card, Player to) {}

    public void distributeObligatoryCards() {
        Map<Card.Suit, Player> ownerBySuit = new HashMap<>();
        Player start = players.getRandom();
        Player current = start;
        do {
            ownerBySuit.put(current.getTrump(), current);
            current = players.getNext(current);
        } while (current != start);

        List<Transfer> transfers = new ArrayList<>();

        current = start;
        do {
            for (Card card : current.getHand()) {
                Card.Suit suit = card.suit();
                if (suit == current.getTrump()) continue;

                Card.Rank required = nextRequiredRank(suit);
                if (required == null || required == Card.Rank.ACE) continue;

                if (card.rank() == required) {
                    transfers.add(new Transfer(current, card, ownerBySuit.get(suit)));
                }
            }
            current = players.getNext(current);
        } while (current != start);

        for (Transfer t : transfers) {
            t.from().getHand().remove(t.card());
            t.to().getHand().add(t.card());
        }
    }

    // ---------- Round helpers ----------

    private void resetRounders() {
        Player start = players.getRandom();
        Player current = start;
        do {
            if (current.isGamer() && !current.getHand().isEmpty()) {
                current.setRounder(true);
            } else {
                current.setRounder(false);
            }
            current = players.getNext(current);
        } while (current != start);
    }

    private int countActiveRounders() {
        int count = 0;
        Player start = players.getRandom();
        Player current = start;
        do {
            if (current.isGamer() && current.isRounder()) count++;
            current = players.getNext(current);
        } while (current != start);
        return count;
    }

    private int countActiveGamers() {
        int count = 0;
        Player start = players.getRandom();
        Player current = start;
        do {
            if (current.isGamer()) count++;
            current = players.getNext(current);
        } while (current != start);
        return count;
    }

    private Player getActivePlayer() {
        Player start = players.getRandom();
        Player current = start;
        do {
            if (current.isGamer() && current.isRounder()) return current;
            current = players.getNext(current);
        } while (current != start);
        return null;
    }

    private Player nextActivePlayer(Player from) {
        Player next = players.getNext(from);
        while (!next.isGamer() || !next.isRounder()) {
            next = players.getNext(next);
        }
        return next;
    }

    // ---------- Round ----------

    private Player playRound() {
        List<Card> bank = new ArrayList<>();

        shuffleAndDeal();
        distributeObligatoryCards();
        resetRounders();

        Player current = dealer;

        while (countActiveRounders() > 1) {
            current.makeMove(bank);
            current = nextActivePlayer(current);
        }

        return getActivePlayer();
    }

    private void registerLoss(Player loser) {
        Card.Suit trump = loser.getTrump();
        Optional<Card> lowestTrump = loser.getHand().stream()
                .filter(c -> c.suit() == trump)
                .min(Comparator.comparing(c -> c.rank().getValue()));

        if (lowestTrump.isPresent()) {
            Card card = lowestTrump.get();
            loser.getHand().remove(card);
            scoreboard.get(trump).push(card);
        }
    }

    private void eliminatePlayers() {
        Player start = players.getRandom();
        Player current = start;
        do {
            if (current.isGamer() && !hasTrumps(current)) {
                current.setGamer(false);
                current.setRounder(false);
            }
            current = players.getNext(current);
        } while (current != start);
    }

    private boolean hasTrumps(Player player) {
        Card.Suit trump = player.getTrump();
        return player.getHand().stream().anyMatch(c -> c.suit() == trump);
    }

    // ---------- Game ----------

    public void playGame() {
        while (countActiveGamers() > 1) {
            Player loser = playRound();
            registerLoss(loser);
            eliminatePlayers();
            dealer = loser;
        }
    }

    public Player getWinner() {
        Player start = players.getRandom();
        Player current = start;
        do {
            if (current.isGamer()) return current;
            current = players.getNext(current);
        } while (current != start);
        return null;
    }

    // ---------- Debug / print ----------

    public void printHands() {
        Player start = players.getRandom();
        Player current = start;
        do {
            System.out.println(current + " hand: " + current.getHand());
            current = players.getNext(current);
        } while (current != start);
    }

    public CircularDoublyLinkedList<Player> getPlayers() {
        return players;
    }

    public Player getDealer() {
        return dealer;
    }
}
