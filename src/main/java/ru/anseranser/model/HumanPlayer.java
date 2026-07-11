package ru.anseranser.model;

import lombok.Getter;
import lombok.Setter;
import ru.anseranser.utils.CircularDoublyLinkedList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

@Getter
public class HumanPlayer extends Player {

    private final Scanner scanner = new Scanner(System.in);

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
            // Human leads - choose card interactively
            Card leadCard = chooseLeadCardInteractive();
            bank.add(leadCard);
            getHand().remove(leadCard);
            System.out.println("  Your move: " + leadCard);
            if (getHand().isEmpty()) setRounder(false);
            return;
        }

        Card topCard = bank.getLast();
        Optional<Card> defense = weakestDefense(topCard);

        if (defense.isEmpty()) {
            System.out.println("  You can`t beat " + topCard + " → take the pot (" + bank.size() + " cards)");
            takePot(bank);
            return;
        }

        // Human can choose which card to beat with
        Card beatCard = chooseDefenseCard(topCard, defense.get());
        bank.add(beatCard);
        getHand().remove(beatCard);
        System.out.println("  You beat " + topCard + " by card " + beatCard);
        if (getHand().isEmpty()) { setRounder(false); return; }

        // Human leads next
        Card leadCard = chooseLeadCardInteractive();
        bank.add(leadCard);
        getHand().remove(leadCard);
        System.out.println("  You put : " + leadCard);
        if (getHand().isEmpty()) setRounder(false);
    }

    private Card chooseLeadCardInteractive() {
        List<Card> hand = getHand();
        System.out.println("\nYour cards:");
        for (int i = 0; i < hand.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + hand.get(i));
        }

        while (true) {
            System.out.print("Choose card to move (1-" + hand.size() + "): ");
            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= hand.size()) {
                    return hand.get(choice - 1);
                }
            } catch (NumberFormatException ignored) {}
            System.out.println("Invalid input. Try again.");
        }
    }

    private Card chooseDefenseCard(Card attacking, Card suggestedDefense) {
        List<Card> validDefenses = getHand().stream()
                .filter(c -> canBeat(attacking, c))
                .sorted(Comparator.comparing((Card c) -> c.suit() == getTrump()) // non-trumps first
                        .thenComparing(c -> c.rank().getValue()))
                .toList();

        System.out.println("\nCan beat " + attacking + " one of cards:");
        for (int i = 0; i < validDefenses.size(); i++) {
            Card c = validDefenses.get(i);
            String marker = (c.equals(suggestedDefense)) ? " (recommended)" : "";
            System.out.println("  " + (i + 1) + ") " + c + marker);
        }
        System.out.println("  " + (validDefenses.size() + 1) + ") Take pot");

        while (true) {
            System.out.print("Choose card (1-" + (validDefenses.size() + 1) + "): ");
            String input = scanner.nextLine().trim();
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= validDefenses.size()) {
                    return validDefenses.get(choice - 1);
                }
                if (choice == validDefenses.size() + 1) {
                    // Take the bank - this shouldn't normally happen since we checked weakestDefense
                    return null;
                }
            } catch (NumberFormatException ignored) {}
            System.out.println("Invalid input. Try again.");
        }
    }
}