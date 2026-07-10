package ru.anseranser.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import ru.anseranser.utils.CircularDoublyLinkedList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

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
    private CircularDoublyLinkedList<Player> table;

    private boolean canBeat(Card attacking, Card defending) {
        if (attacking.suit() == defending.suit()) {
            return attacking.rank().getValue() < defending.rank().getValue();
        } else return defending.suit() == trump;
    }

    public boolean canBeat(Card attacking) {
        return weakestDefense(attacking).isPresent();
    }

    public Optional<Card> weakestDefense(Card attacking) {
        return hand.stream()
                .filter(c -> canBeat(attacking, c))
                .min(Comparator
                        .comparing((Card c) -> c.suit() == trump) // некозыри сначала (false < true)
                        .thenComparing(c -> c.rank().getValue()));
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
                .min(Comparator.comparing(c -> c.rank().getValue()))
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
    public Card chooseLeadCard() {
        Player current = this;
        for (int i = 0; i < table.size() - 1; i++) {
            current = table.getPrevious(current);
            Card.Suit suit = current.getTrump();

            Optional<Card> smallest = hand.stream()
                    .filter(c -> c.suit() == suit)
                    .min(Comparator.comparing(c -> c.rank().getValue()));

            if (smallest.isPresent()) {
                return smallest.get();
            }
        }

        return hand.stream()
                .filter(c -> c.suit() == trump)
                .min(Comparator.comparing(c -> c.rank().getValue()))
                .orElseThrow(() -> new IllegalStateException("No cards to turn"));
    }

    public void makeMove(List<Card> bank) {
        if (!rounder) return;

        if (bank.isEmpty()) {
            Card leadCard = chooseLeadCard();
            bank.add(leadCard);
            hand.remove(leadCard);
            System.out.println("  " + this + " ходит: " + leadCard);
            if (hand.isEmpty()) rounder = false;
            return;
        }

        Card topCard = bank.getLast();
        Optional<Card> defense = weakestDefense(topCard);

        if (defense.isEmpty()) {
            System.out.println("  " + this + " не может побить " + topCard + " → забирает банк (" + bank.size() + " карт)");
            takePot(bank);
            return;
        }

        Card beatCard = defense.get();
        bank.add(beatCard);
        hand.remove(beatCard);
        System.out.println("  " + this + " побивает " + topCard + " картой " + beatCard);
        if (hand.isEmpty()) { rounder = false; return; }

        Card leadCard = chooseLeadCard();
        bank.add(leadCard);
        hand.remove(leadCard);
        System.out.println("  " + this + " ходит: " + leadCard);
        if (hand.isEmpty()) rounder = false;
    }
}
