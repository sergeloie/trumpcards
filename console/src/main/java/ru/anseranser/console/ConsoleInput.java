package ru.anseranser.console;

import ru.anseranser.engine.Move;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Seat;
import ru.anseranser.i18n.CardLocalizer;
import ru.anseranser.i18n.Messages;

import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;

/**
 * Asks the person at the keyboard for a choice. The engine decides which
 * moves are legal; this class shows them and reads the index of the chosen
 * one.
 */
public final class ConsoleInput {

    private final Scanner scanner;
    private final PrintStream out;
    private final Messages messages;
    private final CardLocalizer cards;

    public ConsoleInput(Scanner scanner, PrintStream out) {
        this(scanner, out, new Messages(), new CardLocalizer());
    }

    public ConsoleInput(Scanner scanner, PrintStream out, Messages messages) {
        this(scanner, out, messages, new CardLocalizer(messages));
    }

    public ConsoleInput(Scanner scanner, PrintStream out, Messages messages, CardLocalizer cards) {
        this.scanner = scanner;
        this.out = out;
        this.messages = messages;
        this.cards = cards;
    }

    /** The index of the legal move the player chose. */
    public int chooseMove(Seat seat, List<Move> moves) {
        String name = cards.suitName(seat.trump());
        out.println(messages.get("event.awaiting_input", name));
        out.println(messages.get("prompt.your_moves", name));
        for (int i = 0; i < moves.size(); i++) {
            out.println(messages.get("prompt.card_index", i + 1, moveName(moves.get(i))));
        }
        while (true) {
            out.print(messages.get("prompt.choose_move", moves.size()));
            Integer choice = readChoice(moves.size());
            if (choice != null) {
                return choice - 1;
            }
            out.println(messages.get("prompt.invalid"));
        }
    }

    /** The rank set of the deck the game is played with. */
    public RankSet chooseRankSet() {
        while (true) {
            out.print(messages.get("prompt.choose_deck"));
            Integer choice = readChoice(2);
            if (choice != null) {
                return choice == 1 ? RankSet.SIX_TO_ACE : RankSet.TWO_TO_ACE;
            }
            out.println(messages.get("prompt.invalid"));
        }
    }

    private String moveName(Move move) {
        if (move instanceof Move.Play play) {
            return cards.cardName(play.card());
        }
        return messages.get("prompt.take_pot");
    }

    private Integer readChoice(int max) {
        try {
            int choice = Integer.parseInt(scanner.nextLine().trim());
            if (choice >= 1 && choice <= max) {
                return choice;
            }
        } catch (NumberFormatException ignored) {
        }
        return null;
    }
}
