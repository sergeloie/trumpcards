package ru.anseranser.input;

import ru.anseranser.model.Card;
import ru.anseranser.model.Player;

import java.util.List;
import java.util.Scanner;

/**
 * Console implementation of {@link InputProvider}: prompts the user on
 * {@code System.in} and parses the chosen index. Lives in the presentation
 * layer, so it is allowed to depend on {@code Scanner} and console I/O.
 *
 * Introduced in refactor Stage 1 to move all interactive input out of the
 * model (previously embedded in {@code HumanPlayer}).
 */
public class ConsoleInputProvider implements InputProvider {

    private final Scanner scanner;

    public ConsoleInputProvider() {
        this(new Scanner(System.in));
    }

    public ConsoleInputProvider(Scanner scanner) {
        this.scanner = scanner;
    }

    @Override
    public Card chooseLeadCard(Player player, List<Card> hand) {
        System.out.println("\n" + player + " your cards:");
        for (int i = 0; i < hand.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + hand.get(i));
        }
        while (true) {
            System.out.print("Choose card to move (1-" + hand.size() + "): ");
            Integer choice = readChoice(hand.size());
            if (choice != null) {
                return hand.get(choice - 1);
            }
            System.out.println("Invalid input. Try again.");
        }
    }

    @Override
    public Card chooseDefense(Player player, Card attacking, List<Card> validDefenses) {
        System.out.println("\n" + player + " can beat " + attacking + " one of cards:");
        for (int i = 0; i < validDefenses.size(); i++) {
            System.out.println("  " + (i + 1) + ") " + validDefenses.get(i));
        }
        while (true) {
            System.out.print("Choose card to beat (1-" + validDefenses.size() + "): ");
            Integer choice = readChoice(validDefenses.size());
            if (choice != null) {
                return validDefenses.get(choice - 1);
            }
            System.out.println("Invalid input. Try again.");
        }
    }

    private Integer readChoice(int max) {
        try {
            String input = scanner.nextLine().trim();
            int choice = Integer.parseInt(input);
            if (choice >= 1 && choice <= max) {
                return choice;
            }
        } catch (NumberFormatException ignored) {
            // fall through to null
        }
        return null;
    }
}
