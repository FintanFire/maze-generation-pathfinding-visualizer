import java.util.ArrayList;
import java.util.List;

/**
 * Custom generic FIFO Queue implemented from scratch using linked nodes with head and tail pointers.
 * Assigned Team Member: Mujtuba Zia (F25CSC041)
 *
 * @param <T> Type of elements stored in the queue.
 */
public class CustomQueue<T> {

    private static class Node<T> {
        final T data;
        Node<T> next;

        Node(T data) {
            this.data = data;
            this.next = null;
        }
    }

    private Node<T> head;
    private Node<T> tail;
    private int size;
    private int peakSize;

    public CustomQueue() {
        this.head = null;
        this.tail = null;
        this.size = 0;
        this.peakSize = 0;
    }

    /**
     * Adds an element to the rear of the queue in O(1) time.
     */
    public void enqueue(T item) {
        Node<T> newNode = new Node<T>(item);
        if (tail == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
        if (size > peakSize) {
            peakSize = size;
        }
    }

    /**
     * Removes and returns the front element of the queue in O(1) time.
     */
    public T dequeue() {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot dequeue from an empty CustomQueue.");
        }
        T data = head.data;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return data;
    }

    /**
     * Returns the front element without removing it in O(1) time.
     */
    public T peek() {
        if (isEmpty()) {
            throw new IllegalStateException("Cannot peek an empty CustomQueue.");
        }
        return head.data;
    }

    public boolean isEmpty() {
        return head == null;
    }

    public int size() {
        return size;
    }

    public int getPeakSize() {
        return peakSize;
    }

    public void clear() {
        head = null;
        tail = null;
        size = 0;
        peakSize = 0;
    }

    /**
     * Returns up to maxItems from the front of the queue (Front -> Rear)
     * for the Live Data Structure Inspector UI.
     */
    public List<T> getSnapshot(int maxItems) {
        List<T> snapshot = new ArrayList<T>();
        Node<T> current = head;
        int count = 0;
        while (current != null && count < maxItems) {
            snapshot.add(current.data);
            current = current.next;
            count++;
        }
        return snapshot;
    }
}
