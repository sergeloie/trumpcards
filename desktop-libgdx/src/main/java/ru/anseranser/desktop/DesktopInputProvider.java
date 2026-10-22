package ru.anseranser.desktop;

import ru.anseranser.engine.Card;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.State;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * The click of the person at the mouse, resolved to the index of a legal move.
 *
 * <p>The engine thread asks for a move and blocks until the person clicks a
 * card of the hand, or the button that takes the pot. The click arrives on the
 * render thread; the index it resolves to is the one the engine wants, and a
 * click on a card that is not legal is ignored.</p>
 *
 * <p>Because the engine blocks inside {@link #choose}, the game loop must run
 * on a thread separate from the LibGDX render thread, which is the one
 * dispatching the clicks. See {@link DesktopLauncher}.</p>
 */
final class DesktopInputProvider implements MoveChooser {

    private volatile CompletableFuture<Integer> pending;
    private volatile List<Move> legalMoves = List.of();
    private volatile Runnable repaintHook = () -> {};

    /**
     * Register the callback the provider invokes when the legal moves become
     * known, so that the screen can mark the cards that may be played.
     */
    void setRepaintHook(Runnable hook) {
        this.repaintHook = (hook != null) ? hook : () -> {};
    }

    @Override
    public int choose(State state, List<Move> moves) {
        CompletableFuture<Integer> choice = new CompletableFuture<>();
        pending = choice;
        legalMoves = List.copyOf(moves);
        repaintHook.run();
        try {
            return choice.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while awaiting the player's choice", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            pending = null;
            legalMoves = List.of();
        }
    }

    /** The click on a card: the index of the move that plays it. */
    void onCardClicked(Card card) {
        int index = indexOfCard(legalMoves, card);
        if (index >= 0) {
            complete(index);
        }
    }

    /** The click on the button that takes the pot. */
    void onTakePotClicked() {
        int index = indexOfTakePot(legalMoves);
        if (index >= 0) {
            complete(index);
        }
    }

    /** The legal moves of the pending choice. Empty when no choice is pending. */
    List<Move> legalMoves() {
        return legalMoves;
    }

    private void complete(int index) {
        CompletableFuture<Integer> choice = pending;
        if (choice != null) {
            choice.complete(index);
        }
    }

    /** The index of the move that plays the card, or -1 when it is not legal. */
    static int indexOfCard(List<Move> moves, Card card) {
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i) instanceof Move.Play play && play.card().equals(card)) {
                return i;
            }
        }
        return -1;
    }

    /** The index of the move that takes the pot, or -1 when it is not legal. */
    static int indexOfTakePot(List<Move> moves) {
        for (int i = 0; i < moves.size(); i++) {
            if (moves.get(i) instanceof Move.TakePot) {
                return i;
            }
        }
        return -1;
    }
}
