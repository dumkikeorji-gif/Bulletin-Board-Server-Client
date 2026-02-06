import javax.swing.*;
import java.awt.*;

public class Bulletin extends JFrame {

    private BulletinClient client; // networking client
    private JTextArea serverInfoArea;// area to display server messages

    public Bulletin() {
        setTitle("Pinboard GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 600);
        setLocationRelativeTo(null);

        // MAIN PANEL
        JPanel mainPanel = new JPanel(new BorderLayout());

        // TOP INPUT PANEL
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        JLabel label = new JLabel("Enter command here:");
        JTextField commandInput = new JTextField(15);
        JButton clear = new JButton("CLEAR");
        JButton shake = new JButton("SHAKE");
        JButton pin = new JButton("PIN");
        JButton unpin = new JButton("UNPIN");
        JButton getNote = new JButton("GETNOTE");
        topPanel.add(label);
        topPanel.add(commandInput);
        topPanel.add(clear);
        topPanel.add(shake);
        topPanel.add(pin);
        topPanel.add(unpin);
        topPanel.add(getNote);

        clear.addActionListener(e -> clearButtonPressed());
        shake.addActionListener(e -> shakeButtonPressed());
        pin.addActionListener(e -> pinButtonPressed());
        unpin.addActionListener(e -> unpinButtonPressed());
        getNote.addActionListener(e -> getNoteButtonPressed());

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(topPanel, BorderLayout.NORTH);
        headerPanel.add(new JSeparator(), BorderLayout.SOUTH);

        mainPanel.add(headerPanel, BorderLayout.NORTH);

        
        // BULLETIN BOARD PANEL (where notes will appear later)
        JPanel bulletinBoard = new JPanel(null);
        bulletinBoard.setBackground(Color.WHITE);
        mainPanel.add(bulletinBoard, BorderLayout.CENTER);

        add(mainPanel);
        //SERVER CONNECTION 
        try {
            // Create client socket connection
            client = new BulletinClient("localhost", 4554);

            // Start listening for server responses
            client.listen(msg -> {
                SwingUtilities.invokeLater(() -> {
                    handleServerMessage(msg, bulletinBoard);
                });
            });

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Could not connect to server.");
            
        }

        // ===============================
        // SEND COMMAND WHEN USER PRESSES ENTER
        // ===============================
        commandInput.addActionListener(e -> {
            String input = commandInput.getText();

            if (!input.isEmpty()) {
                client.sendCommand(input); // send to server
            }

            commandInput.setText(""); // clear field
        });

        setVisible(true);

        serverInfoArea = new JTextArea();
        serverInfoArea.setEditable(false);
        serverInfoArea.setLineWrap(true);
        serverInfoArea.setPreferredSize(new Dimension(500,500));

        JScrollPane scrollPane = new JScrollPane(serverInfoArea);
        scrollPane.setPreferredSize(new Dimension(500, 500));

        mainPanel.add(scrollPane, BorderLayout.SOUTH);
    }

    //Handles messages from the server and uses the text area to display it
    private void handleServerMessage(String msg, JPanel board) {
        serverInfoArea.append(msg + "\n");
    }

    //sending the clear command to the server when the clear button is pressed and showing a message dialog to confirm the action
    private void clearButtonPressed() {
        JOptionPane.showMessageDialog(this,
                    "CLEAR button pressed. Sending CLEAR command to server.");
        client.sendCommand("CLEAR");
    }

    //sending the shake command to the server when the shake button is pressed and showing a dialog to get the new coordinates and ID of the note to be shaken and sending the command to the server
    private void shakeButtonPressed() {
        JOptionPane.showMessageDialog(this,
                    "SHAKE button pressed. Sending SHAKE command to server.");
        client.sendCommand("SHAKE");
    }

    //sending the pin command to the server when the pin button is pressed and showing a dialog to get the ID of the note to be pinned and sending the command to the server
    private void pinButtonPressed() {
        String id = JOptionPane.showInputDialog(this, "Enter ID of note to pin:");
        if (id != null && !id.isEmpty()) {
            client.sendCommand("PIN " + id);
        }
    }

    //sending the unpin command to the server when the unpin button is pressed and showing a dialog to get the ID of the note to be unpinned and sending the command to the server
    private void unpinButtonPressed() {
        String id = JOptionPane.showInputDialog(this, "Enter ID of note to unpin:");
        if (id != null && !id.isEmpty()) {
            client.sendCommand("UNPIN " + id);
        }
    }   

    //sending the getnote command to the server when the getnote button is pressed and showing a dialog to get the ID of the note to be retrieved and sending the command to the server
    private void getNoteButtonPressed() {   
        String id = JOptionPane.showInputDialog(this, "Enter ID of note to get:");
        String pinned = JOptionPane.showInputDialog(this, "Is the note pinned? (yes/no):");
        if (pinned != null && pinned.equalsIgnoreCase("yes")) {
            client.sendCommand("GETNOTE " + id + " PINNED");
        } else {
            client.sendCommand("GETNOTE " + id);
        }
        if (id != null && !id.isEmpty()) {
            client.sendCommand("GETNOTE " + id);
        }
    }
        
    public static void main(String[] args) {
        SwingUtilities.invokeLater(Bulletin::new);
    }
}
