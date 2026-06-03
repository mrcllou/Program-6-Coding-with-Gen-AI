package library.service;

import library.datastructures.DoublyLinkedList;
import library.model.Book;
import library.model.BookStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Library — the central service class.
 *
 * Uses TWO data structures simultaneously:
 *   • DoublyLinkedList<Book>  — insertion-order catalog (recent additions, checked-out queue)
 *   • ArrayList<Book>         — random-access catalog (index lookup, sorting, bulk search)
 *
 * Both are kept in sync on every mutation.
 */
public class Library {

    private final String name;

    /** Linked list: tracks books in insertion order; O(1) add/remove at ends */
    private final DoublyLinkedList<Book> linkedCatalog = new DoublyLinkedList<>();

    /** ArrayList: dynamic array for O(1) index access and bulk operations */
    private final ArrayList<Book>        arrayCatalog  = new ArrayList<>();

    public Library(String name) {
        this.name = name;
    }

    // ── Add ────────────────────────────────────────────────────────────────
    /**
     * Add a book to both data structures.
     * Duplicate ISBN check (unless ISBN is blank).
     */
    public boolean addBook(Book book) {
        if (book == null) return false;
        if (!book.getIsbn().isBlank() && findByIsbn(book.getIsbn()) != null) {
            System.out.println("  ⚠  A book with ISBN " + book.getIsbn() + " already exists.");
            return false;
        }
        linkedCatalog.addLast(book);
        arrayCatalog.add(book);
        return true;
    }

    // ── Remove ─────────────────────────────────────────────────────────────
    /** Remove by ISBN — returns the removed book, or null if not found. */
    public Book removeByIsbn(String isbn) {
        Book found = findByIsbn(isbn);
        if (found == null) return null;
        linkedCatalog.removeIf(b -> b.getIsbn().equalsIgnoreCase(isbn));
        arrayCatalog.removeIf(b -> b.getIsbn().equalsIgnoreCase(isbn));
        return found;
    }

    /** Remove by exact title match (case-insensitive). */
    public Book removeByTitle(String title) {
        Book found = findByTitle(title);
        if (found == null) return null;
        linkedCatalog.removeIf(b -> b.getTitle().equalsIgnoreCase(title));
        arrayCatalog.removeIf(b -> b.getTitle().equalsIgnoreCase(title));
        return found;
    }

    // ── Find / Search ──────────────────────────────────────────────────────
    public Book findByIsbn(String isbn) {
        return linkedCatalog.find(b -> b.getIsbn().equalsIgnoreCase(isbn));
    }

    public Book findByTitle(String title) {
        return linkedCatalog.find(b -> b.getTitle().equalsIgnoreCase(title));
    }

    /** Full-text search across title, author, isbn, genre, description. */
    public List<Book> search(String query) {
        if (query == null || query.isBlank()) return new ArrayList<>(arrayCatalog);
        String q = query.toLowerCase();
        return arrayCatalog.stream()
            .filter(b -> contains(b.getTitle(),       q)
                      || contains(b.getAuthor(),      q)
                      || contains(b.getIsbn(),        q)
                      || contains(b.getGenre(),       q)
                      || contains(b.getDescription(), q))
            .collect(Collectors.toList());
    }

    private boolean contains(String field, String q) {
        return field != null && field.toLowerCase().contains(q);
    }

    /** Filter by predicate — uses ArrayList for random access. */
    public List<Book> filter(Predicate<Book> predicate) {
        return arrayCatalog.stream().filter(predicate).collect(Collectors.toList());
    }

    // ── Sorted views ───────────────────────────────────────────────────────
    public List<Book> getAllSortedByTitle() {
        List<Book> copy = new ArrayList<>(arrayCatalog);
        Collections.sort(copy);
        return copy;
    }

    public List<Book> getAllSortedByAuthor() {
        List<Book> copy = new ArrayList<>(arrayCatalog);
        copy.sort((a, b) -> a.getAuthor().compareToIgnoreCase(b.getAuthor()));
        return copy;
    }

    public List<Book> getAllSortedByRating() {
        List<Book> copy = new ArrayList<>(arrayCatalog);
        copy.sort((a, b) -> Double.compare(b.getRating(), a.getRating()));
        return copy;
    }

    // ── Status management ──────────────────────────────────────────────────
    public boolean checkOut(String isbn) {
        Book b = findByIsbn(isbn);
        if (b == null) return false;
        if (b.getStatus() != BookStatus.AVAILABLE) {
            System.out.println("  ⚠  Book is not available (status: " + b.getStatus().getLabel() + ").");
            return false;
        }
        b.setStatus(BookStatus.CHECKED_OUT);
        return true;
    }

    public boolean returnBook(String isbn) {
        Book b = findByIsbn(isbn);
        if (b == null) return false;
        b.setStatus(BookStatus.AVAILABLE);
        return true;
    }

    // ── Stats ──────────────────────────────────────────────────────────────
    public int  totalBooks()     { return arrayCatalog.size(); }
    public long availableCount() { return arrayCatalog.stream().filter(b -> b.getStatus() == BookStatus.AVAILABLE).count(); }
    public long checkedOutCount(){ return arrayCatalog.stream().filter(b -> b.getStatus() == BookStatus.CHECKED_OUT).count(); }

    public List<String> allGenres() {
        return arrayCatalog.stream()
            .map(Book::getGenre)
            .filter(g -> g != null && !g.isBlank())
            .distinct().sorted().collect(Collectors.toList());
    }

    // ── Accessors ──────────────────────────────────────────────────────────
    public String              getName()        { return name; }
    public List<Book>          getAllBooks()     { return Collections.unmodifiableList(arrayCatalog); }
    public DoublyLinkedList<Book> getLinkedCatalog() { return linkedCatalog; }
    public ArrayList<Book>     getArrayCatalog()     { return arrayCatalog; }
}
