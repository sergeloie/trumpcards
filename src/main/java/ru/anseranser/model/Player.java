package ru.anseranser.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

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
    @Setter
    private boolean gamer = true;
    @Setter
    private boolean rounder = true;
    @Setter
    private CircularTable table;

    private boolean canBeat(Card attacking, Card defending) {
        if (attacking.getSuit() == defending.getSuit()) {
            return attacking.getRank().getValue() < defending.getRank().getValue();
        } else return defending.getSuit() == trump;
    }

//    public List<Card> possibleDefenses(Card attacking) {
//        return hand.stream()
//                .filter(card -> canBeat(attacking, card))
//                .collect(Collectors.toList());
//    }

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


    @Override
    public String toString() {
        return "Player(trump=" + trump + ", cards=" + hand.size() + ")";
    }

    // ---------- Выбор карты для отбоя ----------

    public Card chooseBeatCard(List<Card> options) {
        return options.stream()
                .min(Comparator.comparing(c -> c.getRank().getValue()))
                .orElseThrow(() -> new IllegalStateException("No cards to hang up"));
    }

    // ---------- Выбор карты для хода ----------

    /**
     * Единая логика хода — как для первого хода в пустой банк (дилер / игрок после
     * взятия банка), так и для хода второй картой после собственного отбоя.
     * <p>
     * Масть для хода ищется по кругу против направления игры, начиная с игрока,
     * предшествующего текущему в общем массиве (для первого элемента массива —
     * это последний элемент). Берётся наименьшая карта первой найденной подходящей
     * масти. Если ни одна из чужих мастей не представлена в руке — ходим своим козырем.
     */
    public Card chooseLeadCard(List<Player> allPlayers) {
        int n = allPlayers.size();
        int myIndex = allPlayers.indexOf(this);

        for (int step = 1; step < n; step++) {
            int idx = ((myIndex - step) % n + n) % n;
            Card.Suit suit = allPlayers.get(idx).getTrump();

            Optional<Card> smallest = hand.stream()
                    .filter(c -> c.getSuit() == suit)
                    .min(Comparator.comparing(c -> c.getRank().getValue()));

            if (smallest.isPresent()) {
                return smallest.get();
            }
        }

        return hand.stream()
                .filter(c -> c.getSuit() == trump)
                .min(Comparator.comparing(c -> c.getRank().getValue()))
                .orElseThrow(() -> new IllegalStateException("No cards to turn"));
    }

    public void makeMove(List<Card> bank, List<Player> allPlayers) {
        Card topCard = bank.getLast();
        if (rounder) {
            if (!canBeat(topCard)) {
                takePot(bank);
            } else {
                Card beatCard = weakestDefense(topCard).get();
                bank.add(beatCard);
                hand.remove(beatCard);
                if (!hand.isEmpty()) {
                    Card attackingCard = chooseLeadCard(allPlayers);
                    bank.add(attackingCard);
                    hand.remove(attackingCard);
                }
            }
        }
        if (hand.isEmpty()) {
            rounder = false;
        }

    }
}
