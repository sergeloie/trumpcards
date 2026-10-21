package ru.anseranser.engine;

/**
 * One of the choices available to the seat whose turn it is: playing a card or
 * taking the pot. An illegal move cannot be expressed.
 */
public sealed interface Move permits Move.Play, Move.TakePot {

    /** Playing a card: into an empty pot, or over the top card of the pot. */
    record Play(Card card) implements Move {}

    /** Taking the whole pot, the only move left when nothing can beat the top card. */
    record TakePot() implements Move {}
}
