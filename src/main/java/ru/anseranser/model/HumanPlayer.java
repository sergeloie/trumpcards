package ru.anseranser.model;

import lombok.Getter;
import lombok.Setter;
import ru.anseranser.event.GameEvent;
import ru.anseranser.input.InputProvider;
import ru.anseranser.input.NoopInputProvider;

import java.util.Comparator;
import java.util.List;

/**
 * A human-controlled player. Shares the entire move mechanic with {@link Player}
 * (see {@link Player#makeMove}); only the two decision points are overridden to
 * ask an {@link InputProvider} instead of applying the AI heuristic:
 * <ul>
 *   <li>{@link #playLeadCard} — which card to lead with;</li>
 *   <li>{@link #chooseDefenseCard} — which card to defend with.</li>
 * </ul>
 * No {@code makeMove} duplication (removed in refactor Stage 2).
 */
@Getter
public class HumanPlayer extends Player {

    @Setter
    private InputProvider input = NoopInputProvider.INSTANCE;

    public HumanPlayer() {
        super(Card.Suit.SPADES);
    }

    public HumanPlayer(Card.Suit trump) {
        super(trump);
    }

    @Override
    protected void playLeadCard(List<Card> bank) {
        Card leadCard = input.chooseLeadCard(this, getHand());
        bank.add(leadCard);
        getHand().remove(leadCard);
        listener.onEvent(new GameEvent.CardPlayed(this, leadCard));
    }

    @Override
    protected Card chooseDefenseCard(Card attacking, Card weakest) {
        List<Card> validDefenses = getHand().stream()
                .filter(c -> canBeat(attacking, c))
                .sorted(Comparator.comparing((Card c) -> c.suit() == getTrump()) // non-trumps first
                        .thenComparing(c -> c.rank().getValue()))
                .toList();

        return input.chooseDefense(this, attacking, validDefenses);
    }
}
