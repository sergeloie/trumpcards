package ru.anseranser.input;

import ru.anseranser.i18n.Messages;
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
 * model (previously embedded in {@code HumanPlayer}). Refactor Stage 6
 * externalized the prompts into a {@link Messages} ResourceBundle.
 */
public class ConsoleInputProvider implements InputProvider {

    private final Scanner scanner;
    private final Messages messages;

    public ConsoleInputProvider() {
        this(new Scanner(System.in), new Messages());
    }

    public ConsoleInputProvider(Scanner scanner) {
        this(scanner, new Messages());
    }

    public ConsoleInputProvider(Scanner scanner, Messages messages) {
        this.scanner = scanner;
        this.messages = messages;
    }

    @Override
    public Card chooseLeadCard(Player player, List<Card> hand) {
        System.out.println(messages.get("prompt.your_cards", player));
        for (int i = 0; i < hand.size(); i++) {
            System.out.println(messages.get("prompt.card_index", i + 1, hand.get(i)));
        }
        while (true) {
            System.out.print(messages.get("prompt.choose_lead", hand.size()));
            Integer choice = readChoice(hand.size());
            if (choice != null) {
                return hand.get(choice - 1);
            }
            System.out.println(messages.get("prompt.invalid"));
        }
    }

    @Override
    public Card chooseDefense(Player player, Card attacking, List<Card> validDefenses) {
        System.out.println(messages.get("prompt.can_beat", player, attacking));
        for (int i = 0; i < validDefenses.size(); i++) {
            System.out.println(messages.get("prompt.card_index", i + 1, validDefenses.get(i)));
        }
        while (true) {
            System.out.print(messages.get("prompt.choose_beat", validDefenses.size()));
            Integer choice = readChoice(validDefenses.size());
            if (choice != null) {
                return validDefenses.get(choice - 1);
            }
            System.out.println(messages.get("prompt.invalid"));
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
