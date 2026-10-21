package ru.anseranser.engine;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The hand-written layouts of the golden scenarios: the cards every seat
 * holds, and the deck order that deals them.
 *
 * <p>The old engine's scripted dealer hands the cards to the seats directly.
 * The new engine deals a deck, so a layout is turned into the deck order that
 * hands the same cards to the same seats, in the same order.
 */
final class ScriptedLayout {

    private ScriptedLayout() {
    }

    /**
     * The hands of the seats: the cards pinned to them first, then the rest of
     * the deck shared out among the seats of the fill order.
     */
    static Map<Card.Suit, List<Card>> hands(Map<Card.Suit, List<Card>> pinned, List<Card.Suit> fillOrder) {
        Map<Card.Suit, List<Card>> hands = new EnumMap<>(Card.Suit.class);
        for (Card.Suit suit : Card.Suit.values()) {
            hands.put(suit, new ArrayList<>(pinned.getOrDefault(suit, List.of())));
        }
        Set<Card> used = new HashSet<>();
        for (List<Card> cards : pinned.values()) {
            used.addAll(cards);
        }
        int dealt = 0;
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : RankSet.SIX_TO_ACE.ranks()) {
                if (rank == RankSet.SIX_TO_ACE.baseRank()) {
                    continue;
                }
                Card card = new Card(suit, rank);
                if (used.add(card)) {
                    hands.get(fillOrder.get(dealt++ % fillOrder.size())).add(card);
                }
            }
        }
        return hands;
    }

    /** The setup that deals the hands to the seats, the dealer of the seed included. */
    static Setup setup(Map<Card.Suit, List<Card>> hands, long seed) {
        List<Card.Suit> trumps = NewEngine.TRUMPS;
        int dealer = new SplitMix64(seed).nextInt(trumps.size());
        return new Setup(RankSet.SIX_TO_ACE, trumps, seed, deckOrder(hands, trumps, dealer));
    }

    /**
     * The deck order that deals the hands to the seats, one card at a time,
     * starting at the seat to the left of the dealer.
     */
    static List<Card> deckOrder(Map<Card.Suit, List<Card>> hands, List<Card.Suit> trumps, int dealer) {
        int seats = trumps.size();
        int size = hands.get(trumps.get(0)).size();
        List<Card> deck = new ArrayList<>();
        for (int round = 0; round < size; round++) {
            for (int seat = 0; seat < seats; seat++) {
                deck.add(hands.get(trumps.get((dealer + 1 + seat) % seats)).get(round));
            }
        }
        return deck;
    }
}
