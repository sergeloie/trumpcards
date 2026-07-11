package ru.anseranser.model;

import lombok.Getter;
import lombok.Setter;
import ru.anseranser.event.GameEvent;
import ru.anseranser.input.InputProvider;
import ru.anseranser.input.NoopInputProvider;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
    public void makeMove(List<Card> bank) {
        if (!isRounder()) return;

        if (bank.isEmpty()) {
            playLeadCard(bank);
            if (getHand().isEmpty()) setRounder(false);
            return;
        }

        Card topCard = bank.getLast();
        Optional<Card> defense = weakestDefense(topCard);

        if (defense.isEmpty()) {
            listener.onEvent(new GameEvent.PotTaken(this, topCard, bank.size()));
            takePot(bank);
            return;
        }

        Card beatCard = chooseDefense(topCard, defense.get());
        bank.add(beatCard);
        getHand().remove(beatCard);
        listener.onEvent(new GameEvent.CardBeaten(this, topCard, beatCard));
        if (getHand().isEmpty()) {
            setRounder(false);
            return;
        }

        playLeadCard(bank);
        if (getHand().isEmpty()) setRounder(false);
    }

    @Override
    protected void playLeadCard(List<Card> bank) {
        Card leadCard = input.chooseLeadCard(this, getHand());
        bank.add(leadCard);
        getHand().remove(leadCard);
        listener.onEvent(new GameEvent.CardPlayed(this, leadCard));
    }

    private Card chooseDefense(Card attacking, Card suggestedDefense) {
        List<Card> validDefenses = getHand().stream()
                .filter(c -> canBeat(attacking, c))
                .sorted(Comparator.comparing((Card c) -> c.suit() == getTrump()) // non-trumps first
                        .thenComparing(c -> c.rank().getValue()))
                .toList();

        return input.chooseDefense(this, attacking, validDefenses);
    }
}
