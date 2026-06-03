package library;

import library.service.BookResearcher;
import library.service.FileService;
import library.service.Library;
import library.ui.ConsoleUI;
import library.util.DataSeeder;

import java.util.List;
import library.model.Book;

/**
 * Main — application entry point.
 *
 * Boot sequence:
 *   1. Initialise file system (create data/ dir + books.csv if absent)
 *   2. Load persisted books from CSV
 *   3. If catalog is empty, seed with sample data
 *   4. Wire up services and hand off to ConsoleUI
 */
public class Main {

    public static void main(String[] args) {
        FileService   fileService = new FileService();
        Library       library     = new Library("Lynnwood Public Library");
        BookResearcher researcher = new BookResearcher(fileService);

        // ── File system init ──────────────────────────────────────────────
        try {
            fileService.init();
        } catch (Exception e) {
            System.err.println("Could not initialise data directory: " + e.getMessage());
        }

        // ── Load persisted data ───────────────────────────────────────────
        try {
            List<Book> saved = fileService.loadAll();
            saved.forEach(library::addBook);
            System.out.println("  ✅  Loaded " + saved.size() + " books from "
                + fileService.getBooksFile());
        } catch (Exception e) {
            System.out.println("  ⚠  Could not load catalog: " + e.getMessage());
        }

        // ── Seed on first run ─────────────────────────────────────────────
        if (library.totalBooks() == 0) {
            System.out.println("  📚  First run — seeding sample catalog…");
            DataSeeder.seed(library);
            try { fileService.saveAll(library.getAllBooks()); }
            catch (Exception e) { System.out.println("  ⚠  Seed save failed: " + e.getMessage()); }
        }

        // ── Launch UI ─────────────────────────────────────────────────────
        ConsoleUI ui = new ConsoleUI(library, fileService, researcher);
        ui.run();
    }
}

/*
# PROGRAM OUTPUT

════════════════════════════════════════════════════════════════════════
    ██╗     ██╗██████╗ ██████╗  █████╗ ██████╗ ██╗   ██╗
    ██║     ██║██╔══██╗██╔══██╗██╔══██╗██╔══██╗╚██╗ ██╔╝
    ██║     ██║██████╔╝██████╔╝███████║██████╔╝ ╚████╔╝ 
    ██║     ██║██╔══██╗██╔══██╗██╔══██║██╔══██╗  ╚██╔╝  
    ███████╗██║██████╔╝██║  ██║██║  ██║██║  ██║   ██║   
    ╚══════╝╚═╝╚═════╝ ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝   ╚═╝   
          Library Management System  v1.0
          OOP · DoublyLinkedList · ArrayList · File I/O
════════════════════════════════════════════════════════════════════════


════════════════════════════════════════════════════════════════════════
  📚  Lynnwood Public Library   (8 books)
────────────────────────────────────────────────────────────────────────
  [1] View Catalog        [6] Check Out / Return
  [2] Add Book            [7] 🔍 Research Online
  [3] Remove Book         [8] Data Structures View
  [4] Search Catalog      [9] Export
  [5] Book Detail         [0] Save & Exit
════════════════════════════════════════════════════════════════════════
  » Enter choice: 7

════════════════════════════════════════════════════════════════════════
  RESEARCH BOOKS ONLINE
════════════════════════════════════════════════════════════════════════
  Searches Google Books + Open Library.
  Results include price, PDF/preview links, and ratings.

  » Search query (title, author, topic…): Romance 

  🔍  Searching Google Books for: Romance …
  ⚠  Google Books unavailable (HTTP 429 from https://www.googleapis.com/books/v1/volumes?maxResults=5&q=Romance). Trying Open Library…
  [LOG] [2026-06-03 11:48:40] [RESEARCH] Appended research result for: Romance

  Found 5 result(s):

  [1] Romancing Mister Bridgerton               by ],                      $ 0.00  ☆☆☆☆☆ (0.0)
  [2] Flatland                                  by ],                      $ 0.00  ☆☆☆☆☆ (0.0)
  [3] Phantastes                                by ],                      $ 0.00  ☆☆☆☆☆ (0.0)
  [4] The Blithedale Romance                    by ],                      $ 0.00  ☆☆☆☆☆ (0.0)
  [5] An Unlikely Romance                       by Unknown                 $ 0.00  ☆☆☆☆☆ (0.0)
  » 
Add a book to library? Enter number (or 0 to skip): 1
  [LOG] [2026-06-03 11:48:54] [SAVE    ] Saved 9 books → data/books.csv  (backup: data/books_backup_20260603_114854.csv)

  ✅  Added "Romancing Mister Bridgerton" to the library!
╔══════════════════════════════════════════════════════════╗
  BOOK DETAILS
╠══════════════════════════════════════════════════════════╣
  Title       : Romancing Mister Bridgerton
  Author      : ],
  ISBN        : —
  Publisher   : —
  Published   : —
  Genre       : 
  Pages       : 0
  Rating      : ☆☆☆☆☆ (0.0)
  Price       : $0.00
  Status      : Available
  PDF Link    : —
  Buy Link    : —
╠══════════════════════════════════════════════════════════╣
  Description :
  —
╚══════════════════════════════════════════════════════════╝


════════════════════════════════════════════════════════════════════════
  📚  Lynnwood Public Library   (9 books)
────────────────────────────────────────────────────────────────────────
  [1] View Catalog        [6] Check Out / Return
  [2] Add Book            [7] 🔍 Research Online
  [3] Remove Book         [8] Data Structures View
  [4] Search Catalog      [9] Export
  [5] Book Detail         [0] Save & Exit
════════════════════════════════════════════════════════════════════════
  » Enter choice: 2

════════════════════════════════════════════════════════════════════════
  ADD A NEW BOOK
════════════════════════════════════════════════════════════════════════
  » Title: Shatter Me
  » Author: Tahereh Mafi 
  » ISBN (optional): 978-0062085504
  » Publisher: HapperCollins
  » Published Date (YYYY-MM-DD): 2011-11-15
  » Genre: Young Adult Dystopian 
  » Page Count: 338
  » Rating (0.0–5.0): 3.8
  » Price (USD): 8.99        
  » PDF / Preview Link (optional): 
  » Buy Link (optional): https://www.amazon.com/Shatter-Me-Tahereh-Mafi/dp/0062085506
  Description (press Enter twice to finish):
The gripping first installment in global bestselling author Tahereh Mafi’s epic, romantic Shatter Me series.

One touch is all it takes. One touch, and Juliette Ferrars can leave a fully grown man gasping for air. One touch, and she can kill.

No one knows why Juliette has such incredible power. It feels like a curse, a burden that one person alone could never bear. But The Reestablishment sees it as a gift, sees her as an opportunity. An opportunity for a deadly weapon.

Juliette has never fought for herself before. But when she’s reunited with the one person who ever cared about her, she finds a strength she never knew she had.


  [LOG] [2026-06-03 11:53:33] [SAVE    ] Saved 10 books → data/books.csv  (backup: data/books_backup_20260603_115333.csv)

  ✅  "Shatter Me" added to the library!
╔══════════════════════════════════════════════════════════╗
  BOOK DETAILS
╠══════════════════════════════════════════════════════════╣
  Title       : Shatter Me
  Author      : Tahereh Mafi
  ISBN        : 978-0062085504
  Publisher   : HapperCollins
  Published   : 2011-11-15
  Genre       : Young Adult Dystopian 
  Pages       : 338
  Rating      : ★★★½☆ (3.8)
  Price       : $8.99
  Status      : Available
  PDF Link    : —
  Buy Link    : https://www.amazon.com/Shatter-Me-Tahereh-Mafi/dp/0062085506
╠══════════════════════════════════════════════════════════╣
  Description :
  The gripping first installment in global bestselling 
   author Tahereh Mafi’s epic, romantic Shatter Me series.  
   One touch is all it takes. One touch, and Juliette Ferrars 
   can leave a fully grown man gasping for air. One touch, 
   and she can kill.  No one knows why Juliette has such 
   incredible power. It feels like a curse, a burden that one 
   person alone could never bear. But The Reestablishment 
   sees it as a gift, sees her as an opportunity. An 
   opportunity for a deadly weapon.  Juliette has never 
   fought for herself before. But when she’s reunited with 
   the one person who ever cared about her, she finds a 
   strength she never knew she had.
╚══════════════════════════════════════════════════════════╝


════════════════════════════════════════════════════════════════════════
  📚  Lynnwood Public Library   (10 books)
────────────────────────────────────────────────────────────────────────
  [1] View Catalog        [6] Check Out / Return
  [2] Add Book            [7] 🔍 Research Online
  [3] Remove Book         [8] Data Structures View
  [4] Search Catalog      [9] Export
  [5] Book Detail         [0] Save & Exit
════════════════════════════════════════════════════════════════════════
  » Enter choice: 0
  [LOG] [2026-06-03 11:54:46] [SAVE    ] Saved 10 books → data/books.csv  (backup: data/books_backup_20260603_115446.csv)

  ✅  Saved 10 books to data/books.csv

  👋  Goodbye! Library data has been saved.

*/