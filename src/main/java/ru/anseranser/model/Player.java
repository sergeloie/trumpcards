package ru.anseranser.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Getter
public class Player {

    private final Card.Suit trump;
    private final List<Card> hand = new ArrayList<>();
    private boolean eliminated = false;

    private boolean canBeat(Card attacking, Card defending) {
        if (attacking.getSuit() == defending.getSuit()) {
            return attacking.getRank().getValue() < defending.getRank().getValue();
        } else return defending.getSuit() == trump;
    }

    private List<Card> possibleDefences(Card attacking) {
        return hand.stream()
                .filter(card -> canBeat(attacking, card))
                .collect(Collectors.toList());
    }

    public boolean canBeat(Card attacking) {
        Optional<Card> optionalCard = weakestDefense(attacking);
        return optionalCard.isEmpty();
    }

    public Optional<Card> weakestDefense(Card attacking) {
        return hand.stream()
                .filter(c -> canBeat(attacking, c))
                .min(Comparator
                        .comparing((Card c) -> c.getSuit() == trump) // некозыри сначала (false < true)
                        .thenComparing(c -> c.getRank().getValue()));
    }

    public void takePot(List<Card> pot) {
        hand.addAll(pot);
        pot.clear();
    }

    public void eliminate() {
        this.eliminated = true;
    }
}
