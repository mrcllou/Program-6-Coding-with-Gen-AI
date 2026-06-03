package library.ui;

import library.model.Book;
import library.model.BookStatus;
import library.service.BookResearcher;
import library.service.FileService;
import library.service.Library;

import java.io.IOException;
import java.util.List;
import java.util.Scanner;

/**
 * ConsoleUI — the interactive terminal interface.
 *
 * All user interaction flows through this class.
 * Delegates business logic to Library, file I/O to FileService,
 * and online research to BookResearcher.
 */
public class ConsoleUI {

    private static final String DIVIDER  = "═".repeat(72);
    private static final String THIN     = "─".repeat(72);

    private final Library       library;
    private final FileService   fileService;
    private final BookResearcher researcher;
    private final Scanner        scanner;

    public ConsoleUI(Library library, FileService fileService, BookResearcher researcher) {
        this.library    = library;
        this.fileService = fileService;
        this.researcher  = researcher;
        this.scanner     = new Scanner(System.in);
    }

    // ── Main loop ──────────────────────────────────────────────────────────
    public void run() {
        printBanner();
        boolean running = true;
        while (running) {
            printMainMenu();
            String choice = prompt("Enter choice").trim();
            running = handleMainMenu(choice);
        }
        System.out.println("\n  👋  Goodbye! Library data has been saved.");
    }

    private boolean handleMainMenu(String choice) {
        switch (choice) {
            case "1"  -> viewCatalog();
            case "2"  -> addBookManual();
            case "3"  -> removeBook();
            case "4"  -> searchBooks();
            case "5"  -> viewBookDetail();
            case "6"  -> checkOutReturn();
            case "7"  -> researchOnline();
            case "8"  -> showDataStructures();
            case "9"  -> exportMenu();
            case "0"  -> { saveAndExit(); return false; }
            default   -> System.out.println("  ⚠  Unknown option — please try again.");
        }
        return true;
    }

    // ── Catalog view ──────────────────────────────────────────────────────
    private void viewCatalog() {
        header("LIBRARY CATALOG — " + library.getName());
        String sort = prompt("Sort by: [T]itle  [A]uthor  [R]ating  (Enter = Title)").toUpperCase();
        List<Book> books = switch (sort) {
            case "A" -> library.getAllSortedByAuthor();
            case "R" -> library.getAllSortedByRating();
            default  -> library.getAllSortedByTitle();
        };
        printCatalogTable(books);
        printStats();
    }

    private void printCatalogTable(List<Book> books) {
        if (books.isEmpty()) { System.out.println("\n  (No books found)\n"); return; }
        System.out.printf("%n  %-3s  %-38s  %-20s  %-13s  %-12s  %s%n",
            "#", "TITLE", "AUTHOR", "ISBN", "STATUS", "RATING");
        System.out.println("  " + THIN);
        int i = 1;
        for (Book b : books) {
            System.out.printf("  %-3d  %-38s  %-20s  %-13s  %-12s  %s%n",
                i++,
                trunc(b.getTitle(),  38),
                trunc(b.getAuthor(), 20),
                b.getIsbn().isEmpty() ? "—" : b.getIsbn(),
                b.getStatus().getLabel(),
                b.starsDisplay());
        }
        System.out.println();
    }

    // ── Add book manually ──────────────────────────────────────────────────
    private void addBookManual() {
        header("ADD A NEW BOOK");
        String title  = requireInput("Title");
        String author = requireInput("Author");
        String isbn   = prompt("ISBN (optional)");
        String pub    = prompt("Publisher");
        String date   = prompt("Published Date (YYYY-MM-DD)");
        String genre  = prompt("Genre");
        int    pages  = parseInt(prompt("Page Count"));
        double rating = parseDouble(prompt("Rating (0.0–5.0)"));
        double price  = parseDouble(prompt("Price (USD)"));
        String pdf    = prompt("PDF / Preview Link (optional)");
        String buy    = prompt("Buy Link (optional)");

        System.out.println("  Description (press Enter twice to finish):");
        String desc = readMultiLine();

        Book b = new Book(title, author, isbn);
        b.setPublisher(pub);
        b.setPublishedDate(date);
        b.setDescription(desc);
        b.setGenre(genre);
        b.setPageCount(pages);
        b.setRating(rating);
        b.setPrice(price);
        b.setPdfLink(pdf);
        b.setBuyLink(buy);

        if (library.addBook(b)) {
            saveQuiet();
            System.out.println("\n  ✅  \"" + title + "\" added to the library!");
            System.out.println(b.toDetailString());
        }
    }

    // ── Remove book ────────────────────────────────────────────────────────
    private void removeBook() {
        header("REMOVE A BOOK");
        String method = prompt("Remove by: [I]SBN  or  [T]itle").toUpperCase();
        Book removed = null;
        if ("I".equals(method)) {
            String isbn = requireInput("ISBN");
            removed = library.removeByIsbn(isbn);
        } else {
            String title = requireInput("Title");
            removed = library.removeByTitle(title);
        }
        if (removed != null) {
            saveQuiet();
            System.out.println("\n  🗑  Removed: \"" + removed.getTitle() + "\" by " + removed.getAuthor());
        } else {
            System.out.println("\n  ⚠  Book not found.");
        }
    }

    // ── Search ────────────────────────────────────────────────────────────
    private void searchBooks() {
        header("SEARCH CATALOG");
        String query = requireInput("Search (title / author / genre / ISBN)");
        List<Book> results = library.search(query);
        System.out.println("\n  Found " + results.size() + " result(s) for \"" + query + "\":");
        printCatalogTable(results);
    }

    // ── Book detail ────────────────────────────────────────────────────────
    private void viewBookDetail() {
        header("BOOK DETAIL");
        String isbn = prompt("Enter ISBN (or leave blank to search by title)");
        Book b;
        if (isbn.isBlank()) {
            String title = requireInput("Title");
            b = library.findByTitle(title);
        } else {
            b = library.findByIsbn(isbn);
        }
        if (b == null) System.out.println("\n  ⚠  Book not found.");
        else            System.out.println(b.toDetailString());
    }

    // ── Check out / return ─────────────────────────────────────────────────
    private void checkOutReturn() {
        header("CHECK OUT / RETURN");
        String action = prompt("[C]heck out  or  [R]eturn").toUpperCase();
        String isbn   = requireInput("ISBN");
        if ("C".equals(action)) {
            if (library.checkOut(isbn)) {
                saveQuiet();
                System.out.println("\n  ✅  Checked out successfully.");
            }
        } else {
            if (library.returnBook(isbn)) {
                saveQuiet();
                System.out.println("\n  ✅  Returned and marked as Available.");
            } else {
                System.out.println("\n  ⚠  ISBN not found.");
            }
        }
    }

    // ── Online research ────────────────────────────────────────────────────
    private void researchOnline() {
        header("RESEARCH BOOKS ONLINE");
        System.out.println("  Searches Google Books + Open Library.");
        System.out.println("  Results include price, PDF/preview links, and ratings.\n");
        String query = requireInput("Search query (title, author, topic…)");

        try {
            List<Book> results = researcher.research(query);
            if (results.isEmpty()) {
                System.out.println("\n  ⚠  No results found. Try a different query.");
                return;
            }

            System.out.println("\n  Found " + results.size() + " result(s):\n");
            for (int i = 0; i < results.size(); i++) {
                Book b = results.get(i);
                System.out.printf("  [%d] %-40s  by %-22s  $%5.2f  %s%n",
                    i + 1, trunc(b.getTitle(), 40), trunc(b.getAuthor(), 22),
                    b.getPrice(), b.starsDisplay());
                if (b.getPdfLink() != null && !b.getPdfLink().isBlank())
                    System.out.println("      📄 PDF/Preview: " + b.getPdfLink());
                if (b.getBuyLink() != null && !b.getBuyLink().isBlank())
                    System.out.println("      🛒 Buy: " + b.getBuyLink());
            }

            String choice = prompt("\nAdd a book to library? Enter number (or 0 to skip)");
            int idx;
            try { idx = Integer.parseInt(choice.trim()) - 1; }
            catch (Exception e) { return; }
            if (idx >= 0 && idx < results.size()) {
                Book picked = results.get(idx);
                if (library.addBook(picked)) {
                    saveQuiet();
                    System.out.println("\n  ✅  Added \"" + picked.getTitle() + "\" to the library!");
                    System.out.println(picked.toDetailString());
                }
            }
        } catch (Exception e) {
            System.out.println("\n  ⚠  Research failed: " + e.getMessage());
            System.out.println("  Make sure you have an internet connection.");
        }
    }

    // ── Data structure view ────────────────────────────────────────────────
    private void showDataStructures() {
        header("DATA STRUCTURES VISUALISATION");
        System.out.println("  DoublyLinkedList<Book>  (size=" + library.getLinkedCatalog().getSize() + ")");
        System.out.println("  " + THIN);
        System.out.println("  " + library.getLinkedCatalog().toChainString(b -> trunc(b.getTitle(), 18)));
        System.out.println();
        System.out.println("  ArrayList<Book>  (size=" + library.getArrayCatalog().size() + ")");
        System.out.println("  " + THIN);
        var arr = library.getArrayCatalog();
        for (int i = 0; i < arr.size(); i++)
            System.out.printf("  [%3d] %s%n", i, arr.get(i).getTitle());
        System.out.println();
    }

    // ── Export menu ────────────────────────────────────────────────────────
    private void exportMenu() {
        header("EXPORT");
        System.out.println("  [1] Export all books");
        System.out.println("  [2] Export available books only");
        System.out.println("  [3] Export by genre");
        String choice = prompt("Choice");
        try {
            switch (choice) {
                case "1" -> {
                    fileService.exportSubset(library.getAllBooks(), "export_all.csv");
                    System.out.println("\n  ✅  Exported to data/export_all.csv");
                }
                case "2" -> {
                    var avail = library.filter(b -> b.getStatus() == BookStatus.AVAILABLE);
                    fileService.exportSubset(avail, "export_available.csv");
                    System.out.println("\n  ✅  Exported " + avail.size() + " books to data/export_available.csv");
                }
                case "3" -> {
                    System.out.println("  Genres: " + String.join(", ", library.allGenres()));
                    String genre = requireInput("Genre");
                    var filtered = library.filter(b -> genre.equalsIgnoreCase(b.getGenre()));
                    fileService.exportSubset(filtered, "export_" + genre.replaceAll("\\s+","_") + ".csv");
                    System.out.println("\n  ✅  Exported " + filtered.size() + " books.");
                }
            }
        } catch (IOException e) {
            System.out.println("  ⚠  Export failed: " + e.getMessage());
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────
    private void printMainMenu() {
        System.out.println("\n" + DIVIDER);
        System.out.printf("  📚  %s   (%d books)%n", library.getName(), library.totalBooks());
        System.out.println(THIN);
        System.out.println("  [1] View Catalog        [6] Check Out / Return");
        System.out.println("  [2] Add Book            [7] 🔍 Research Online");
        System.out.println("  [3] Remove Book         [8] Data Structures View");
        System.out.println("  [4] Search Catalog      [9] Export");
        System.out.println("  [5] Book Detail         [0] Save & Exit");
        System.out.println(DIVIDER);
    }

    private void printStats() {
        System.out.printf("  📊 Total: %d  |  Available: %d  |  Checked Out: %d  |  Genres: %d%n%n",
            library.totalBooks(), library.availableCount(),
            library.checkedOutCount(), library.allGenres().size());
    }

    private void printBanner() {
        System.out.println("\n" + DIVIDER);
        System.out.println("    ██╗     ██╗██████╗ ██████╗  █████╗ ██████╗ ██╗   ██╗");
        System.out.println("    ██║     ██║██╔══██╗██╔══██╗██╔══██╗██╔══██╗╚██╗ ██╔╝");
        System.out.println("    ██║     ██║██████╔╝██████╔╝███████║██████╔╝ ╚████╔╝ ");
        System.out.println("    ██║     ██║██╔══██╗██╔══██╗██╔══██║██╔══██╗  ╚██╔╝  ");
        System.out.println("    ███████╗██║██████╔╝██║  ██║██║  ██║██║  ██║   ██║   ");
        System.out.println("    ╚══════╝╚═╝╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝   ");
        System.out.println("          Library Management System  v1.0");
        System.out.println("          OOP · DoublyLinkedList · ArrayList · File I/O");
        System.out.println(DIVIDER + "\n");
    }

    private void header(String title) {
        System.out.println("\n" + DIVIDER);
        System.out.println("  " + title);
        System.out.println(DIVIDER);
    }

    private String prompt(String msg) {
        System.out.print("  » " + msg + ": ");
        return scanner.nextLine();
    }

    private String requireInput(String msg) {
        String s;
        do { s = prompt(msg).trim(); if (s.isBlank()) System.out.println("  ⚠  Cannot be empty."); }
        while (s.isBlank());
        return s;
    }

    private String readMultiLine() {
        StringBuilder sb = new StringBuilder();
        String line;
        String prev = null;
        while (true) {
            line = scanner.nextLine();
            if (line.isEmpty() && (prev == null || prev.isEmpty())) break;
            sb.append(line).append(" ");
            prev = line;
        }
        return sb.toString().trim();
    }

    private int    parseInt(String s)    { try { return Integer.parseInt(s.trim());    } catch (Exception e) { return 0;   } }
    private double parseDouble(String s) { try { return Double.parseDouble(s.trim());  } catch (Exception e) { return 0.0; } }
    private String trunc(String s, int n){ if (s==null) return ""; return s.length()<=n ? s : s.substring(0,n-1)+"…"; }

    private void saveQuiet() {
        try { fileService.saveAll(library.getAllBooks()); }
        catch (IOException e) { System.out.println("  ⚠  Auto-save failed: " + e.getMessage()); }
    }

    private void saveAndExit() {
        try {
            fileService.saveAll(library.getAllBooks());
            System.out.println("\n  ✅  Saved " + library.totalBooks() + " books to " + fileService.getBooksFile());
        } catch (IOException e) {
            System.out.println("  ⚠  Save error: " + e.getMessage());
        }
    }
}
