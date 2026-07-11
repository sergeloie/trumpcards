package ru.anseranser.desktop;

import ru.anseranser.input.InputProvider;
import ru.anseranser.model.Card;
import ru.anseranser.model.Player;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * LibGDX click-driven {@link InputProvider}.
 *
 * <p>The engine calls {@link #chooseLeadCard} / {@link #chooseDefense} from the
 * game-loop thread and blocks until the human clicks a card in the UI. The
 * {@link GameScreen} forwards card clicks to {@link #onCardClicked}, which
 * resolves the pending choice — but only if the clicked card is currently a
 * valid choice (otherwise the click is ignored).</p>
 *
 * <p>Because the engine blocks inside {@code chooseLeadCard}/{@code chooseDefense},
 * the game loop must run on a thread separate from the LibGDX render thread
 * (which is the one dispatching the clicks). See {@link DesktopLauncher}.</p>
 */
public final class DesktopInputProvider implements InputProvider {

    private volatile CompletableFuture<Card> pending;
    private volatile List<Card> validChoices = List.of();

    @Override
    public Card chooseLeadCard(Player player, List<Card> hand) {
        return await(hand);
    }

    @Override
    public Card chooseDefense(Player player, Card attacking, List<Card> validDefenses) {
        return await(validDefenses);
    }

    private Card await(List<Card> choices) {
        validChoices = List.copyOf(choices);
        CompletableFuture<Card> f = new CompletableFuture<>();
        pending = f;
        try {
            return f.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while awaiting card choice", e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            pending = null;
            validChoices = List.of();
        }
    }

    /** Called by the UI when a card widget is clicked. Resolves the pending choice if valid. */
    public void onCardClicked(Card card) {
        CompletableFuture<Card> f = pending;
        if (f != null && validChoices.contains(card)) {
            f.complete(card);
        }
    }

    /** Currently acceptable cards (for highlighting in the view). Empty when not awaiting. */
    public List<Card> validChoices() {
        return validChoices;
    }
}
