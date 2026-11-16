import java.util.ArrayList;
import java.util.List;

/**
 * Custom generic LIFO Stack implemented from scratch using a singly linked list.
 * Assigned Team Members: Syed Inshal Ali Kazmi (F25CSC039) & Mujtuba Zia (F25CSC041)
 *
 * @param <T> Type of elements stored in the stack.
 */
public class CustomStack<T> {

    private static class Node<T> {
        final T data;
        Node<T> next;

        Node(T data, Node<T> next) {
            this.data = data;
            this.next = next;
        }
    }

    private Node<T> top;
    private int size;
    private int peakSize;

    public CustomStack() {
        this.top = null;
        this.size = 0;
        this.peakSize = 0;
    }

    /**
     * Pushes an element onto the top of the stack in O(1) time.
     */
    public void push(T item) {
        top = new Node<T>(item, top);
        size++;
        if (size > peakSize) {
            peakSize = size;
        }
    }

    /**
     * Removes and returns the top element of the stack in O(1) time.
     */
    public T pop() {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot pop from an empty CustomStack.");
        }
        T data = top.data;
        top = top.next;
        size--;
        return data;
    }

    /**
     * Returns the top element without removing it in O(1) time.
     */
    public T peek() {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot peek an empty CustomStack.");
        }
        return top.data;
    }

    public boolean isEmpty() {
        return top == null;
    }

    public int size() {
        return size;
    }

    public int getPeakSize() {
        return peakSize;
    }

    public void clear() {
        top = null;
        size = 0;
        peakSize = 0;
    }

    /**
     * Returns up to maxItems from the top of the stack (Top -> Down)
     * for the Live Data Structure Inspector UI.
     */
    public List<T> getSnapshot(int maxItems) {
        List<T> snapshot = new ArrayList<T>();
        Node<T> current = top;
        int count = 0;
        while (current != null && count < maxItems) {
            snapshot.add(current.data);
            current = current.next;
            count++;
        }
        return snapshot;
    }
}
