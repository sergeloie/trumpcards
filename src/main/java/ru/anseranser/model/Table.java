package ru.anseranser.model;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class Table {
    private final List<Player> gamePlayers;
    private final List<Card> deck;
    private final Map<Card.Suit, Deque<Card>> scoreboard;
    private final List<Card> bank = new ArrayList<>();
    private final List<Player> eliminationOrder = new ArrayList<>();

    private int dealerIndex;

    public Table() {
        gamePlayers = Arrays.stream(Card.Suit.values())
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
        dealerIndex = new Random().nextInt(gamePlayers.size());
    }

    // ---------- Setup ----------

    private void initScoreboard() {
        for (Card.Suit suit : Card.Suit.values()) {
            Card six = extractCard(suit, Card.Rank.SIX);
            scoreboard.get(suit).push(six);
        }
    }

    private Card extractCard(Card.Suit suit, Card.Rank rank) {
        Iterator<Card> it = deck.iterator();
        while (it.hasNext()) {
            Card c = it.next();
            if (c.getSuit() == suit && c.getRank() == rank) {
                it.remove();
                return c;
            }
        }
        throw new IllegalStateException("Card " + rank + " of " + suit + " not found in the deck");
    }

    // ---------- Game-level circular navigation (across rounds) ----------
/*

    private List<Player> playersInGame() {
        return gamePlayers.stream()
                .filter(p -> !p.isEliminated())
                .collect(Collectors.toList());
    }

    private int nextIndexInGame(int fromIndex) {
        int n = gamePlayers.size();
        int i = fromIndex;
        do {
            i = (i + 1) % n;
        } while (gamePlayers.get(i).isEliminated());
        return i;
    }

    // ---------- Round-level circular navigation (within one round) ----------

    private int countRoundActive() {
        return (int) gamePlayers.stream().filter(Player::isRoundActive).count();
    }

    private Player theOnlyRoundActivePlayer() {
        return gamePlayers.stream()
                .filter(Player::isRoundActive)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active player left in the round"));
    }

    private int nextRoundActiveIndex(int fromIndex) {
        int n = gamePlayers.size();
        int i = fromIndex;
        do {
            i = (i + 1) % n;
        } while (!gamePlayers.get(i).isRoundActive());
        return i;
    }

    private int firstRoundActiveIndexFrom(int fromIndexInclusive) {
        if (gamePlayers.get(fromIndexInclusive).isRoundActive()) {
            return fromIndexInclusive;
        }
        return nextRoundActiveIndex(fromIndexInclusive);
    }

    private void resetRoundActivity() {
        for (Player player : playersInGame()) {
            if (!player.getHand().isEmpty()) {
                player.enterRound();
            }
            // if a player somehow ends up with an empty hand right after dealing
            // and the obligatory-card exchange, they simply never enter the round
        }
    }

    // ---------- Dealing ----------

    private void dealCards() {
        Collections.shuffle(deck);

        List<Player> inGame = playersInGame();
        int n = inGame.size();
        int firstRecipientTableIndex = nextIndexInGame(dealerIndex);
        int start = inGame.indexOf(gamePlayers.get(firstRecipientTableIndex));

        for (int i = 0; i < deck.size(); i++) {
            inGame.get((start + i) % n).getHand().add(deck.get(i));
        }
        deck.clear();
    }

    // ---------- Obligatory-card exchange ----------

    private Card.Rank nextRequiredRank(Card.Suit suit) {
        Card.Rank lastRemoved = scoreboard.get(suit).peek().getRank();
        Card.Rank[] ranks = Card.Rank.values();
        int nextOrdinal = lastRemoved.ordinal() + 1;
        return nextOrdinal >= ranks.length ? null : ranks[nextOrdinal];
    }

    private record Transfer(Player from, Card card, Player to) {}

    private void distributeObligatoryCards() {
        Map<Card.Suit, Player> ownerBySuit = gamePlayers.stream()
                .collect(Collectors.toMap(Player::getTrump, p -> p));

        List<Transfer> transfers = new ArrayList<>();

        for (Player player : gamePlayers) {
            for (Card card : player.getHand()) {
                Card.Suit suit = card.getSuit();
                if (suit == player.getTrump()) continue;

                Card.Rank required = nextRequiredRank(suit);
                if (required == null || required == Card.Rank.ACE) continue;

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

    // ---------- Trick play ----------

    */
/** Plays one lead card for the given player, removing it from hand and marking them
 *  out of the round if that was their last card. *//*

    private void playLeadCard(Player player) {
        Card card = player.chooseLeadCard(gamePlayers);
        player.getHand().remove(card);
        bank.add(card);

        if (player.getHand().isEmpty()) {
            player.leaveRound();
        }
    }

    private Player playTricksUntilLoserFound() {
        int leaderIndex = firstRoundActiveIndexFrom(dealerIndex);

        while (true) {
            bank.clear();

            Player leader = gamePlayers.get(leaderIndex);
            playLeadCard(leader);

            if (countRoundActive() <= 1) {
                return theOnlyRoundActivePlayer();
            }

            int currentIndex = nextRoundActiveIndex(leaderIndex);
            Player taker = null;

            while (taker == null) {
                Player current = gamePlayers.get(currentIndex);
                Card topCard = bank.get(bank.size() - 1);

                List<Card> options = current.possibleDefenses(topCard);

                if (options.isEmpty()) {
                    current.getHand().addAll(bank);
                    bank.clear();
                    taker = current;
                } else {
                    Card beatCard = current.chooseBeatCard(options);
                    current.getHand().remove(beatCard);
                    bank.add(beatCard);

                    if (current.getHand().isEmpty()) {
                        current.leaveRound();
                    }

                    if (countRoundActive() <= 1) {
                        return theOnlyRoundActivePlayer();
                    }

                    // only lead a second card if the player actually has cards left
                    if (current.isRoundActive()) {
                        playLeadCard(current);

                        if (countRoundActive() <= 1) {
                            return theOnlyRoundActivePlayer();
                        }
                    }

                    currentIndex = nextRoundActiveIndex(currentIndex);
                }
            }

            leaderIndex = nextRoundActiveIndex(gamePlayers.indexOf(taker));
        }
    }

    // ---------- End of round ----------



    private void registerLoss(Player loser) {
        Card.Suit trump = loser.getTrump();
        Card.Rank nextRank = nextRequiredRank(trump);
        if (nextRank == null) {
            throw new IllegalStateException("Trump suit " + trump + " has already been fully removed");
        }

        Card card = extractCard(trump, nextRank);
        scoreboard.get(trump).push(card);

        if (nextRank == Card.Rank.ACE) {
            loser.setEliminated(true);
            eliminationOrder.add(loser);
        }
    }

    private void endRound(Player loser) {
        deck.addAll(loser.getHand());
        loser.getHand().clear();

        deck.addAll(bank);
        bank.clear();

        registerLoss(loser);
    }

    // ---------- Public game flow ----------

    public void playRound() {
        dealCards();
        distributeObligatoryCards();
        resetRoundActivity();

        Player loser = playTricksUntilLoserFound();
        int loserIndex = gamePlayers.indexOf(loser);

        endRound(loser);

        dealerIndex = loser.isEliminated()
                ? nextIndexInGame(loserIndex)
                : loserIndex;
    }

    public boolean isGameOver() {
        return playersInGame().size() == 1;
    }

    public Player getWinner() {
        List<Player> remaining = playersInGame();
        if (remaining.size() != 1) {
            throw new IllegalStateException("The game is not over yet");
        }
        return remaining.get(0);
    }

    public void playGame() {
        while (!isGameOver()) {
            playRound();
        }
    }

    public List<Player> getFinalRanking() {
        List<Player> ranking = new ArrayList<>(eliminationOrder);
        ranking.add(getWinner());
        return ranking;
    }
}*/
}