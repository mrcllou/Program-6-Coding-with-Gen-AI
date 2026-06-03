package library.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Book entity — encapsulates all metadata for a single library book.
 * Implements Serializable for file persistence.
 */
public class Book implements Serializable, Comparable<Book> {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ── Fields ─────────────────────────────────────────────────────────────
    private String title;
    private String author;
    private String isbn;
    private String publisher;
    private LocalDate publishedDate;
    private String description;
    private int    pageCount;
    private double rating;       // 0.0 – 5.0
    private String genre;
    private BookStatus status;
    private double price;
    private String pdfLink;
    private String buyLink;

    // ── Constructor ────────────────────────────────────────────────────────
    public Book(String title, String author, String isbn) {
        if (title == null || title.isBlank())  throw new IllegalArgumentException("Title cannot be empty.");
        if (author == null || author.isBlank()) throw new IllegalArgumentException("Author cannot be empty.");
        this.title  = title.trim();
        this.author = author.trim();
        this.isbn   = (isbn != null) ? isbn.trim() : "";
        this.status = BookStatus.AVAILABLE;
    }

    // Full constructor used by CSV/JSON deserialization
    public Book(String title, String author, String isbn, String publisher,
                String publishedDate, String description, int pageCount,
                double rating, String genre, String status,
                double price, String pdfLink, String buyLink) {
        this(title, author, isbn);
        this.publisher     = publisher;
        this.publishedDate = parseDate(publishedDate);
        this.description   = description;
        this.pageCount     = Math.max(0, pageCount);
        this.rating        = Math.min(5.0, Math.max(0.0, rating));
        this.genre         = genre;
        this.status        = BookStatus.fromString(status);
        this.price         = Math.max(0.0, price);
        this.pdfLink       = pdfLink;
        this.buyLink       = buyLink;
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.substring(0, 10), FMT); }
        catch (Exception e) { return null; }
    }

    public String starsDisplay() {
        int full  = (int) rating;
        boolean half = (rating - full) >= 0.5;
        return "★".repeat(full) + (half ? "½" : "") + "☆".repeat(5 - full - (half ? 1 : 0))
               + String.format(" (%.1f)", rating);
    }

    // ── Comparable (sort by title by default) ─────────────────────────────
    @Override
    public int compareTo(Book other) {
        return this.title.compareToIgnoreCase(other.title);
    }

    @Override
    public String toString() {
        return String.format("%-40s | %-22s | %-15s | %-10s | %s",
            truncate(title, 40), truncate(author, 22),
            isbn.isEmpty() ? "—" : isbn,
            status.getLabel(),
            starsDisplay());
    }

    public String toDetailString() {
        return """
               ╔══════════════════════════════════════════════════════════╗
                 BOOK DETAILS
               ╠══════════════════════════════════════════════════════════╣
                 Title       : %s
                 Author      : %s
                 ISBN        : %s
                 Publisher   : %s
                 Published   : %s
                 Genre       : %s
                 Pages       : %d
                 Rating      : %s
                 Price       : $%.2f
                 Status      : %s
                 PDF Link    : %s
                 Buy Link    : %s
               ╠══════════════════════════════════════════════════════════╣
                 Description :
                 %s
               ╚══════════════════════════════════════════════════════════╝
               """.formatted(
                title, author,
                isbn.isEmpty() ? "—" : isbn,
                publisher    != null ? publisher    : "—",
                publishedDate != null ? publishedDate.format(FMT) : "—",
                genre        != null ? genre        : "—",
                pageCount,
                starsDisplay(),
                price,
                status.getLabel(),
                (pdfLink  != null && !pdfLink.isBlank())  ? pdfLink  : "—",
                (buyLink  != null && !buyLink.isBlank())  ? buyLink  : "—",
                wrap(description != null ? description : "—", 58));
    }

    /** Convert to CSV line */
    public String toCsv() {
        return String.join(",",
            csvEsc(title), csvEsc(author), csvEsc(isbn),
            csvEsc(publisher), publishedDate != null ? publishedDate.format(FMT) : "",
            csvEsc(description), String.valueOf(pageCount),
            String.valueOf(rating), csvEsc(genre), status.name(),
            String.valueOf(price), csvEsc(pdfLink), csvEsc(buyLink));
    }

    private String csvEsc(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n"))
            return "\"" + s.replace("\"", "\"\"") + "\"";
        return s;
    }

    private String truncate(String s, int len) {
        if (s == null) return "";
        return s.length() <= len ? s : s.substring(0, len - 1) + "…";
    }

    private String wrap(String s, int width) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        String[] words = s.split(" ");
        int lineLen = 0;
        for (String w : words) {
            if (lineLen + w.length() > width) { sb.append("\n   "); lineLen = 0; }
            sb.append(w).append(" ");
            lineLen += w.length() + 1;
        }
        return sb.toString().trim();
    }

    // ── Getters & Setters ──────────────────────────────────────────────────
    public String getTitle()        { return title; }
    public String getAuthor()       { return author; }
    public String getIsbn()         { return isbn; }
    public String getPublisher()    { return publisher; }
    public LocalDate getPublishedDate() { return publishedDate; }
    public String getPublishedDateStr() { return publishedDate != null ? publishedDate.format(FMT) : ""; }
    public String getDescription()  { return description; }
    public int    getPageCount()    { return pageCount; }
    public double getRating()       { return rating; }
    public String getGenre()        { return genre; }
    public BookStatus getStatus()   { return status; }
    public double getPrice()        { return price; }
    public String getPdfLink()      { return pdfLink; }
    public String getBuyLink()      { return buyLink; }

    public void setTitle(String t)       { this.title = t; }
    public void setAuthor(String a)      { this.author = a; }
    public void setIsbn(String i)        { this.isbn = i; }
    public void setPublisher(String p)   { this.publisher = p; }
    public void setPublishedDate(String d){ this.publishedDate = parseDate(d); }
    public void setDescription(String d) { this.description = d; }
    public void setPageCount(int p)      { this.pageCount = Math.max(0, p); }
    public void setRating(double r)      { this.rating = Math.min(5.0, Math.max(0.0, r)); }
    public void setGenre(String g)       { this.genre = g; }
    public void setStatus(BookStatus s)  { this.status = s; }
    public void setPrice(double p)       { this.price = Math.max(0.0, p); }
    public void setPdfLink(String l)     { this.pdfLink = l; }
    public void setBuyLink(String l)     { this.buyLink = l; }
}
