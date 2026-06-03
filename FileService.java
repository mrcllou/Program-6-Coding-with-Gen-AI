package library.service;

import library.model.Book;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * FileService — handles all file I/O for the library.
 *
 * Files produced / consumed:
 *   data/books.csv          — main persistent catalog (CSV)
 *   data/books_backup_*.csv — timestamped backup created on every save
 *   data/library.log        — append-only operation log
 */
public class FileService {

    private static final String DATA_DIR      = "data";
    private static final String BOOKS_FILE    = DATA_DIR + "/books.csv";
    private static final String LOG_FILE      = DATA_DIR + "/library.log";
    private static final String CSV_HEADER    =
        "title,author,isbn,publisher,publishedDate,description,pageCount,rating,genre,status,price,pdfLink,buyLink";

    private static final DateTimeFormatter TS_FMT =
        DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    // ── Initialise ──────────────────────────────────────────────────────────
    public void init() throws IOException {
        Files.createDirectories(Paths.get(DATA_DIR));
        Path catalog = Paths.get(BOOKS_FILE);
        if (!Files.exists(catalog)) {
            Files.writeString(catalog, CSV_HEADER + System.lineSeparator());
            log("INFO", "Created new catalog file: " + BOOKS_FILE);
        }
    }

    // ── Save ────────────────────────────────────────────────────────────────
    /**
     * Write all books to CSV, then create a timestamped backup.
     */
    public void saveAll(List<Book> books) throws IOException {
        StringBuilder sb = new StringBuilder(CSV_HEADER).append(System.lineSeparator());
        for (Book b : books) sb.append(b.toCsv()).append(System.lineSeparator());

        Files.writeString(Paths.get(BOOKS_FILE), sb.toString());

        // timestamped backup
        String ts     = LocalDateTime.now().format(TS_FMT);
        String backup = DATA_DIR + "/books_backup_" + ts + ".csv";
        Files.copy(Paths.get(BOOKS_FILE), Paths.get(backup), StandardCopyOption.REPLACE_EXISTING);

        log("SAVE", "Saved " + books.size() + " books → " + BOOKS_FILE + "  (backup: " + backup + ")");
        cleanOldBackups(5);
    }

    // ── Load ────────────────────────────────────────────────────────────────
    /**
     * Read books from CSV.  Returns an empty list if the file doesn't exist yet.
     */
    public List<Book> loadAll() throws IOException {
        Path path = Paths.get(BOOKS_FILE);
        if (!Files.exists(path)) return new ArrayList<>();

        List<String> lines = Files.readAllLines(path);
        List<Book>   books = new ArrayList<>();

        for (int i = 1; i < lines.size(); i++) {   // skip header
            String line = lines.get(i).trim();
            if (line.isBlank()) continue;
            try {
                Book b = parseCsvLine(line);
                if (b != null) books.add(b);
            } catch (Exception e) {
                log("WARN", "Skipped malformed line " + (i + 1) + ": " + e.getMessage());
            }
        }

        log("LOAD", "Loaded " + books.size() + " books from " + BOOKS_FILE);
        return books;
    }

    // ── Export helpers ──────────────────────────────────────────────────────
    /** Export a filtered / sorted subset to a named file. */
    public void exportSubset(List<Book> books, String filename) throws IOException {
        StringBuilder sb = new StringBuilder(CSV_HEADER).append(System.lineSeparator());
        for (Book b : books) sb.append(b.toCsv()).append(System.lineSeparator());
        Files.writeString(Paths.get(DATA_DIR + "/" + filename), sb.toString());
        log("EXPORT", "Exported " + books.size() + " books → " + filename);
    }

    /** Append a single book's research result to a separate research file. */
    public void appendResearchResult(String query, String result) throws IOException {
        String filename = DATA_DIR + "/research_results.txt";
        String entry = "─".repeat(60) + System.lineSeparator()
            + "Query : " + query + System.lineSeparator()
            + "Date  : " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            + System.lineSeparator() + result + System.lineSeparator();
        Files.writeString(Paths.get(filename), entry,
            StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        log("RESEARCH", "Appended research result for: " + query);
    }

    // ── Logging ─────────────────────────────────────────────────────────────
    public void log(String level, String message) {
        String entry = String.format("[%s] [%-8s] %s%n",
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
            level, message);
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            Files.writeString(Paths.get(LOG_FILE), entry,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {}
        // Also mirror to stdout for transparency
        System.out.print("  [LOG] " + entry.trim() + System.lineSeparator());
    }

    // ── CSV Parser ──────────────────────────────────────────────────────────
    private Book parseCsvLine(String line) {
        List<String> fields = splitCsv(line);
        if (fields.size() < 10) return null;
        return new Book(
            fields.get(0),                         // title
            fields.get(1),                         // author
            fields.get(2),                         // isbn
            fields.get(3),                         // publisher
            fields.get(4),                         // publishedDate
            fields.get(5),                         // description
            parseInt(fields.get(6)),               // pageCount
            parseDouble(fields.get(7)),            // rating
            fields.get(8),                         // genre
            fields.get(9),                         // status
            fields.size() > 10 ? parseDouble(fields.get(10)) : 0.0,
            fields.size() > 11 ? fields.get(11)   : "",
            fields.size() > 12 ? fields.get(12)   : ""
        );
    }

    /** RFC 4180-compliant CSV splitter (handles quoted fields with commas). */
    private List<String> splitCsv(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder cur   = new StringBuilder();
        boolean inQuotes    = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"'); i++;
                } else { inQuotes = !inQuotes; }
            } else if (c == ',' && !inQuotes) {
                result.add(cur.toString()); cur.setLength(0);
            } else { cur.append(c); }
        }
        result.add(cur.toString());
        return result;
    }

    private int    parseInt   (String s) { try { return Integer.parseInt(s.trim());    } catch (Exception e) { return 0;   } }
    private double parseDouble(String s) { try { return Double.parseDouble(s.trim());  } catch (Exception e) { return 0.0; } }

    // ── Backup pruning ──────────────────────────────────────────────────────
    private void cleanOldBackups(int keep) {
        try {
            List<Path> backups = new ArrayList<>();
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(
                    Paths.get(DATA_DIR), "books_backup_*.csv")) {
                ds.forEach(backups::add);
            }
            backups.sort((a, b) -> b.compareTo(a));      // newest first
            for (int i = keep; i < backups.size(); i++) {
                Files.deleteIfExists(backups.get(i));
            }
        } catch (IOException ignored) {}
    }

    public String getBooksFile() { return BOOKS_FILE; }
    public String getLogFile()   { return LOG_FILE; }
    public String getDataDir()   { return DATA_DIR; }
}
