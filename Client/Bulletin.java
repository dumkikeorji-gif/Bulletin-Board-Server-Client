import javax.swing.*;
import java.awt.*;

public class Bulletin extends JFrame {

    private BulletinClient client; // networking client
    private JTextArea serverInfoArea;// area to display server messages

    public Bulletin(String ip, int port) {
        setTitle("Pinboard GUI");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(700, 600);
        setLocationRelativeTo(null);

        // MAIN PANEL
        JPanel mainPanel = new JPanel(new BorderLayout());

        // TOP INPUT PANEL
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        // Container to hold the command disconnect
        JPanel bottomContainer = new JPanel(new BorderLayout());

        JLabel label = new JLabel("Enter command here:");
        JTextField commandInput = new JTextField(15);
        JButton clear = new JButton("CLEAR");
        JButton shake = new JButton("SHAKE");
        JButton pin = new JButton("PIN");
        JButton unpin = new JButton("UNPIN");
        JButton get = new JButton("GET");
        topPanel.add(label);
        topPanel.add(commandInput);
        topPanel.add(clear);
        topPanel.add(shake);
        topPanel.add(pin);
        topPanel.add(unpin);
        topPanel.add(get);

        

        clear.addActionListener(e -> clearButtonPressed());
        shake.addActionListener(e -> shakeButtonPressed());
        pin.addActionListener(e -> pinButtonPressed());
        unpin.addActionListener(e -> unpinButtonPressed());
        get.addActionListener(e -> getNoteButtonPressed());

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
            client = new BulletinClient(ip, port);

            // Start listening for server responses
            client.listen(msg -> {
                SwingUtilities.invokeLater(() -> {
                    handleServerMessage(msg, bulletinBoard);
                });
            });

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this,
                    "Could not connect to server.");
            System.exit(ABORT);
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
        scrollPane.setPreferredSize(new Dimension(500, 450));

        bottomContainer.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton disconnect = new JButton("DISCONNECT");
        disconnect.addActionListener(e -> disconnectButtonPressed());
        buttonPanel.add(disconnect);

        bottomContainer.add(buttonPanel, BorderLayout.SOUTH);


        mainPanel.add(bottomContainer, BorderLayout.SOUTH);
    }

    //============================================================================
    // Method to handle messages from the server and display them in the text area
    //============================================================================
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
        String id = JOptionPane.showInputDialog(this, "Enter coords of note to pin:");
        if (id != null && !id.isEmpty()) {
            client.sendCommand("PIN " + id);
        }
    }

    //sending the unpin command to the server when the unpin button is pressed and showing a dialog to get the ID of the note to be unpinned and sending the command to the server
    private void unpinButtonPressed() {
        String id = JOptionPane.showInputDialog(this, "Enter coords of note to unpin:");
        if (id != null && !id.isEmpty()) {
            client.sendCommand("UNPIN " + id);
        }
    }   

    private void disconnectButtonPressed() {
        if (client != null) {
        client.sendCommand("DISCONNECT");
    }
    
    // Close the current board window
    this.dispose();
    
    // Re-open the connection screen
    SwingUtilities.invokeLater(() -> new ConnectionGUI());
    }

    //sending the getnote command to the server when the getnote button is pressed and showing a dialog to get the ID of the note to be retrieved and sending the command to the server
    private void getNoteButtonPressed() {   
        JPanel dialog = new JPanel(new GridLayout(0, 2, 5, 5));

        JTextField newXInput = new JTextField(5);
            JTextField newYInput = new JTextField(5);
        JTextField getColor = new JTextField(5);
        JTextField getHasWord = new JTextField(5);

        // Filter Fields
        dialog.add(new JLabel("Color:"));
        dialog.add(getColor);
        dialog.add(new JLabel("Contains X:"));
        dialog.add(newXInput);
        dialog.add(new JLabel("Contains Y:"));
        dialog.add(newYInput);
        dialog.add(new JLabel("Refers To:"));
        dialog.add(getHasWord);

        // The Special Shortcut Button
        JButton getpins = new JButton("Get All Pinned Coords");
        dialog.add(new JLabel("Shortcuts:"));
        dialog.add(getpins);

        // Logic for the shortcut button inside the dialog
        getpins.addActionListener(e -> {
            client.sendCommand("GET PINS");
            // This trick finds the popup window and closes it automatically
            Window w = SwingUtilities.getWindowAncestor(getpins);
            if (w != null) w.dispose();
        });

        int result = JOptionPane.showConfirmDialog(this, dialog, 
            "Get/Filter Notes", JOptionPane.OK_CANCEL_OPTION);

        // This handles the "OK" button for filtered notes
        if (result == JOptionPane.OK_OPTION) {
            StringBuilder command = new StringBuilder("GET");

            String colorVal = getColor.getText().trim();
            if (!colorVal.isEmpty()) command.append(" color=").append(colorVal.toLowerCase());

            String xVal = newXInput.getText().trim();
            String yVal = newYInput.getText().trim();
            if (!xVal.isEmpty() && !yVal.isEmpty()) {
                command.append(" contains=").append(xVal).append(" ").append(yVal);
            }

            String wordVal = getHasWord.getText().trim();
            if (!wordVal.isEmpty()) command.append(" refersTo=").append(wordVal);

            client.sendCommand(command.toString());
        }
    }
        
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ConnectionGUI());
    }
}
