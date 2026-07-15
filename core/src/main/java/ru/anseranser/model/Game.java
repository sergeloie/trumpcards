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
    /**
     * -- GETTER --
     * Scoreboard stacks (trump ladders) for rendering.
     */
    @Getter
    private final Scoreboard scoreboard = new Scoreboard();
    private Player dealerSeat;
    private final List<Card> pot = new ArrayList<>();
    private final boolean humanPlayer;
    @Getter
    private GameListener listener = NopListener.INSTANCE;
    @Setter
    private Random rng = new Random();

    // Verification counters (read by GameSimulator / tests). Harmless in production:
    // they only tally what playGame already does, and reset at the start of each game.
    private int roundsPlayed = 0;
    private int cappedRounds = 0;

    int getRoundsPlayed() { return roundsPlayed; }
    int getCappedRounds() { return cappedRounds; }

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

        scoreboard.init(dealer.deck());
        // Default seat; the real first dealer is chosen at random in startGame().
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

    /** Players still in the round: active gamers holding at least one card. */
    private int countActiveGamersWithCards() {
        int count = 0;
        for (Player p : players) {
            if (p.isGamer() && !p.getHand().isEmpty()) count++;
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

    /**
     * Hard safety bound on a single round's move count. A normal round ends in a
     * few dozen moves; this guards against the pathological case where, under the
     * "taker stays in the round, drops out only on an empty hand" rule and with no
     * draw pile, two players can pass the same trumps back and forth forever
     * (proven by GameSimulator: e.g. seed 0 loops 2M+ moves). When the bound is
     * hit we end the round and pick the player (still holding cards) with the
     * most cards as the
     * loser — the natural "last player with cards" outcome. This is a defensive
     * guard, not a rules change: it never fires on a deal that would terminate
     * normally.
     */
    private static final int MAX_ROUND_MOVES = 10_000;

    private void setupRound() {
        pot.clear();
        shuffleAndDeal();
        distributeObligatoryCards();

        listener.onEvent(new GameEvent.RoundStarted(
                dealerSeat,
                snapshotScoreboard(),
                snapshotHands()));
    }

    /**
        for (Player p : players) {
            if (p.isGamer() && !p.getHand().isEmpty()) {
                return p;
            }
        }
        return null;
    }

    /** Fallback loser for a capped (non-terminating) round: most cards in hand. */
    private Player mostCardsRounder() {
        Player best = null;
        for (Player p : players) {
            if (p.isGamer() && !p.getHand().isEmpty()) {
                if (best == null || p.getHand().size() > best.getHand().size()) {
                    best = p;
                }
            }
        }
        return best;
    }

    private Player determineLoser() {
        for (Player p : players) {
            if (p.isGamer() && !p.getHand().isEmpty()) {
                return p;
            }
        }
        return null;
    }

    private Player nextDealer(Player from) {
        return players.nextActive(from, Player::isGamer);
    }

    /** Pick the first dealer at random (house rule for round 1). */
    private void chooseFirstDealer() {
        dealerSeat = players.get(rng.nextInt(players.size()));
    }

    // ---------- Game ----------

    private boolean endRound(Player loser) {
        // Collect all cards the loser still holds plus the round's pot.
        List<Card> pile = new ArrayList<>(loser.getHand());
        loser.getHand().clear();
        pile.addAll(pot);
        pot.clear();

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
     * Test seam: run a full game with a specific RNG, synchronously. Implemented
     * on top of {@link GameDriver} so the synchronous and stepwise paths share
     * one rule engine (no duplicated move logic).
     */
    public void playGame(Random rng) {
        this.rng = rng;
        roundsPlayed = 0;
        cappedRounds = 0;
        GameDriver d = createDriver();
        d.startGame();
        while (!d.isGameOver()) {
            while (d.step()) {
                // synchronous run: no pause between moves
            }
            d.finishRound();
        }
    }

    public Player getWinner() {
        for (Player p : players) {
            if (p.isGamer()) return p;
        }
        return null;
    }

    /** Trump suit of the current deal (the dealer's own suit). */
    public Card.Suit getTrump() {
        return dealerSeat.getTrump();
    }

    /** Live pot (cards in the middle of the current trick). Empty between rounds. */
    public List<Card> getPot() {
        return new ArrayList<>(pot);
    }

    /** All cards still in play: every hand, every scoreboard stack, and the dealer's deck pool. */
    public List<Card> allCards() {
        List<Card> all = new ArrayList<>();
        for (Player p : players) {
            all.addAll(p.getHand());
        }
        for (List<Card> stack : scoreboard.snapshot().values()) {
            all.addAll(stack);
        }
        all.addAll(dealer.deck());
        return all;
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

    // ---------- Stepwise driver ----------

    /** Phases of a game driven step-by-step by {@link GameDriver}. */
    public enum Phase { INITIAL, ROUND_ACTIVE, ROUND_ENDED, GAME_ENDED }

    /**
     * Stepwise driver for the game. Unlike {@link #playGame(Random)}, which runs
     * a whole game synchronously, this advances the game one move at a time so a
     * presentation layer (desktop / mobile UI) can render each emitted event,
     * animate, and pause between moves.
     *
     * <pre>{@code
     *   GameDriver d = game.createDriver();
     *   d.startGame();
     *   while (!d.isGameOver()) {
     *       while (d.step()) { /* render / animate between moves *\/ }
     *       d.finishRound();
     *   }
     * }</pre>
     *
     * All rule logic (dealing, trick exchange, loser determination, ladder
     * elimination, dealer advance) is shared with {@code playGame} — there is a
     * single source of truth for the game flow.
     */
    public class GameDriver {
        @Getter
        private Player current;
        private int moves;
        @Getter
        private Phase phase = Phase.INITIAL;

        /** Start the game: emit GameStarted and set up the first round. */
        public void startGame() {
            chooseFirstDealer();
            listener.onEvent(new GameEvent.GameStarted());
            beginRound();
        }

        private void beginRound() {
            setupRound();
            current = dealerSeat;
            moves = 0;
            roundsPlayed++;
            phase = Phase.ROUND_ACTIVE;
        }

        /**
         * Advance exactly one move. Returns {@code true} while the round is still
         * active (caller may render and call again); returns {@code false} once
         * the round has ended (the caller should then call {@link #finishRound}).
         */
        public boolean step() {
            if (phase != Phase.ROUND_ACTIVE) return false;
            current.makeMove(pot);
            current = players.nextActive(current, p -> p.isGamer() && !p.getHand().isEmpty());
            moves++;
            if (countActiveGamersWithCards() <= 1 || moves >= MAX_ROUND_MOVES) {
                phase = Phase.ROUND_ENDED;
                return false;
            }
            return true;
        }

        /**
         * Resolve the round that {@link #step()} just ended: pick the loser, push
         * their lowest trump to the scoreboard, eliminate if the ladder
         * completes, advance the dealer, then either begin the next round or end
         * the game.
         */
        public void finishRound() {
            if (phase != Phase.ROUND_ENDED) return;

            Player loser;
            if (moves >= MAX_ROUND_MOVES && countActiveGamersWithCards() > 1) {
                cappedRounds++;
                loser = mostCardsRounder();
            } else {
                loser = determineLoser();
            }

            boolean eliminated = endRound(loser);
            if (eliminated) {
                loser.setGamer(false);
            }
            // House rule: the loser of the previous round deals the next one.
            // If the loser was eliminated (ladder completed) there is no live
            // dealer, so fall back to the next active gamer.
            dealerSeat = loser.isGamer() ? loser : nextDealer(loser);

            if (countActiveGamers() > 1) {
                beginRound();
            } else {
                phase = Phase.GAME_ENDED;
                listener.onEvent(new GameEvent.GameEnded(getWinner()));
            }
        }

        public boolean isGameOver() { return phase == Phase.GAME_ENDED; }

        public List<Card> getPot() { return new ArrayList<>(pot); }
    }

    /** Create a stepwise driver for this game (see {@link GameDriver}). */
    public GameDriver createDriver() { return new GameDriver(); }
}
