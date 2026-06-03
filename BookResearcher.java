package library.service;

import library.model.Book;

import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * BookResearcher — fetches live book data from public APIs.
 *
 * Sources used:
 *   1. Google Books API   — https://www.googleapis.com/books/v1/volumes?q=
 *   2. Open Library API   — https://openlibrary.org/search.json?q=
 *
 * No API key required for either source (rate limits apply).
 * All results include: title, author, ISBN, publisher, publishedDate,
 * description, pageCount, rating, price info, and any available PDF/preview links.
 */
public class BookResearcher {

    private static final String GOOGLE_BOOKS = "https://www.googleapis.com/books/v1/volumes?maxResults=5&q=";
    private static final String OPEN_LIBRARY = "https://openlibrary.org/search.json?limit=5&q=";

    private final HttpClient http;
    private final FileService fileService;

    public BookResearcher(FileService fileService) {
        this.fileService = fileService;
        this.http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(8))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    }

    // ── Public entry point ─────────────────────────────────────────────────
    /**
     * Research a query string. Tries Google Books first; falls back to Open Library.
     * Returns up to 5 Book objects populated with whatever data is available.
     */
    public List<Book> research(String query) throws IOException, InterruptedException {
        System.out.println("\n  🔍  Searching Google Books for: " + query + " …");
        List<Book> results = new ArrayList<>();

        try {
            results = searchGoogleBooks(query);
        } catch (Exception e) {
            System.out.println("  ⚠  Google Books unavailable (" + e.getMessage() + "). Trying Open Library…");
        }

        if (results.isEmpty()) {
            try {
                results = searchOpenLibrary(query);
            } catch (Exception e) {
                System.out.println("  ⚠  Open Library also unavailable: " + e.getMessage());
            }
        }

        // Log the research
        if (!results.isEmpty()) {
            StringBuilder log = new StringBuilder();
            results.forEach(b -> log.append(b.toDetailString()).append("\n"));
            try { fileService.appendResearchResult(query, log.toString()); }
            catch (IOException ignored) {}
        }

        return results;
    }

    // ── Google Books ───────────────────────────────────────────────────────
    private List<Book> searchGoogleBooks(String query) throws Exception {
        String url = GOOGLE_BOOKS + URLEncoder.encode(query, StandardCharsets.UTF_8);
        String json = get(url);
        return parseGoogleBooks(json);
    }

    private List<Book> parseGoogleBooks(String json) {
        List<Book> books = new ArrayList<>();
        // minimal JSON parser — no external deps
        String[] items = splitByItems(json, "\"volumeInfo\"");
        for (String item : items) {
            try {
                String title       = extract(item, "\"title\"");
                String author      = extractFirstFromArray(item, "\"authors\"");
                String publisher   = extract(item, "\"publisher\"");
                String pubDate     = extract(item, "\"publishedDate\"");
                String description = extract(item, "\"description\"");
                int    pages       = extractInt(item, "\"pageCount\"");
                double rating      = extractDouble(item, "\"averageRating\"");
                String isbn        = extractIsbn(item);
                String genre       = extractFirstFromArray(item, "\"categories\"");
                String previewLink = extract(item, "\"previewLink\"");
                String infoLink    = extract(item, "\"infoLink\"");
                String pdfAvail    = extract(item, "\"isAvailable\"");

                // retail price
                double price = 0;
                String priceStr = extractFromSection(item, "\"retailPrice\"", "\"amount\"");
                if (!priceStr.isEmpty()) { try { price = Double.parseDouble(priceStr); } catch (Exception ignored) {} }
                String buyLink  = extract(item, "\"buyLink\"");

                if (title == null || title.isBlank()) continue;
                if (author == null || author.isBlank()) author = "Unknown";

                Book b = new Book(title, author, isbn != null ? isbn : "");
                b.setPublisher(publisher);
                b.setPublishedDate(pubDate);
                b.setDescription(description);
                b.setPageCount(pages);
                b.setRating(rating);
                b.setGenre(genre);
                b.setPrice(price);
                b.setPdfLink("true".equals(pdfAvail) ? previewLink : "");
                b.setBuyLink(buyLink != null ? buyLink : (infoLink != null ? infoLink : ""));

                books.add(b);
            } catch (Exception ignored) {}
        }
        return books;
    }

    // ── Open Library ──────────────────────────────────────────────────────
    private List<Book> searchOpenLibrary(String query) throws Exception {
        String url = OPEN_LIBRARY + URLEncoder.encode(query, StandardCharsets.UTF_8);
        String json = get(url);
        return parseOpenLibrary(json);
    }

    private List<Book> parseOpenLibrary(String json) {
        List<Book> books = new ArrayList<>();
        String[] docs = splitByItems(json, "\"key\":\"/works/");
        for (String doc : docs) {
            try {
                String title   = extract(doc, "\"title\"");
                String author  = extractFirstFromArray(doc, "\"author_name\"");
                String pubDate = extractFirstFromArray(doc, "\"publish_date\"");
                int    pages   = extractInt(doc, "\"number_of_pages_median\"");
                double rating  = extractDouble(doc, "\"ratings_average\"");
                String isbn    = extractFirstFromArray(doc, "\"isbn\"");
                String genre   = extractFirstFromArray(doc, "\"subject\"");

                if (title == null || title.isBlank()) continue;
                if (author == null || author.isBlank()) author = "Unknown";

                Book b = new Book(title, author, isbn != null ? isbn : "");
                if (pubDate != null && !pubDate.isBlank() && pubDate.length() >= 4)
                    b.setPublishedDate(pubDate.substring(0, 4) + "-01-01");
                b.setPageCount(pages);
                b.setRating(Math.min(5.0, rating));
                b.setGenre(genre);
                String key = extract(doc, "\"key\"");
                if (key != null && !key.isBlank())
                    b.setBuyLink("https://openlibrary.org" + key);

                books.add(b);
            } catch (Exception ignored) {}
        }
        return books;
    }

    // ── HTTP helper ────────────────────────────────────────────────────────
    private String get(String url) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Accept", "application/json")
            .header("User-Agent", "LibraryManagementSystem/1.0")
            .timeout(Duration.ofSeconds(10))
            .GET().build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200)
            throw new IOException("HTTP " + resp.statusCode() + " from " + url);
        return resp.body();
    }

    // ── Minimal JSON helpers ───────────────────────────────────────────────
    private String[] splitByItems(String json, String delimiter) {
        return json.split(delimiter);
    }

    private String extract(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return "";
        int start = json.indexOf('"', i + key.length() + 1);
        if (start < 0) return "";
        start++;
        StringBuilder sb = new StringBuilder();
        for (int k = start; k < json.length(); k++) {
            char c = json.charAt(k);
            if (c == '"' && (k == 0 || json.charAt(k - 1) != '\\')) break;
            if (c == '\\' && k + 1 < json.length()) { k++; sb.append(json.charAt(k)); continue; }
            sb.append(c);
        }
        return sb.toString().trim();
    }

    private String extractFromSection(String json, String section, String key) {
        int sec = json.indexOf(section);
        if (sec < 0) return "";
        int end = json.indexOf('}', sec);
        return extract(json.substring(sec, end > sec ? end : json.length()), key);
    }

    private String extractFirstFromArray(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return "";
        int arr = json.indexOf('[', i);
        if (arr < 0) return "";
        int start = json.indexOf('"', arr + 1);
        if (start < 0) return "";
        return extract(json.substring(start - 1), "\"");
    }

    private int extractInt(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return 0;
        int colon = json.indexOf(':', i);
        if (colon < 0) return 0;
        StringBuilder sb = new StringBuilder();
        for (int k = colon + 1; k < json.length(); k++) {
            char c = json.charAt(k);
            if (Character.isDigit(c)) sb.append(c);
            else if (sb.length() > 0) break;
        }
        try { return Integer.parseInt(sb.toString()); } catch (Exception e) { return 0; }
    }

    private double extractDouble(String json, String key) {
        int i = json.indexOf(key);
        if (i < 0) return 0;
        int colon = json.indexOf(':', i);
        if (colon < 0) return 0;
        StringBuilder sb = new StringBuilder();
        for (int k = colon + 1; k < json.length(); k++) {
            char c = json.charAt(k);
            if (Character.isDigit(c) || c == '.') sb.append(c);
            else if (sb.length() > 0) break;
        }
        try { return Double.parseDouble(sb.toString()); } catch (Exception e) { return 0; }
    }

    private String extractIsbn(String json) {
        // prefer ISBN-13
        int i = json.indexOf("ISBN_13");
        if (i < 0) i = json.indexOf("ISBN_10");
        if (i < 0) return "";
        return extractFirstFromArray(json.substring(i), "\"identifier\"");
    }
}
