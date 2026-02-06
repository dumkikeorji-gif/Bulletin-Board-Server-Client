import java.net.*;
import java.io.*;

public class BulletinClient {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    // Constructor connects to the server
    public BulletinClient(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }

    // Send a command to the server
    public void sendCommand(String command) {
        out.println(command);
    }

    // Listen for server messages on a background thread
    public void listen(ServerListener listener) {
        new Thread(() -> {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    listener.onMessage(msg); // callback to GUI
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Interface so GUI can receive messages
    public interface ServerListener {
        void onMessage(String msg);
    }
}