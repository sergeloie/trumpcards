package ru.anseranser.model;

import lombok.AccessLevel;
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

/**
 * Orchestrates a full game: deals rounds, runs moves, determines the loser of
 * each round, and eliminates players whose trump ladder completes (ACE).
 *
 * <p>Delegates deck/shuffle/dealing to {@link Dealer} and scoring/elimination
 * rules to {@link Scoreboard} (both extracted in refactor Stage 4). The only
 * remaining responsibilities here are round orchestration and game flow.
 *
 * <h2>Thread-safety contract (R7)</h2>
 * A {@code Game} instance is <strong>not</strong> thread-safe. It must be
 * mutated from a single "engine" thread only (the thread that calls
 * {@link #playGame()}, {@link #step()}, or {@link #resume(Card)}). All public
 * getters return <em>unmodifiable</em> views (see R5), but they still read
 * internal mutable state and must not be called concurrently with a mutation
 * on another thread.
 *
 * <p>UI platforms (desktop, web, mobile) should follow this pattern:
 * <ol>
 *   <li>Run the engine on a dedicated background thread.</li>
 *   <li>When the UI needs to render, capture an immutable {@link GameState}
 *       via {@link #snapshot()} <em>on the engine thread</em> and hand that
 *       snapshot to the UI/render thread. Never share the live {@code Game}.</li>
 *   <li>For human turns, enable async input ({@code driver.setAsyncInput(true)}),
 *       let {@link #step()} return while {@link #isAwaitingHumanInput()} is true,
 *       and resume from the UI thread by calling {@link #resume(Card)} on the
 *       engine thread (e.g. post it back to the engine thread).</li>
 * </ol>
 * This keeps the domain engine free of any UI, threading, or I/O assumptions.
 */
public class Game {
    @Getter
    private final TurnOrder players;
    private final Dealer dealer = new Dealer();
    /**
     * Scoreboard stacks (trump ladders) for rendering. Exposed as an immutable
     * per-suit snapshot (R5) so external code can read but never mutate the
     * ladders through the game's public API.
     */
    @Getter(AccessLevel.NONE)
    private final Scoreboard scoreboard = new Scoreboard();

    /** Immutable per-suit snapshot of the scoreboard ladders (R5). */
    public Map<Card.Suit, List<Card>> getScoreboard() {
        return scoreboard.snapshot();
    }

    private Player dealerSeat;
    private final List<Card> pot = new ArrayList<>();
    @Getter
    private GameListener listener = NopListener.INSTANCE;
    @Setter
    private Rng rng = new SplitMix64();

    // Verification counters (read by GameSimulator / tests). Harmless in production:
    // they only tally what playGame already does, and reset at the start of each game.
    private int roundsPlayed = 0;
    private int cappedRounds = 0;

    int getRoundsPlayed() { return roundsPlayed; }
    int getCappedRounds() { return cappedRounds; }

    public Game() {
        this(defaultPlayers());
    }

    /**
     * Build a game from explicitly provided players. This is the injection seam
     * for the human seat (R6): a launcher constructs the {@link Player} list,
     * giving the human seat a {@code HumanDecisionStrategy} (from the input
     * layer) and the rest an {@link AiDecisionStrategy}. The domain never
     * decides who is human — that is a composition-root concern.
     */
    public Game(List<Player> players) {
        this.players = new TurnOrder(players);

        scoreboard.init(dealer.deck());
        // Default seat; the real first dealer is chosen at random in startGame().
        dealerSeat = players.get(0);
    }

    private static List<Player> defaultPlayers() {
        List<Player> order = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            order.add(new Player(suit));
        }
        return order;
    }

    public void setListener(GameListener listener) {
        this.listener = listener;
    }

    // ---------- Setup / dealing ----------

    public void shuffleAndDeal() {
        shuffleAndDeal(this.rng);
    }

    /** Test seam: same as {@link #shuffleAndDeal()} but uses the supplied RNG. */
    void shuffleAndDeal(Rng rng) {
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
            t.from().removeCard(t.card());
            t.to().addCard(t.card());
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
                dealerSeat.getTrump(),
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
        loser.clearHand();
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
                loser.getTrump(), pushed, snapshotScoreboard(), eliminated));
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
    public void playGame(Rng rng) {
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

    /**
     * Immutable, presentation-agnostic snapshot of the current game state (R2).
     * Every UI should read the game through this rather than the live getters,
     * so the view can never mutate the model and never races with the engine
     * thread. Build it on the engine thread, then hand the copy to the renderer.
     */
    public GameState snapshot() {
        return GameState.of(this);
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

    private Map<Card.Suit, List<Card>> snapshotHands() {
        Map<Card.Suit, List<Card>> hands = new java.util.HashMap<>();
        for (Player p : players) {
            hands.put(p.getTrump(), new ArrayList<>(p.getHand()));
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

        /**
         * When {@code true}, a human turn does NOT block inside {@link Player#makeMove};
         * instead the driver emits {@link GameEvent.AwaitingHumanInput}, yields control
         * (see {@link #isAwaitingHumanInput()}), and resumes when the UI calls
         * {@link #resume(Card)}. This is the web-friendly, non-blocking input model (R4).
         * When {@code false} (default), the engine blocks on the human's
         * {@link ru.anseranser.input.InputProvider} — the model used by the desktop port
         * (engine on a background thread) and by headless simulation.
         */
        @Setter
        private boolean asyncInput = false;

        /** {@code true} while the driver has yielded and is waiting for {@link #resume(Card)}. */
        private boolean awaitingHuman = false;
        /** {@code true} when the next human action is a fresh lead (after a successful beat). */
        private boolean leadingNewTrick = false;
        /** The action the human must supply on the next {@link #resume(Card)}. */
        private PendingHumanAction pendingAction = null;
        /** The card supplied via the most recent {@link #resume(Card)} call. */
        private Card forcedCard = null;

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
            awaitingHuman = false;
            leadingNewTrick = false;
            pendingAction = null;
            forcedCard = null;
        }

        /**
         * Whether the driver is currently paused, waiting for the UI to supply the
         * human's chosen card via {@link #resume(Card)}. Only meaningful when
         * {@link #isAsyncInput()} is {@code true}. While this is {@code true}, the
         * caller must NOT call {@link #step()} again — it should wait for the user's
         * input and then call {@link #resume(Card)}.
         */
        public boolean isAwaitingHumanInput() {
            return awaitingHuman;
        }

        public boolean isAsyncInput() {
            return asyncInput;
        }

        /**
         * Describes what the UI must supply while {@link #isAwaitingHumanInput()}
         * is {@code true}. For a {@link Kind#LEAD} the whole hand is valid; for a
         * {@link Kind#DEFEND} only the cards that legally beat the current top are
         * offered. The UI uses this to enable exactly the legal choices.
         */
        public InputRequest pendingInput() {
            if (!awaitingHuman || pendingAction == null) {
                throw new IllegalStateException("not awaiting human input");
            }
            if (pendingAction == PendingHumanAction.LEAD) {
                return new InputRequest(current.getTrump(), InputRequest.Kind.LEAD,
                        new ArrayList<>(current.getHand()));
            }
            Card top = pot.getLast();
            return new InputRequest(current.getTrump(), InputRequest.Kind.DEFEND,
                    current.validDefenses(top));
        }

        /** What the UI must supply on the next {@link #resume(Card)} call. */
        public record InputRequest(Card.Suit player, Kind kind, List<Card> validCards) {
            /** LEAD: play any card. DEFEND: play a card that beats the current top. */
            public enum Kind { LEAD, DEFEND }
        }

        /**
         * Advance exactly one move. Returns {@code true} while the round is still
         * active (caller may render and call again); returns {@code false} once
         * the round has ended (the caller should then call {@link #finishRound}).
         */
        public boolean step() {
            if (phase != Phase.ROUND_ACTIVE) return false;
            if (current.isHuman() && asyncInput) {
                // Non-blocking path: yield and wait for resume(Card). step() returns
                // true (round still active) but isAwaitingHumanInput() is now true, so
                // the caller must pause and not call step() again until resume().
                boolean turnComplete = humanTurn();
                if (!turnComplete) {
                    return true;
                }
                advanceAfterTurn();
                return !isRoundOver();
            }
            if (current.isHuman()) {
                listener.onEvent(new GameEvent.AwaitingHumanInput(current.getTrump()));
            }
            for (Player.MoveAction action : current.makeMove(pot)) {
                emit(action, current);
            }
            advanceAfterTurn();
            return !isRoundOver();
        }

        /**
         * Resume a paused human turn by supplying the chosen card (async input path, R4).
         * Must only be called while {@link #isAwaitingHumanInput()} is {@code true}.
         * Applies the card, emits the resulting event(s), and — if the human's whole
         * turn is now complete — advances to the next player (exactly as {@link #step()}
         * would have). If the turn needs another decision (e.g. lead after a beat), the
         * driver yields again and {@link #isAwaitingHumanInput()} becomes {@code true}
         * once more.
         */
        public void resume(Card chosen) {
            if (phase != Phase.ROUND_ACTIVE) {
                throw new IllegalStateException("resume() called outside an active round");
            }
            if (!awaitingHuman) {
                throw new IllegalStateException("resume() called but the driver is not awaiting input");
            }
            forcedCard = chosen;
            boolean turnComplete = humanTurn();
            if (turnComplete) {
                advanceAfterTurn();
            }
        }

        /** Drive one human decision point. @return true if the human's whole turn is done. */
        private boolean humanTurn() {
            if (pendingAction == null) {
                // Decide what the human must do next.
                if (pot.isEmpty() || leadingNewTrick) {
                    pendingAction = PendingHumanAction.LEAD;
                } else {
                    Card top = pot.getLast();
                    if (current.validDefenses(top).isEmpty()) {
                        // No legal defense: the only option is to take the pot (no choice).
                        emit(current.takePot(top, pot), current);
                        leadingNewTrick = false;
                        pendingAction = null;
                        return true;
                    }
                    pendingAction = PendingHumanAction.DEFEND;
                }
                listener.onEvent(new GameEvent.AwaitingHumanInput(current.getTrump()));
                awaitingHuman = true;
                return false;
            }
            // Resumed: apply the pending action with the supplied card.
            PendingHumanAction action = pendingAction;
            pendingAction = null;
            awaitingHuman = false;
            if (action == PendingHumanAction.LEAD) {
                emit(current.playLeadCard(pot, forcedCard), current);
                leadingNewTrick = false;
                return true; // a lead ends the turn
            } else { // DEFEND
                Card top = pot.getLast();
                emit(current.beatCard(top, forcedCard, pot), current);
                if (current.getHand().isEmpty()) {
                    return true; // the beat emptied the hand: turn is over, no follow-up lead
                }
                leadingNewTrick = true; // after beating, the same player leads a new trick
                forcedCard = null;
                return humanTurn(); // loop: the follow-up lead will yield again
            }
        }

        private void advanceAfterTurn() {
            current = players.nextActive(current, p -> p.isGamer() && !p.getHand().isEmpty());
            moves++;
            if (countActiveGamersWithCards() <= 1 || moves >= MAX_ROUND_MOVES) {
                phase = Phase.ROUND_ENDED;
            }
        }

        private boolean isRoundOver() {
            return phase == Phase.ROUND_ENDED;
        }

        /** What the human must supply on the next {@link #resume(Card)} call. */
        private enum PendingHumanAction { LEAD, DEFEND }

        /** Translate a domain {@link Player.MoveAction} into the matching event. */
        private void emit(Player.MoveAction action, Player actor) {
            if (action instanceof Player.MoveAction.Led e) {
                listener.onEvent(new GameEvent.CardPlayed(actor.getTrump(), e.card()));
            } else if (action instanceof Player.MoveAction.Beat e) {
                listener.onEvent(new GameEvent.CardBeaten(actor.getTrump(), e.attacking(), e.beating()));
            } else if (action instanceof Player.MoveAction.TookPot e) {
                listener.onEvent(new GameEvent.PotTaken(actor.getTrump(), e.topCard(), e.potSize()));
            }
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
                listener.onEvent(new GameEvent.GameEnded(getWinner().getTrump()));
            }
        }

        public boolean isGameOver() { return phase == Phase.GAME_ENDED; }

        public List<Card> getPot() { return new ArrayList<>(pot); }

        /** The owning {@link Game}, for callers that need the winner after a resumed game ends. */
        public Game getGame() { return Game.this; }

        /**
         * Capture the full game + driver state into a portable {@link SavedGame}
         * (R10). The returned object is JSON-serializable via {@link SavedGame#toJson()}.
         */
        public SavedGame save() {
            List<SavedGame.PlayerState> ps = new ArrayList<>();
            for (Player p : Game.this.players) {
                ps.add(new SavedGame.PlayerState(
                        p.getTrump().name(),
                        new ArrayList<>(p.getHand()),
                        p.isGamer(),
                        p.isHuman()));
            }
            SavedGame.DriverState ds = new SavedGame.DriverState(
                    phase.name(),
                    current != null ? current.getTrump().name() : null,
                    moves,
                    asyncInput,
                    awaitingHuman,
                    leadingNewTrick,
                    pendingAction != null ? pendingAction.name() : null,
                    forcedCard);
            return new SavedGame(
                    1,
                    ps,
                    Game.this.dealerSeat.getTrump().name(),
                    new ArrayList<>(Game.this.pot),
                    Game.this.scoreboard.snapshot(),
                    new ArrayList<>(Game.this.dealer.deck()),
                    Game.this.rng.getState(),
                    Game.this.roundsPlayed,
                    Game.this.cappedRounds,
                    ds);
        }

        /** Restore the driver flow state from a {@link SavedGame.DriverState} (R10). */
        void restore(SavedGame.DriverState ds) {
            this.phase = Phase.valueOf(ds.phase());
            this.current = ds.currentTrump() != null ? playerByTrump(ds.currentTrump()) : null;
            this.moves = ds.moves();
            this.asyncInput = ds.asyncInput();
            this.awaitingHuman = ds.awaitingHuman();
            this.leadingNewTrick = ds.leadingNewTrick();
            this.pendingAction = ds.pendingAction() != null ? PendingHumanAction.valueOf(ds.pendingAction()) : null;
            this.forcedCard = ds.forcedCard();
        }

        private Player playerByTrump(String trumpName) {
            for (Player p : Game.this.players) {
                if (p.getTrump().name().equals(trumpName)) return p;
            }
            throw new IllegalArgumentException("No player with trump " + trumpName);
        }
    }

    /** Create a stepwise driver for this game (see {@link GameDriver}). */
    public GameDriver createDriver() { return new GameDriver(); }

    /**
     * Rebuild a playable game from a previously saved state (R10). The
     * {@code strategyFor} callback re-wires each seat's {@link DecisionStrategy}
     * — the engine stores only a {@code human} flag, so the loader decides (e.g.
     * a human seat gets a {@code HumanDecisionStrategy} bound to the platform's
     * input provider). After this returns, continue with the returned driver:
     * <pre>{@code
     *   GameDriver d = Game.restore(saved, suit -> new AiDecisionStrategy());
     *   while (!d.isGameOver()) { while (d.step()) {} d.finishRound(); }
     * }</pre>
     */
    public static GameDriver restore(SavedGame saved,
                                      java.util.function.Function<Card.Suit, DecisionStrategy> strategyFor) {
        List<Player> players = new ArrayList<>();
        for (SavedGame.PlayerState ps : saved.players()) {
            Card.Suit trump = Card.Suit.valueOf(ps.trump());
            Player p = new Player(trump, strategyFor.apply(trump));
            p.setGamer(ps.gamer());
            for (Card c : ps.hand()) p.addCard(c);
            players.add(p);
        }
        Game g = new Game(players);
        g.dealerSeat = playerByTrump(g, saved.dealerSeatTrump());
        g.scoreboard.restore(saved.scoreboard());
        g.pot.clear();
        g.pot.addAll(saved.pot());
        g.dealer.deck().clear();
        g.dealer.deck().addAll(saved.deckPool());
        g.rng = new SplitMix64();
        g.rng.setState(saved.rngSeed());
        g.roundsPlayed = saved.roundsPlayed();
        g.cappedRounds = saved.cappedRounds();
        GameDriver d = g.createDriver();
        d.restore(saved.driver());
        return d;
    }

    private static Player playerByTrump(Game game, String trumpName) {
        for (Player p : game.players) {
            if (p.getTrump().name().equals(trumpName)) return p;
        }
        throw new IllegalArgumentException("No player with trump " + trumpName);
    }
}
