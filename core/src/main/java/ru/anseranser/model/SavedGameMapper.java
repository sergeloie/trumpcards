package ru.anseranser.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The single point that maps a live {@link Game} + its {@link Game.GameDriver}
 * to the portable {@link SavedGame} DTO and back (finding I3).
 *
 * <p>Everything about the persisted form lives here: the format {@link #VERSION},
 * the field layout, the {@code DeckSize} default, re-wiring each seat's
 * {@link DecisionStrategy} through the supplied factory, restoring the scoreboard,
 * pot, deck pool and RNG state, and resolving a seat by its trump name. The
 * public entry points {@link Game.GameDriver#save()} and {@link Game#restore} are
 * thin delegates, so {@code state ⇄ SavedGame} can never drift across call sites.
 *
 * <p>{@link Game.GameDriver}'s own flow state is exchanged through
 * {@link Game.GameDriver#toDriverState()} / {@link Game.GameDriver#applyDriverState}
 * so the driver keeps its fields private; those two methods are the only
 * driver-serialization code outside this class.
 */
final class SavedGameMapper {

    /** Persisted format version; bump whenever the on-disk shape changes. */
    static final int VERSION = 1;

    /** Fallback for old saves that predate the {@code deckSize} field. */
    private static final DeckSize DEFAULT_DECK_SIZE = DeckSize.THIRTY_SIX;

    private SavedGameMapper() {
    }

    /** Capture a game + driver into a portable, JSON-serializable {@link SavedGame}. */
    static SavedGame toSaved(Game game, Game.GameDriver driver) {
        List<SavedGame.PlayerState> players = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            players.add(new SavedGame.PlayerState(
                    p.getTrump().name(),
                    new ArrayList<>(p.getHand()),
                    p.isGamer(),
                    p.isHuman()));
        }
        return new SavedGame(
                VERSION,
                game.deckSize(),
                players,
                game.dealerSeat().getTrump().name(),
                game.getPot(),
                game.getScoreboard(),
                new ArrayList<>(game.getCardDealer().deck()),
                game.getRng().getState(),
                game.getRoundsPlayed(),
                game.getCappedRounds(),
                driver.toDriverState());
    }

    /**
     * Rebuild a playable game from a previously saved state. The
     * {@code strategyFor} callback re-wires each seat's {@link DecisionStrategy}
     * — the engine stores only a {@code human} flag, so the loader decides (e.g.
     * a human seat gets a {@code HumanDecisionStrategy} bound to the platform's
     * input provider).
     */
    static Game.GameDriver restore(SavedGame saved,
                                   Function<Card.Suit, DecisionStrategy> strategyFor) {
        List<Player> players = new ArrayList<>();
        for (SavedGame.PlayerState ps : saved.players()) {
            Card.Suit trump = Card.Suit.valueOf(ps.trump());
            Player p = new Player(trump, strategyFor.apply(trump));
            p.setGamer(ps.gamer());
            for (Card c : ps.hand()) {
                p.addCard(c);
            }
            players.add(p);
        }

        DeckSize deckSize = saved.deckSize() != null ? saved.deckSize() : DEFAULT_DECK_SIZE;
        Game game = new Game(players, deckSize);
        game.setDealerSeat(Game.playerByTrump(game, saved.dealerSeatTrump()));
        game.getScoreboardStacks().restore(saved.scoreboard());
        game.setPot(saved.pot());
        game.getCardDealer().setDeck(saved.deckPool());
        game.getRng().setState(saved.rngSeed());
        game.setRoundsPlayed(saved.roundsPlayed());
        game.setCappedRounds(saved.cappedRounds());

        Game.GameDriver driver = game.createDriver();
        driver.applyDriverState(saved.driver());
        return driver;
    }
}
