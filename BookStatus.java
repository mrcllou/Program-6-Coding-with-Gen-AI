package library.model;

/** Status of a book in the library system. */
public enum BookStatus {
    AVAILABLE    ("Available"),
    CHECKED_OUT  ("Checked Out"),
    RESERVED     ("Reserved"),
    LOST         ("Lost");

    private final String label;
    BookStatus(String label) { this.label = label; }
    public String getLabel() { return label; }

    public static BookStatus fromString(String s) {
        if (s == null) return AVAILABLE;
        for (BookStatus bs : values())
            if (bs.name().equalsIgnoreCase(s) || bs.label.equalsIgnoreCase(s))
                return bs;
        return AVAILABLE;
    }
}
