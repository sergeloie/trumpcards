package ru.anseranser.model;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import ru.anseranser.event.GameEvent;
import ru.anseranser.event.GameListener;
import ru.anseranser.event.NopListener;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

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
    private final DeckSize deckSize;
    /**
     * The card dealer (dealing seam). Defaults to the RNG-driven {@link Dealer};
     * tests may replace it via {@link #setCardDealer(CardDealer)} to script an
     * exact layout without touching the RNG.
     */
    private CardDealer dealer;
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
        this(DeckSize.THIRTY_SIX);
    }

    /** Build a 4-player game (one seat per suit) with the given deck size. */
    public Game(DeckSize deckSize) {
        this(defaultPlayers(), deckSize);
    }

    /**
     * Build a game from explicitly provided players, using the default 36-card
     * deck. Kept for backward compatibility; prefer {@link #Game(List, DeckSize)}.
     */
    public Game(List<Player> players) {
        this(players, DeckSize.THIRTY_SIX);
    }

    /**
     * Build a game from explicitly provided players with the given deck size.
     * This is the injection seam for the human seat (R6): a launcher constructs
     * the {@link Player} list, giving the human seat a {@code HumanDecisionStrategy}
     * (from the input layer) and the rest an {@link AiDecisionStrategy}. The
     * domain never decides who is human — that is a composition-root concern.
     * The deck size (36 or 52) is likewise a composition-root choice made at
     * launch (see {@link DeckSize}).
     */
    public Game(List<Player> players, DeckSize deckSize) {
        this.players = new TurnOrder(players);
        this.deckSize = deckSize;
        this.dealer = new Dealer(deckSize);

        scoreboard.init(dealer.takeBaseCards(deckSize.baseRank()));
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

    /**
     * Replace the card dealer, e.g. to inject a deterministic test dealer.
     * Intended to be called before the game starts (before {@link #playGame()} or
     * {@link GameDriver#startGame()}); the scoreboard setup already performed in
     * the constructor is independent of the dealer's pool.
     *
     * @throws NullPointerException if {@code dealer} is null
     */
    public void setCardDealer(CardDealer dealer) {
        this.dealer = java.util.Objects.requireNonNull(dealer, "dealer");
    }

    /** The card dealer currently used by this game (never null). */
    public CardDealer getCardDealer() {
        return dealer;
    }

    // ---------- Serialization seam (I3: SavedGameMapper) ----------
    // Package-private accessors used only by SavedGameMapper (same package).
    // Everything else — the engine and all platform modules — keeps going
    // through the public API, so the persisted layout lives in exactly one place.

    /** Deck size of this game (36 or 52). */
    DeckSize deckSize() {
        return deckSize;
    }

    /** The seat that deals the current round. */
    Player dealerSeat() {
        return dealerSeat;
    }

    void setDealerSeat(Player dealerSeat) {
        this.dealerSeat = dealerSeat;
    }

    /** The RNG whose {@link Rng#getState() state} is persisted. */
    Rng getRng() {
        return rng;
    }

    /** The live scoreboard, so {@link Scoreboard#restore} can repopulate it. */
    Scoreboard getScoreboardStacks() {
        return scoreboard;
    }

    /** Replace the pot contents (used only when loading a saved game). */
    void setPot(List<Card> cards) {
        pot.clear();
        pot.addAll(cards);
    }

    void setRoundsPlayed(int roundsPlayed) {
        this.roundsPlayed = roundsPlayed;
    }

    void setCappedRounds(int cappedRounds) {
        this.cappedRounds = cappedRounds;
    }

    // ---------- Driver state access (I4c) ----------
    // GameDriver is an inner class and could reach straight into the fields
    // above; instead it goes through these named, package-private accessors so
    // the coupling between the driver and the game is explicit and greppable.

    /** The live, mutable pot. The driver applies moves straight against it. */
    List<Card> pot() {
        return pot;
    }

    /** Count one more round played (driver round bookkeeping). */
    void incrementRoundsPlayed() {
        roundsPlayed++;
    }

    /** Count one more capped (move-limited) round (driver round bookkeeping). */
    void incrementCappedRounds() {
        cappedRounds++;
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
        Map<Card.Suit, Player> ownerBySuit = new EnumMap<>(Card.Suit.class);
        for (Player p : players) {
            ownerBySuit.put(p.getTrump(), p);
        }

        List<Transfer> transfers = new ArrayList<>();

        for (Player current : players) {
            for (Card card : current.getHand()) {
                Card.Suit suit = card.suit();
                if (suit == current.getTrump()) continue;

                // A suit with no player at the table has no recipient: skip the
                // transfer (the card stays with its holder) instead of passing it
                // to a null owner. All standard compositions use one seat per suit,
                // so this path is only reachable for custom player sets.
                Player owner = ownerBySuit.get(suit);
                if (owner == null) continue;

                Card.Rank required = scoreboard.nextRequiredRank(suit);
                if (required == null || required == Card.Rank.ACE) continue;

                if (card.rank() == required) {
                    transfers.add(new Transfer(current, card, owner));
                }
            }
        }

        for (Transfer t : transfers) {
            t.from().removeCard(t.card());
            t.to().addCard(t.card());
        }
    }

    // ---------- Round ----------

    /**
     * Hard safety bound on a single round's move count. A normal round ends in a
     * few dozen moves; this guards against the pathological case where, under the
     * "taker stays in the round, drops out only on an empty hand" rule and with no
     * draw pile, two players can pass the same trumps back and forth forever.
     * Such deals are rare: a scan of seeds 0..49999 found only four capped seeds —
     * 12129, 24847, 30649 and 49128 (each caps exactly one round). Seed 0 is NOT
     * pathological any more (it terminates in ~31 rounds); the old "seed 0 loops
     * 2M+ moves" note is obsolete. When the bound is hit we end the round and pick
     * the player (still holding cards) with the most cards as the loser — the
     * natural "last player with cards" outcome. This is a defensive guard, not a
     * rules change: it never fires on a deal that would terminate normally.
     *
     * <p>The capped seeds above are used by the determinism tests
     * ({@code ReplayDeterminismTest}, {@code RoundLevelSimulatorTest}) to prove a
     * capped round is still reproducible.
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
        // rules.md §6: the loser's scoreboard receives the card that is the next
        // rank required for their ladder. Under the game's invariants that is
        // exactly the lowest trump in this pile — every lower trump already sits
        // on the board, and the required card must be here, in the round's last
        // non-empty hand plus the pot (pinned by ScoreboardLadderTest). Naming the
        // required rank states the rule directly instead of relying on that
        // equivalence implicitly.
        Card.Rank required = scoreboard.nextRequiredRank(trump);
        Card card = required == null ? null : pile.stream()
                .filter(c -> c.suit() == trump && c.rank() == required)
                .findFirst()
                .orElse(null);
        if (card == null) {
            // Defensive fallback to the previous lowest-trump selection: under the
            // invariants `required` is always in the pile, so this only matters if
            // an invariant ever breaks — and then it degrades exactly as before.
            card = pile.stream()
                    .filter(c -> c.suit() == trump)
                    .min(Comparator.comparing(c -> c.rank().getValue()))
                    .orElse(null);
        }

        Card pushed = null;
        boolean eliminated;
        if (card != null) {
            pile.remove(card);
            pushed = card;
            eliminated = scoreboard.pushAndEliminates(card);
        } else {
            // No trumps left at all: the ladder is already complete → eliminated.
            eliminated = true;
        }

        // Return remaining cards to the shared deck pool for the next round.
        dealer.returnCards(pile);

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

    /**
     * All cards still in play: every hand, the current pot, every scoreboard
     * stack, and the dealer's deck pool. Between rounds the pot is empty, so at
     * the end of a game the four sources always add up to a full deck.
     */
    public List<Card> allCards() {
        List<Card> all = new ArrayList<>();
        for (Player p : players) {
            all.addAll(p.getHand());
        }
        all.addAll(pot);
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
        Map<Card.Suit, List<Card>> hands = new EnumMap<>(Card.Suit.class);
        for (Player p : players) {
            hands.put(p.getTrump(), new ArrayList<>(p.getHand()));
        }
        return hands;
    }

    // ---------- Stepwise driver ----------

    /** Phases of a game driven step-by-step by {@link GameDriver}. */
    public enum Phase { INITIAL, ROUND_ACTIVE, ROUND_ENDED, GAME_ENDED }

    /**
     * Stepwise driver for the game. Unlike {@link #playGame(Rng)}, which runs
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
            getListener().onEvent(new GameEvent.GameStarted());
            beginRound();
        }

        private void beginRound() {
            setupRound();
            current = dealerSeat();
            moves = 0;
            incrementRoundsPlayed();
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
            List<Card> livePot = pot();
            Card top = livePot.get(livePot.size() - 1);
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
                getListener().onEvent(new GameEvent.AwaitingHumanInput(current.getTrump()));
            }
            for (Player.MoveAction action : current.makeMove(pot())) {
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

        /**
         * Drive one human decision point. The lead/defend/take-pot rule itself is
         * owned by {@link Player#nextActionKind(List, boolean)} (single source of
         * truth, finding C1): this method only turns that decision into a pause
         * (awaiting {@link #resume(Card)}) or an applied action. Both the async
         * path and the blocking {@link #step()} path therefore evaluate moves
         * through the exact same {@link Player} code.
         *
         * @return true if the human's whole turn is done
         */
        private boolean humanTurn() {
            if (pendingAction == null) {
                Player.ActionKind kind = current.nextActionKind(pot(), leadingNewTrick);
                if (kind == Player.ActionKind.TAKE_POT) {
                    // No legal defense: taking the pot is not a choice, so never pause.
                    emit(current.applyTakePot(pot()), current);
                    leadingNewTrick = false;
                    return true;
                }
                pendingAction = kind == Player.ActionKind.LEAD
                        ? PendingHumanAction.LEAD
                        : PendingHumanAction.DEFEND;
                getListener().onEvent(new GameEvent.AwaitingHumanInput(current.getTrump()));
                awaitingHuman = true;
                return false;
            }
            // Resumed: apply the pending action with the supplied card.
            PendingHumanAction action = pendingAction;
            pendingAction = null;
            awaitingHuman = false;
            if (action == PendingHumanAction.LEAD) {
                emit(current.applyLead(pot(), forcedCard), current);
                leadingNewTrick = false;
                return true; // a lead ends the turn
            }
            // DEFEND
            emit(current.applyDefense(pot(), forcedCard), current);
            if (current.getHand().isEmpty()) {
                return true; // the beat emptied the hand: turn is over, no follow-up lead
            }
            leadingNewTrick = true; // after beating, the same player leads a new trick
            forcedCard = null;
            return humanTurn(); // loop: the follow-up lead will yield again
        }

        private void advanceAfterTurn() {
            current = getPlayers().nextActive(current, p -> p.isGamer() && !p.getHand().isEmpty());
            moves++;
            if (RoundResolver.countActiveWithCards(getPlayers()) <= 1 || moves >= MAX_ROUND_MOVES) {
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
                getListener().onEvent(new GameEvent.CardPlayed(actor.getTrump(), e.card()));
            } else if (action instanceof Player.MoveAction.Beat e) {
                getListener().onEvent(new GameEvent.CardBeaten(actor.getTrump(), e.attacking(), e.beating()));
            } else if (action instanceof Player.MoveAction.TookPot e) {
                getListener().onEvent(new GameEvent.PotTaken(actor.getTrump(), e.topCard(), e.potSize()));
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

            Player loser = resolveLoser();

            boolean eliminated = endRound(loser);
            if (eliminated) {
                loser.setGamer(false);
            }

            setDealerSeat(advanceDealer(loser));

            if (RoundResolver.countActive(getPlayers()) > 1) {
                beginRound();
            } else {
                endGame();
            }
        }

        /**
         * Pick the round's loser: the player with the most cards left when the
         * round hit the move cap (a capped round — the counter is bumped here),
         * otherwise the player still holding cards on the normal exit path.
         */
        private Player resolveLoser() {
            if (moves >= MAX_ROUND_MOVES && RoundResolver.countActiveWithCards(getPlayers()) > 1) {
                incrementCappedRounds();
                return RoundResolver.mostCards(getPlayers());
            }
            return RoundResolver.firstWithCards(getPlayers());
        }

        /**
         * The dealer of the next round is the loser of the previous one
         * (house rule). If the loser was eliminated (ladder completed) there
         * is no live dealer, so fall back to the next active gamer.
         */
        private Player advanceDealer(Player loser) {
            return loser.isGamer() ? loser : nextDealer(loser);
        }

        /** Finish the game: only one active gamer remains. */
        private void endGame() {
            phase = Phase.GAME_ENDED;
            getListener().onEvent(new GameEvent.GameEnded(getWinner().getTrump()));
        }

        public boolean isGameOver() { return phase == Phase.GAME_ENDED; }

        public List<Card> getPot() { return new ArrayList<>(pot()); }

        /** The owning {@link Game}, for callers that need the winner after a resumed game ends. */
        public Game getGame() { return Game.this; }

        /**
         * Capture the full game + driver state into a portable {@link SavedGame}
         * (R10). Delegates to {@link SavedGameMapper} — the single place that
         * knows the persisted layout (finding I3). The returned object is
         * JSON-serializable via {@link SavedGame#toJson()}.
         */
        public SavedGame save() {
            return SavedGameMapper.toSaved(Game.this, this);
        }

        /**
         * Snapshot this driver's flow state for {@link SavedGameMapper} (R10);
         * the inverse of {@link #applyDriverState(SavedGame.DriverState)}.
         */
        SavedGame.DriverState toDriverState() {
            return new SavedGame.DriverState(
                    phase.name(),
                    current != null ? current.getTrump().name() : null,
                    moves,
                    asyncInput,
                    awaitingHuman,
                    leadingNewTrick,
                    pendingAction != null ? pendingAction.name() : null,
                    forcedCard);
        }

        /** Restore the driver flow state from a {@link SavedGame.DriverState} (R10). */
        void applyDriverState(SavedGame.DriverState ds) {
            this.phase = Phase.valueOf(ds.phase());
            this.current = ds.currentTrump() != null
                    ? playerByTrump(Game.this, ds.currentTrump())
                    : null;
            this.moves = ds.moves();
            this.asyncInput = ds.asyncInput();
            this.awaitingHuman = ds.awaitingHuman();
            this.leadingNewTrick = ds.leadingNewTrick();
            this.pendingAction = ds.pendingAction() != null
                    ? PendingHumanAction.valueOf(ds.pendingAction())
                    : null;
            this.forcedCard = ds.forcedCard();
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
        return SavedGameMapper.restore(saved, strategyFor);
    }

    /**
     * Resolve the seat whose trump is {@code trumpName}. Package-private because
     * it is shared by {@link SavedGameMapper} and {@link GameDriver#applyDriverState}.
     */
    static Player playerByTrump(Game game, String trumpName) {
        for (Player p : game.players) {
            if (p.getTrump().name().equals(trumpName)) return p;
        }
        throw new IllegalArgumentException("No player with trump " + trumpName);
    }
}
