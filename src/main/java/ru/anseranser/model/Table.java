package ru.anseranser.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Table {
    private final List<Player> players;
    private final List<Card> deck;
    private final Map<Card.Suit, Deque<Card>> scoreboard;
    private final List<Card> bank;
    

    public Table() {
        players = Arrays.stream(Card.Suit.values())
                .map(Player::new)
                .toList();
        deck = new ArrayList<>();
        for (Card.Suit suit : Card.Suit.values()) {
            for (Card.Rank rank : Card.Rank.values()) {
                deck.add(new Card(suit, rank));
            }
        }
        scoreboard = new HashMap<>();
        for (Card.Suit suit : Card.Suit.values()) {
            scoreboard.put(suit, new ArrayDeque<>());
        }
        initScoreboard();
        bank = new ArrayList<>();
    }

    public void startRound() {
        dealCards();
        distributeObligatoryCards();
    }



    // Вызывается после определения проигравшего в раунде
    public void registerLoss(Player loser) {
        Card.Suit trump = loser.getTrump();
        Card.Rank nextRank = nextRankToRemove(trump);

        Card card = extractFromDeck(trump, nextRank);
        scoreboard.get(trump).push(card);

        if (nextRank == Card.Rank.ACE) {
            onPlayerEliminated(loser);
        }
    }

    // Следующий ранг определяется по последней карте, уже лежащей в scoreboard
    private Card.Rank nextRankToRemove(Card.Suit trump) {
        Card.Rank next = nextRequiredRank(trump);
        if (next == null) {
            throw new IllegalStateException(
                    "Нет карт для извлечения: масть " + trump + " уже выбита полностью");
        }
        return next;
    }



    private void onPlayerEliminated(Player player) {
        // например: players.remove(player), объявление проигравшего игру и т.д.
    }

    private void dealCards() {
        Collections.shuffle(deck);

        int playerCount = players.size();
        for (int i = 0; i < deck.size(); i++) {
            players.get(i % playerCount).getHand().add(deck.get(i));
        }
        deck.clear();
    }

    // Возвращает следующий ранг, который "должен" появиться в scoreboard для этой масти.
// null — если масть уже полностью выбита (дошли до туза).
    private Card.Rank nextRequiredRank(Card.Suit suit) {
        Card.Rank lastRemoved = scoreboard.get(suit).peek().getRank();
        Card.Rank[] ranks = Card.Rank.values();
        int nextOrdinal = lastRemoved.ordinal() + 1;

        if (nextOrdinal >= ranks.length) {
            return null;
        }
        return ranks[nextOrdinal];
    }


    private record Transfer(Player from, Card card, Player to) {}

    private void distributeObligatoryCards() {
        Map<Card.Suit, Player> ownerBySuit = players.stream()
                .collect(Collectors.toMap(Player::getTrump, Function.identity()));

        List<Transfer> transfers = new ArrayList<>();

        for (Player player : players) {
            for (Card card : player.getHand()) {
                Card.Suit suit = card.getSuit();

                if (suit == player.getTrump()) {
                    continue; // это его собственный козырь, а не чужой
                }

                Card.Rank required = nextRequiredRank(suit);
                if (required == null || required == Card.Rank.ACE) {
                    continue; // масть выбита целиком, либо это туз — тузов не отдаём
                }

                if (card.getRank() == required) {
                    transfers.add(new Transfer(player, card, ownerBySuit.get(suit)));
                }
            }
        }

        for (Transfer t : transfers) {
            t.from().getHand().remove(t.card());
            t.to().getHand().add(t.card());
        }
    }

    //Handwritten methods behind this line

    // Кладём шестёрки всех мастей на scoreboard в начале игры
    private void initScoreboard() {
        for (Card.Suit suit : Card.Suit.values()) {
            Card six = extractFromDeck(suit, Card.Rank.SIX);
            scoreboard.get(suit).push(six);
        }
    }

    // Удаляет карту из колоды и возращает её
    private Card extractFromDeck(Card.Suit suit, Card.Rank rank) {
        Iterator<Card> it = deck.iterator();
        while (it.hasNext()) {
            Card c = it.next();
            if (c.getSuit() == suit && c.getRank() == rank) {
                it.remove();
                return c;
            }
        }
        throw new IllegalStateException(
                "Card " + rank + " suit " + suit + " not found in deck");
    }

    private void endRound(Player loser) {
        deck.addAll(loser.getHand());
        loser.getHand().clear();

        deck.addAll(bank);
        bank.clear();

        registerLoss(loser);
    }


}
