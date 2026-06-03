package library.datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Generic doubly-linked list.
 * Provides O(1) add/remove at head and tail, O(n) search.
 *
 * @param <T> element type
 */
public class DoublyLinkedList<T> implements Iterable<T> {

    // ── Inner node class ───────────────────────────────────────────────────
    private static class Node<T> {
        T    data;
        Node<T> prev;
        Node<T> next;
        Node(T data) { this.data = data; }
    }

    // ── State ──────────────────────────────────────────────────────────────
    private Node<T> head;
    private Node<T> tail;
    private int size;

    // ── Add operations ─────────────────────────────────────────────────────
    /** Append to the end of the list — O(1). */
    public void addLast(T item) {
        Node<T> node = new Node<>(item);
        if (tail == null) {
            head = tail = node;
        } else {
            node.prev  = tail;
            tail.next  = node;
            tail       = node;
        }
        size++;
    }

    /** Prepend to the front of the list — O(1). */
    public void addFirst(T item) {
        Node<T> node = new Node<>(item);
        if (head == null) {
            head = tail = node;
        } else {
            node.next  = head;
            head.prev  = node;
            head       = node;
        }
        size++;
    }

    // ── Remove operations ──────────────────────────────────────────────────
    /** Remove first element matching the predicate — O(n). Returns true if removed. */
    public boolean removeIf(Predicate<T> condition) {
        Node<T> cur = head;
        while (cur != null) {
            if (condition.test(cur.data)) {
                unlink(cur);
                return true;
            }
            cur = cur.next;
        }
        return false;
    }

    /** Remove all elements matching the predicate — O(n). Returns count removed. */
    public int removeAllIf(Predicate<T> condition) {
        int count = 0;
        Node<T> cur = head;
        while (cur != null) {
            Node<T> next = cur.next;
            if (condition.test(cur.data)) { unlink(cur); count++; }
            cur = next;
        }
        return count;
    }

    /** Remove the head node — O(1). */
    public T removeFirst() {
        if (head == null) throw new NoSuchElementException("List is empty.");
        T data = head.data;
        unlink(head);
        return data;
    }

    private void unlink(Node<T> node) {
        if (node.prev != null) node.prev.next = node.next; else head = node.next;
        if (node.next != null) node.next.prev = node.prev; else tail = node.prev;
        node.prev = node.next = null;
        size--;
    }

    // ── Query operations ───────────────────────────────────────────────────
    /** Find first element matching predicate — O(n). */
    public T find(Predicate<T> condition) {
        for (T item : this) if (condition.test(item)) return item;
        return null;
    }

    public boolean isEmpty()  { return size == 0; }
    public int     getSize()  { return size; }
    public T       getHead()  { return head != null ? head.data : null; }
    public T       getTail()  { return tail != null ? tail.data : null; }

    /** Visualise the list as a chain of nodes — useful for debugging. */
    public String toChainString(java.util.function.Function<T, String> labelFn) {
        if (isEmpty()) return "NULL ↔ (empty) ↔ NULL";
        StringBuilder sb = new StringBuilder("HEAD ↔ ");
        Node<T> cur = head;
        while (cur != null) {
            sb.append("[").append(labelFn.apply(cur.data)).append("]");
            if (cur.next != null) sb.append(" ↔ ");
            cur = cur.next;
        }
        sb.append(" ↔ TAIL");
        return sb.toString();
    }

    // ── Iterator ───────────────────────────────────────────────────────────
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {
            Node<T> cur = head;
            @Override public boolean hasNext() { return cur != null; }
            @Override public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                T d = cur.data; cur = cur.next; return d;
            }
        };
    }
}
