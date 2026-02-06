package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class BBoard {

    // CLASS FIELDS for server config
    private static int boardW, boardH, noteW, noteH;
    private static String[] colors;

    public static void main(String[] args) {

        // Step 1: Validate command-line args
        if (args.length < 6) {
            System.out.println("Usage: java server.BBoard <port> <boardW> <boardH> <noteW> <noteH> <color1> [color2] ...");
            System.out.println("Example: java server.BBoard 4554 200 100 20 10 red green blue");
            return;
        }

        // Step 2: Parse args into server config
        int port;
        try {
            port   = Integer.parseInt(args[0]);
            boardW = Integer.parseInt(args[1]);
            boardH = Integer.parseInt(args[2]);
            noteW  = Integer.parseInt(args[3]);
            noteH  = Integer.parseInt(args[4]);

            colors = new String[args.length - 5];
            for (int i = 5; i < args.length; i++) {
                colors[i - 5] = args[i].toLowerCase();
            }

        } catch (NumberFormatException e) {
            System.out.println("Error: port/boardW/boardH/noteW/noteH/maxNotes must be integers.");
            return;
        }

        // Step 3: Print config to confirm everything parsed correctly
        System.out.println("Config:");
        System.out.println("port=" + port + ", board=" + boardW + "x" + boardH +
                ", note=" + noteW + "x" + noteH );

        System.out.print("colors=");
        for (int i = 0; i < colors.length; i++) {
            System.out.print(colors[i] + (i == colors.length - 1 ? "\n" : ", "));
        }
        //Add Sharedboard to manage notes and pins across all clients
        SharedBboard sharedBoard = new SharedBboard(boardW, boardH, noteW, noteH, colors);
        
        // Step 4: Start server socket + accept clients forever
        try (ServerSocket serverSocket = new ServerSocket(port)) {

            System.out.println("Server listening on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println(
                    "Client connected: " + clientSocket.getRemoteSocketAddress()
                );

                ClientHandler handler = new ClientHandler(
                    clientSocket,
                    boardW, boardH,
                    noteW, noteH,
                    colors,
                    sharedBoard
                );

                handler.start(); // run ClientHandler in its own thread
            }

        } 
        catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
}
