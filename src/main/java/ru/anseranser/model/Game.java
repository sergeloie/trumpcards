package ru.anseranser.model;

import lombok.Getter;
import ru.anseranser.event.GameEvent;
import ru.anseranser.event.GameListener;
import ru.anseranser.event.NopListener;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Game {
    @Getter
    private final TurnOrder players;
    private final List<Card> deck;
    private final Map<Card.Suit, Deque<Card>> scoreboard;
    private Player dealer;
    private final List<Card> bank = new ArrayList<>();
    private final boolean humanPlayer;
    @Getter
    private GameListener listener = NopListener.INSTANCE;

    public Game() {
        this(false);
    }

    public Game(boolean humanPlayer) {
        this.humanPlayer = humanPlayer;

        // Create players: human gets SPADES if humanPlayer is true
        List<Player> order = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            Player player;
            if (humanPlayer && suit == Card.Suit.SPADES) {
                player = new HumanPlayer();
            } else {
                player = new Player(suit);
            }
            order.add(player);
        }
        players = new TurnOrder(order);
        for (Player p : players) {
            p.setOrder(players);
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
        dealer = players.get(0);
    }

    public void setListener(GameListener listener) {
        this.listener = listener;
        for (Player p : players) {
            p.setListener(listener);
        }
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

        Player current = players.nextActive(dealer, Player::isGamer);

        for (int i = 0; i < deck.size(); i++) {
            current.getHand().add(deck.get(i));
            current = players.nextActive(current, Player::isGamer);
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
        for (Player p : players) {
            ownerBySuit.put(p.getTrump(), p);
        }

        List<Transfer> transfers = new ArrayList<>();

        for (Player current : players) {
            for (Card card : current.getHand()) {
                Card.Suit suit = card.suit();
                if (suit == current.getTrump()) continue;

                Card.Rank required = nextRequiredRank(suit);
                if (required == null || required == Card.Rank.ACE) continue;

                if (card.rank() == required) {
                    transfers.add(new Transfer(current, card, ownerBySuit.get(suit)));
                }
            }
        }

        for (Transfer t : transfers) {
            t.from().getHand().remove(t.card());
            t.to().getHand().add(t.card());
        }
    }

    // ---------- Round helpers ----------

    private void resetRounders() {
        for (Player p : players) {
            boolean active = p.isGamer() && !p.getHand().isEmpty();
            p.setRounder(active);
        }
    }

    private int countActiveRounders() {
        int count = 0;
        for (Player p : players) {
            if (p.isGamer() && p.isRounder()) count++;
        }
        return count;
    }

    private int countActiveGamers() {
        int count = 0;
        for (Player p : players) {
            if (p.isGamer()) count++;
        }
        return count;
    }

    // ---------- Round ----------

    private Player playRound() {
        setupRound();

        Player current = dealer;
        playMoves(current);

        return determineLoser();
    }

    private void setupRound() {
        bank.clear();
        shuffleAndDeal();
        distributeObligatoryCards();
        resetRounders();

        listener.onEvent(new GameEvent.RoundStarted(
                dealer,
                snapshotScoreboard(),
                snapshotHands()));
    }

    private void playMoves(Player current) {
        while (countActiveRounders() > 1) {
            current.makeMove(bank);
            current = players.nextActive(current, p -> p.isGamer() && p.isRounder());
        }
    }

    private Player determineLoser() {
        for (Player p : players) {
            if (p.isGamer() && p.isRounder() && !p.getHand().isEmpty()) {
                return p;
            }
        }
        return null;
    }

    private Player nextDealer(Player from) {
        return players.nextActive(from, Player::isGamer);
    }

    // ---------- Game ----------

    private boolean endRound(Player loser, List<Card> bank) {
        deck.addAll(loser.getHand());
        loser.getHand().clear();
        deck.addAll(bank);
        bank.clear();

        Card.Suit trump = loser.getTrump();
        Optional<Card> lowestTrump = deck.stream()
                .filter(c -> c.suit() == trump)
                .min(Comparator.comparing(c -> c.rank().getValue()));

        Card pushed = null;
        boolean eliminated;
        if (lowestTrump.isPresent()) {
            Card card = lowestTrump.get();
            deck.remove(card);
            scoreboard.get(trump).push(card);
            pushed = card;
            eliminated = card.rank() == Card.Rank.ACE;
        } else {
            eliminated = true;
        }

        listener.onEvent(new GameEvent.RoundEnded(
                loser, pushed, snapshotScoreboard(), eliminated));
        return eliminated;
    }

    public void playGame() {
        listener.onEvent(new GameEvent.GameStarted());
        while (countActiveGamers() > 1) {
            Player loser = playRound();
            if (loser == null) break;
            boolean eliminated = endRound(loser, bank);
            if (eliminated) {
                loser.setGamer(false);
                loser.setRounder(false);
            }
            dealer = nextDealer(loser);
        }
        listener.onEvent(new GameEvent.GameEnded(getWinner()));
    }

    public Player getWinner() {
        for (Player p : players) {
            if (p.isGamer()) return p;
        }
        return null;
    }

    // ---------- Snapshots (replaces debug/print) ----------

    private Map<Card.Suit, List<Card>> snapshotScoreboard() {
        Map<Card.Suit, List<Card>> snapshot = new HashMap<>();
        for (Card.Suit suit : Card.Suit.values()) {
            snapshot.put(suit, new ArrayList<>(scoreboard.get(suit)));
        }
        return snapshot;
    }

    private Map<Player, List<Card>> snapshotHands() {
        Map<Player, List<Card>> hands = new HashMap<>();
        for (Player p : players) {
            hands.put(p, new ArrayList<>(p.getHand()));
        }
        return hands;
    }
}
