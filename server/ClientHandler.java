package server;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler extends Thread {

    // Each client gets its own socket connection
    private final Socket socket;

    // Used to read text lines from the client (one line per command)
    private final BufferedReader in;

    // Used to send text lines back to the client
    private final PrintWriter out;

    // Server configuration values (same for every client)
    private final int boardW, boardH, noteW, noteH;
    private final String[] colors;

    // SHARED bulletin board (same object used by all clients)
    private final SharedBboard board;

    // Constructor runs when the server accepts a new client
    public ClientHandler(Socket socket,
                         int boardW, int boardH,
                         int noteW, int noteH,
                         String[] colors,
                         SharedBboard board) throws IOException {

        // Save the socket for this client
        this.socket = socket;

        // Save configuration settings
        this.boardW = boardW;
        this.boardH = boardH;
        this.noteW = noteW;
        this.noteH = noteH;
        this.colors = colors;

        // Save the shared board reference
        this.board = board;

        // Set up input stream (client -> server)
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Set up output stream (server -> client)
        // 'true' means auto-flush so messages go out immediately
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
    }

    @Override
    public void run() {
        try {
            // ---------------------------
            // 1) Send handshake message
            // ---------------------------
            // When a client connects, we immediately send the board + note dimensions
            // plus the list of allowed colors.
            StringBuilder sb = new StringBuilder();
            sb.append("WELCOME ")
              .append(boardW).append(" ")
              .append(boardH).append(" ")
              .append(noteW).append(" ")
              .append(noteH);

            // Add all valid colors to the handshake (normalize to lowercase)
            for (String c : colors) sb.append(" ").append(c.toLowerCase());

            // Send handshake to client
            out.println(sb.toString());

            // ---------------------------
            // 2) Main command loop
            // ---------------------------
            // Keep reading commands until the client disconnects
            String line;
            while ((line = in.readLine()) != null) {

                // Remove extra spaces
                line = line.trim();

                // Ignore blank lines
                if (line.isEmpty()) continue;

                // Debug log on server side
                System.out.println("FROM CLIENT: " + line);

                // Handle the command and produce a response string (or null)
                String response = handleCommand(line);

                // Some commands like GET send multiple lines inside handleGET(),
                // so they return null and we do NOT print here.
                if (response != null) out.println(response);
            }

        } catch (IOException e) {
            // This happens when the client closes or connection drops
            System.out.println("Client disconnected: " + e.getMessage());
        } finally {
            // Always close socket in the end to free resources
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // This chooses which command handler to run
    private String handleCommand(String line) {

        // Split into: command + rest of line (only 2 pieces)
        // Example: "POST 10 10 red hello world"
        // cmd = POST, rest = "10 10 red hello world"
        String[] firstSplit = line.split("\\s+", 2);

        // Normalize command to uppercase so user can type post/POST/Post
        String command = firstSplit[0].toUpperCase();

        switch (command) {

            // POST x y color message...
            case "POST":
                return handlePOST(line);

            // GET filters OR GET PINS
            case "GET":
                return handleGET(line); // may print multiple lines and return null

            // PIN x y
            case "PIN":
                return handlePIN(line);

            // UNPIN x y
            case "UNPIN":
                return handleUNPIN(line);

            // SHAKE has no arguments
            case "SHAKE":
                return handleNoArg(line, "SHAKE", board.shake());

            // CLEAR has no arguments
            case "CLEAR":
                return handleNoArg(line, "CLEAR", board.clear());

            // DISCONNECT closes this client gracefully
            case "DISCONNECT":
                out.println("OK BYE"); // server tells client it is closing nicely
                try { socket.close(); } catch (IOException ignored) {}
                return null;

            // Anything else is invalid
            default:
                return "ERROR INVALID_FORMAT Unknown command";
        }
    }

    // ---------------------------
    // POST command
    // POST x y color message(with spaces)
    // ---------------------------
    private String handlePOST(String line) {

        // Split into at most 5 pieces:
        // POST | x | y | color | message (message can contain spaces)
        String[] parts = line.split("\\s+", 5);

        // Need at least: POST x y color message
        if (parts.length < 5) {
            return "ERROR INVALID_FORMAT POST requires coordinates, color, and message";
        }

        // Parse x and y
        Integer x = parseNonNegInt(parts[1]);
        Integer y = parseNonNegInt(parts[2]);

        // x/y must be valid non-negative integers
        if (x == null || y == null) {
            return "ERROR INVALID_FORMAT POST requires non-negative integer coordinates";
        }

        // Color is 4th token
        // ✅ Normalize to lowercase so BLUE/Blue/blue all work
        String color = parts[3].toLowerCase();

        // Message is everything after that
        String message = parts[4];

        // Message cannot be empty
        if (message.trim().isEmpty()) {
            return "ERROR INVALID_FORMAT POST requires coordinates, color, and message";
        }

        // Call the shared board logic (thread-safe)
        // This checks bounds, color validation, overlap rules, etc.
        return board.post(x, y, color, message);
    }

    // ---------------------------
    // PIN command
    // PIN x y
    // ---------------------------
    private String handlePIN(String line) {

        // PIN should be exactly 3 tokens
        String[] parts = line.split("\\s+");
        if (parts.length != 3) {
            return "ERROR INVALID_FORMAT PIN requires x and y";
        }

        Integer x = parseNonNegInt(parts[1]);
        Integer y = parseNonNegInt(parts[2]);

        if (x == null || y == null) {
            return "ERROR INVALID_FORMAT PIN requires non-negative integer coordinates";
        }

        // Ask shared board to add a pin
        return board.pin(x, y);
    }

    // ---------------------------
    // UNPIN command
    // UNPIN x y
    // ---------------------------
    private String handleUNPIN(String line) {

        // UNPIN should be exactly 3 tokens
        String[] parts = line.split("\\s+");
        if (parts.length != 3) {
            return "ERROR INVALID_FORMAT UNPIN requires x and y";
        }

        Integer x = parseNonNegInt(parts[1]);
        Integer y = parseNonNegInt(parts[2]);

        if (x == null || y == null) {
            return "ERROR INVALID_FORMAT UNPIN requires non-negative integer coordinates";
        }

        // Ask shared board to remove a pin
        return board.unpin(x, y);
    }

    // ---------------------------
    // GET command
    // GET PINS
    // OR
    // GET color=<color> contains=<x> <y> refersTo=<substring>
    // Missing criteria means "ALL"
    // ---------------------------
    private String handleGET(String line) {

        // Split the GET line into tokens
        String[] parts = line.split("\\s+");

        // Case 1: GET PINS
        if (parts.length == 2 && parts[1].equalsIgnoreCase("PINS")) {

            // Get a snapshot copy of pins (safe copy)
            List<SharedBboard.Pin> pins = board.getPinsSnapshot();

            // First line: OK <count>
            out.println("OK " + pins.size());

            // Then each pin line
            for (SharedBboard.Pin p : pins) {
                out.println("PIN " + p.x + " " + p.y);
            }

            // We already printed response lines, so return null
            return null;
        }

        // Case 2: GET filters
        // Default values = null means "no filter"
        String colorFilter = null;
        Integer containsX = null;
        Integer containsY = null;
        String refersTo = null;

        // Parse filters after GET
        for (int i = 1; i < parts.length; i++) {
            String t = parts[i];

            // color=<something>
            if (t.startsWith("color=")) {
                // ✅ normalize to lowercase so color=BLUE works
                colorFilter = t.substring("color=".length()).toLowerCase();

            // contains=<x> <y>  (two tokens: contains= and y)
            } else if (t.startsWith("contains=")) {
                String xStr = t.substring("contains=".length());

                // Must have a y token after it
                if (i + 1 >= parts.length) {
                    return "ERROR INVALID_FORMAT GET contains requires x and y";
                }

                String yStr = parts[i + 1];
                i++; // consume y token

                containsX = parseNonNegInt(xStr);
                containsY = parseNonNegInt(yStr);

                if (containsX == null || containsY == null) {
                    return "ERROR INVALID_FORMAT GET contains requires non-negative integer coordinates";
                }

            // refersTo=<substring> (might include spaces, so we capture the rest)
            } else if (t.startsWith("refersTo=")) {

                // Start with current token after "refersTo="
                StringBuilder r = new StringBuilder();
                r.append(t.substring("refersTo=".length()));

                // Add the rest of tokens as part of substring
                for (int j = i + 1; j < parts.length; j++) {
                    r.append(" ").append(parts[j]);
                }

                refersTo = r.toString();
                break; // refersTo consumes the rest of the line

            } else {
                // Unknown filter format
                return "ERROR INVALID_FORMAT GET has invalid filter format";
            }
        }

        // Ask board to return filtered notes
        List<SharedBboard.Note> notes = board.getNotesFiltered(colorFilter, containsX, containsY, refersTo);

        // First line: OK <count>
        out.println("OK " + notes.size());

        // Print each matching note on its own line
        for (SharedBboard.Note n : notes) {
            boolean pinned = board.isNotePinned(n);

            // NOTE x y color message PINNED=true/false
            out.println("NOTE " + n.x + " " + n.y + " " + n.color + " " + n.message + " PINNED=" + pinned);
        }

        // We already printed lines, so return null
        return null;
    }

    // Helper: SHAKE/CLEAR should have no extra tokens
    private String handleNoArg(String line, String name, String okResponse) {
        if (!line.equalsIgnoreCase(name)) {
            return "ERROR INVALID_FORMAT " + name + " takes no arguments";
        }
        return okResponse;
    }

    // Helper: parse a non-negative integer (returns null if invalid)
    private Integer parseNonNegInt(String s) {
        try {
            int v = Integer.parseInt(s);
            if (v < 0) return null;
            return v;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
