package ru.anseranser.ml;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.anseranser.engine.Card;
import ru.anseranser.engine.Game;
import ru.anseranser.engine.Move;
import ru.anseranser.engine.RankSet;
import ru.anseranser.engine.Seat;
import ru.anseranser.engine.Setup;
import ru.anseranser.engine.State;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static ru.anseranser.engine.Card.Suit.CLUBS;
import static ru.anseranser.engine.Card.Suit.DIAMONDS;
import static ru.anseranser.engine.Card.Suit.HEARTS;
import static ru.anseranser.engine.Card.Suit.SPADES;

/**
 * The policy that runs the model: it loads the model, reads the vector of the
 * seat to move, masks the scores and turns the best action into the index of a
 * legal move.
 */
class OnnxPolicyTest {

    @TempDir
    Path directory;

    @Test
    void thePolicyChoosesTheActionTheModelFavours() throws IOException {
        State state = leadState();
        List<Move> moves = Game.legalMoves(state);
        int four = Vector.actionOf(card(SPADES, Card.Rank.FOUR));
        int eight = Vector.actionOf(card(SPADES, Card.Rank.EIGHT));

        try (OnnxPolicy policy = new OnnxPolicy(modelWith(favouring(four)))) {
            assertEquals(0, policy.choose(state, moves), "the model favours the four of spades");
        }

        try (OnnxPolicy policy = new OnnxPolicy(modelWith(favouring(eight)))) {
            assertEquals(1, policy.choose(state, moves), "the model favours the eight of spades");
        }
    }

    @Test
    void thePolicyIgnoresTheActionsThatAreNotLegal() throws IOException {
        State state = leadState();
        List<Move> moves = Game.legalMoves(state);
        int ace = Vector.actionOf(card(SPADES, Card.Rank.ACE));

        try (OnnxPolicy policy = new OnnxPolicy(modelWith(favouring(ace)))) {
            int chosen = policy.choose(state, moves);

            assertEquals(card(SPADES, Card.Rank.FOUR), card(moves.get(chosen)),
                    "the ace of spades is not legal, so the lowest legal action wins");
        }
    }

    @Test
    void thePolicyCanTakeThePot() throws IOException {
        State state = takePotState();
        List<Move> moves = Game.legalMoves(state);

        try (OnnxPolicy policy = new OnnxPolicy(modelWith(favouring(Vector.TAKE_POT)))) {
            assertEquals(0, policy.choose(state, moves), "the only legal move is to take the pot");
        }
    }

    @Test
    void thePolicyObservesTheSeatThatIsAboutToMove() throws IOException {
        State state = leadState();
        List<Move> moves = Game.legalMoves(state);
        int fromTurn = favourite(Vector.of(state, state.turn(), moves));
        int fromOther = favourite(Vector.of(state, clubs(), moves));

        assertNotEquals(fromTurn, fromOther, "another seat sees another hand");

        try (OnnxPolicy policy = new OnnxPolicy(echoing())) {
            assertEquals(indexOf(moves, fromTurn), policy.choose(state, moves),
                    "the model read the vector of the seat to move");
        }
    }

    @Test
    void thePolicyIsReproducible() throws IOException {
        Path model = echoing();

        List<Integer> first = play(model, 42L);
        List<Integer> second = play(model, 42L);

        assertTrue(first.size() > 10, "the game was played");
        assertEquals(first, second, "the same weights and the same seed give the same game");
    }

    @Test
    void aMissingModelFailsLoudly() {
        Path missing = directory.resolve("missing.onnx");

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new OnnxPolicy(missing));

        assertTrue(failure.getMessage().contains("missing.onnx"), failure.getMessage());
    }

    @Test
    void aBrokenModelFailsLoudly() throws IOException {
        Path broken = directory.resolve("broken.onnx");
        Files.writeString(broken, "not a model");

        assertThrows(IllegalStateException.class, () -> new OnnxPolicy(broken));
    }

    @Test
    void aModelOfTheWrongShapeFailsLoudly() throws IOException {
        Path model = TinyModel.write(directory, 100, Vector.ACTIONS, gain(), bias());

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> new OnnxPolicy(model));

        assertTrue(failure.getMessage().contains("100"), failure.getMessage());
    }

    private Path modelWith(float[] bias) throws IOException {
        return TinyModel.write(directory, gain(), bias);
    }

    private Path echoing() throws IOException {
        float[] ones = new float[Vector.ACTIONS];
        Arrays.fill(ones, 1f);
        return TinyModel.write(directory, ones, bias());
    }

    private static float[] gain() {
        return new float[Vector.ACTIONS];
    }

    private static float[] bias() {
        return new float[Vector.ACTIONS];
    }

    /** A bias that favours one action and says nothing about the others. */
    private static float[] favouring(int action) {
        float[] bias = bias();
        bias[action] = 1f;
        return bias;
    }

    /** The move the model that reads the vector would choose. */
    private static int favourite(Vector vector) {
        float[] values = vector.values();
        int best = -1;
        float bestScore = Float.NEGATIVE_INFINITY;
        for (int action = 0; action < Vector.ACTIONS; action++) {
            if ((vector.mask() & (1L << action)) == 0) {
                continue;
            }
            if (values[action] > bestScore) {
                bestScore = values[action];
                best = action;
            }
        }
        return best;
    }

    private static int indexOf(List<Move> moves, int action) {
        for (int index = 0; index < moves.size(); index++) {
            Move move = moves.get(index);
            boolean chosen = action == Vector.TAKE_POT
                    ? move instanceof Move.TakePot
                    : move instanceof Move.Play play && Vector.actionOf(play.card()) == action;
            if (chosen) {
                return index;
            }
        }
        throw new IllegalArgumentException("the action " + action + " is not among the legal moves");
    }

    private static Card card(Move move) {
        return ((Move.Play) move).card();
    }

    /** A whole game, played by the policy, as the list of the moves it chose. */
    private static List<Integer> play(Path model, long seed) {
        List<Integer> chosen = new ArrayList<>();
        try (OnnxPolicy policy = new OnnxPolicy(model)) {
            State state = Game.start(new Setup(RankSet.SIX_TO_ACE,
                    List.of(SPADES, CLUBS, DIAMONDS, HEARTS), seed)).state();
            while (true) {
                List<Move> moves = Game.legalMoves(state);
                if (moves.isEmpty()) {
                    break;
                }
                int index = policy.choose(state, moves);
                chosen.add(index);
                state = Game.step(state, index).state();
            }
        }
        return chosen;
    }

    /** A table where the seat to move leads with two cards. */
    private static State leadState() {
        return new State(List.of(spades(), clubs(), diamonds(), hearts()),
                List.of(), List.of(), clubs(), spades(), true, 0, 42L);
    }

    /** A table where nothing beats the ace of spades, so the pot is taken. */
    private static State takePotState() {
        return new State(List.of(spades(), clubs(), diamonds(), hearts()),
                List.of(card(SPADES, Card.Rank.ACE)), List.of(), clubs(), spades(), false, 0, 42L);
    }

    private static Seat spades() {
        return new Seat(SPADES, List.of(card(SPADES, Card.Rank.FOUR), card(SPADES, Card.Rank.EIGHT)),
                List.of(), false);
    }

    private static Seat clubs() {
        return new Seat(CLUBS, List.of(card(CLUBS, Card.Rank.EIGHT)), List.of(), false);
    }

    private static Seat diamonds() {
        return new Seat(DIAMONDS, List.of(card(DIAMONDS, Card.Rank.KING)), List.of(), false);
    }

    private static Seat hearts() {
        return new Seat(HEARTS, List.of(card(HEARTS, Card.Rank.TEN)), List.of(), false);
    }

    private static Card card(Card.Suit suit, Card.Rank rank) {
        return new Card(suit, rank);
    }
}
