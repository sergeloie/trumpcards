package ru.anseranser.model;

import lombok.Getter;
import lombok.Setter;
import ru.anseranser.event.GameEvent;
import ru.anseranser.event.GameListener;
import ru.anseranser.event.NopListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Orchestrates a full game: deals rounds, runs moves, determines the loser of
 * each round, and eliminates players whose trump ladder completes (ACE).
 *
 * <p>Delegates deck/shuffle/dealing to {@link Dealer} and scoring/elimination
 * rules to {@link Scoreboard} (both extracted in refactor Stage 4). The only
 * remaining responsibilities here are round orchestration and game flow.
 */
public class Game {
    @Getter
    private final TurnOrder players;
    private final Dealer dealer = new Dealer();
    private final Scoreboard scoreboard = new Scoreboard();
    private Player dealerSeat;
    private final List<Card> bank = new ArrayList<>();
    private final boolean humanPlayer;
    @Getter
    private GameListener listener = NopListener.INSTANCE;
    @Setter
    private Random rng = new Random();

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

        scoreboard.init(dealer.deck());
        dealerSeat = players.get(0);
    }

    public void setListener(GameListener listener) {
        this.listener = listener;
        for (Player p : players) {
            p.setListener(listener);
        }
    }

    // ---------- Setup / dealing ----------

    public void shuffleAndDeal() {
        shuffleAndDeal(this.rng);
    }

    /** Test seam: same as {@link #shuffleAndDeal()} but uses the supplied RNG. */
    void shuffleAndDeal(Random rng) {
        dealer.shuffle(rng);
        dealer.deal(players, dealerSeat, Player::isGamer);
    }

    // ---------- Obligatory-card exchange ----------

    private record Transfer(Player from, Card card, Player to) {}

    public void distributeObligatoryCards() {
        Map<Card.Suit, Player> ownerBySuit = new java.util.HashMap<>();
        for (Player p : players) {
            ownerBySuit.put(p.getTrump(), p);
        }

        List<Transfer> transfers = new ArrayList<>();

        for (Player current : players) {
            for (Card card : current.getHand()) {
                Card.Suit suit = card.suit();
                if (suit == current.getTrump()) continue;

                Card.Rank required = scoreboard.nextRequiredRank(suit);
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

        Player current = dealerSeat;
        playMoves(current);

        return determineLoser();
    }

    private void setupRound() {
        bank.clear();
        shuffleAndDeal();
        distributeObligatoryCards();
        resetRounders();

        listener.onEvent(new GameEvent.RoundStarted(
                dealerSeat,
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

    private boolean endRound(Player loser) {
        // Collect all cards the loser still holds plus the round's bank.
        List<Card> pile = new ArrayList<>(loser.getHand());
        loser.getHand().clear();
        pile.addAll(bank);
        bank.clear();

        Card.Suit trump = loser.getTrump();
        Optional<Card> lowestTrump = pile.stream()
                .filter(c -> c.suit() == trump)
                .min(Comparator.comparing(c -> c.rank().getValue()));

        Card pushed = null;
        boolean eliminated;
        if (lowestTrump.isPresent()) {
            Card card = lowestTrump.get();
            pile.remove(card);
            pushed = card;
            eliminated = scoreboard.pushAndEliminates(card);
        } else {
            // No trumps left at all: the ladder is already complete → eliminated.
            eliminated = true;
        }

        // Return remaining cards to the shared deck pool for the next round.
        dealer.deck().addAll(pile);

        listener.onEvent(new GameEvent.RoundEnded(
                loser, pushed, snapshotScoreboard(), eliminated));
        return eliminated;
    }

    public void playGame() {
        playGame(this.rng);
    }

    /**
     * Test seam: run a full game with a specific RNG. With the same seed and
     * identical player setup, two invocations produce bit-identical games
     * (the only non-determinism in the engine was the shuffle, now injected).
     */
    public void playGame(Random rng) {
        this.rng = rng;
        listener.onEvent(new GameEvent.GameStarted());
        while (countActiveGamers() > 1) {
            Player loser = playRound();
            if (loser == null) break;
            boolean eliminated = endRound(loser);
            if (eliminated) {
                loser.setGamer(false);
                loser.setRounder(false);
            }
            dealerSeat = nextDealer(loser);
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
        return scoreboard.snapshot();
    }

    private Map<Player, List<Card>> snapshotHands() {
        Map<Player, List<Card>> hands = new java.util.HashMap<>();
        for (Player p : players) {
            hands.put(p, new ArrayList<>(p.getHand()));
        }
        return hands;
    }
}
