package server;

import java.util.*;

/**
 * Shared, thread-safe bulletin board state.
 * One single board instance is shared across all connected clients.
 * All public methods are synchronized so each command runs atomically.
 */
public class SharedBboard {

    /* =========================
       Inner data classes
       ========================= */

    // Represents a single note on the board
    public static class Note {
        public final int x, y;        // top-left corner of the note
        public final String color;    // note color (stored lowercase)
        public final String message;  // note message text

        public Note(int x, int y, String color, String message) {
            this.x = x;
            this.y = y;
            this.color = color;
            this.message = message;
        }
    }

    // Represents a pin (a single point on the board)
    public static class Pin {
        public final int x, y;

        public Pin(int x, int y) {
            this.x = x;
            this.y = y;
        }

        // Pins are equal if they have the same coordinates
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Pin)) return false;
            Pin other = (Pin) o;
            return x == other.x && y == other.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /* =========================
       Board configuration
       ========================= */

    private final int boardW, boardH;
    private final int noteW, noteH;

    // All valid colors stored in lowercase
    private final Set<String> validColors;

    private final List<Note> notes = new ArrayList<>();
    private final Set<Pin> pins = new HashSet<>();

    // Constructor — called once when the server starts
    public SharedBboard(int boardW, int boardH, int noteW, int noteH, String[] colors) {
        this.boardW = boardW;
        this.boardH = boardH;
        this.noteW = noteW;
        this.noteH = noteH;

        // Normalize all valid colors to lowercase
        this.validColors = new HashSet<>();
        for (String c : colors) {
            validColors.add(c.toLowerCase());
        }
    }

    /* =========================
       Helper methods
       ========================= */

    // Check if a note fits fully on the board
    private boolean fitsOnBoard(int x, int y) {
        return x >= 0 && y >= 0 &&
               x + noteW <= boardW &&
               y + noteH <= boardH;
    }

    // Check if color is supported (expects lowercase)
    private boolean isColorValid(String c) {
        return validColors.contains(c);
    }

    // Check if another note already exists at the same top-left corner
    private boolean isCompleteOverlap(int x, int y) {
        for (Note n : notes) {
            if (n.x == x && n.y == y) return true;
        }
        return false;
    }

    // Check if a point is inside a note
    private boolean noteContainsPoint(Note n, int px, int py) {
        return px >= n.x && px < n.x + noteW &&
               py >= n.y && py < n.y + noteH;
    }

    // Check if any note contains a point
    private boolean anyNoteContainsPoint(int x, int y) {
        for (Note n : notes) {
            if (noteContainsPoint(n, x, y)) return true;
        }
        return false;
    }

    // Check if a note is pinned
    private boolean isPinned(Note n) {
        for (Pin p : pins) {
            if (noteContainsPoint(n, p.x, p.y)) return true;
        }
        return false;
    }

    /* =========================
       Command operations
       ========================= */

    // POST x y color message
    public synchronized String post(int x, int y, String color, String message) {

        // Normalize color to lowercase so BLUE/Blue/blue all work
        color = color.toLowerCase();

        if (!fitsOnBoard(x, y))
            return "ERROR OUT_OF_BOUNDS Note exceeds board boundaries";

        if (!isColorValid(color))
            return "ERROR COLOR_NOT_SUPPORTED " + color;

        if (isCompleteOverlap(x, y))
            return "ERROR COMPLETE_OVERLAP Note overlaps an existing note entirely";

        notes.add(new Note(x, y, color, message));
        return "OK NOTE_POSTED";
    }

    // PIN x y
    public synchronized String pin(int x, int y) {
        if (!anyNoteContainsPoint(x, y))
            return "ERROR NO_NOTE_AT_COORDINATE";

        pins.add(new Pin(x, y));
        return "OK PIN_ADDED";
    }

    // UNPIN x y
    public synchronized String unpin(int x, int y) {
        if (!anyNoteContainsPoint(x, y))
            return "ERROR NO_NOTE_AT_COORDINATE";

        boolean removed = pins.remove(new Pin(x, y));
        if (!removed)
            return "ERROR PIN_NOT_FOUND";

        return "OK PIN_REMOVED";
    }

    // SHAKE — remove all unpinned notes
    public synchronized String shake() {
        notes.removeIf(n -> !isPinned(n));
        pins.removeIf(p -> !anyNoteContainsPoint(p.x, p.y));
        return "OK SHAKE_COMPLETE";
    }

    // CLEAR — remove everything
    public synchronized String clear() {
        notes.clear();
        pins.clear();
        return "OK CLEAR_COMPLETE";
    }

    /* =========================
       GET helpers
       ========================= */

    // Safe snapshot of pins
    public synchronized List<Pin> getPinsSnapshot() {
        return new ArrayList<>(pins);
    }

    // Check if a note is pinned
    public synchronized boolean isNotePinned(Note n) {
        return isPinned(n);
    }

    // Return notes matching optional filters
    public synchronized List<Note> getNotesFiltered(
            String colorFilter,
            Integer containsX,
            Integer containsY,
            String refersTo
    ) {
        List<Note> result = new ArrayList<>();

        for (Note n : notes) {

            // Filter by color
            if (colorFilter != null && !n.color.equals(colorFilter))
                continue;

            // Filter by contains=x,y
            if (containsX != null && containsY != null &&
                !noteContainsPoint(n, containsX, containsY))
                continue;

            // Filter by refersTo substring
            if (refersTo != null && !n.message.contains(refersTo))
                continue;

            result.add(n);
        }

        return result;
    }
}
