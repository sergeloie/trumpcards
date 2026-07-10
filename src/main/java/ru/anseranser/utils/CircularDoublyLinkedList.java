package ru.anseranser.utils;

public class CircularDoublyLinkedList<T> {
    private static class Node<T> {
        T value;
        Node<T> next;
        Node<T> prev;

        Node(T value) {
            this.value = value;
        }
    }

    private Node<T> head;
    private int size;

    public void addLast(T value) {
        Node<T> node = new Node<>(value);

        if (head == null) {
            head = node;
            head.next = head;
            head.prev = head;
        } else {
            Node<T> tail = head.prev;

            node.next = head;
            node.prev = tail;

            tail.next = node;
            head.prev = node;
        }

        size++;
    }

    public void addFirst(T value) {
        addLast(value);
        head = head.prev;
    }

    public T removeFirst() {
        if (head == null) {
            return null;
        }

        T value = head.value;

        if (size == 1) {
            head = null;
        } else {
            Node<T> tail = head.prev;
            Node<T> newHead = head.next;

            tail.next = newHead;
            newHead.prev = tail;
            head = newHead;
        }

        size--;
        return value;
    }

    public T removeLast() {
        if (head == null) {
            return null;
        }

        if (size == 1) {
            return removeFirst();
        }

        Node<T> tail = head.prev;
        T value = tail.value;
        Node<T> newTail = tail.prev;

        newTail.next = head;
        head.prev = newTail;

        size--;
        return value;
    }

    public void printForward() {
        if (head == null) {
            System.out.println("[]");
            return;
        }

        Node<T> current = head;
        System.out.print("[");
        for (int i = 0; i < size; i++) {
            System.out.print(current.value);
            if (i < size - 1) System.out.print(", ");
            current = current.next;
        }
        System.out.println("]");
    }

    public int size() {
        return size;
    }

    public boolean delete(T value) {
        if (head == null) {
            return false;
        }

        Node<T> current = head;

        for (int i = 0; i < size; i++) {
            if ((current.value == null && value == null) ||
                    (current.value != null && current.value.equals(value))) {

                if (size == 1) {
                    head = null;
                } else {
                    Node<T> prev = current.prev;
                    Node<T> next = current.next;

                    prev.next = next;
                    next.prev = prev;

                    if (current == head) {
                        head = next;
                    }
                }

                size--;
                return true;
            }

            current = current.next;
        }

        return false;
    }
}