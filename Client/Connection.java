import javax.swing.*;
import java.awt.*;


class ConnectionGUI extends JFrame {
    public ConnectionGUI() {
        setTitle("Connect to Bulletin Board");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JTextField ipField = new JTextField("localhost", 10);
        JTextField portField = new JTextField("4554", 10);
        JButton connectBtn = new JButton("OK");
        JButton quitBtn = new JButton("Quit");

        gbc.gridx = 0; gbc.gridy = 0;
        add(new JLabel("IP:"), gbc);
        gbc.gridx = 1;
        add(ipField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        add(new JLabel("Port:"), gbc);
        gbc.gridx = 1;
        add(portField, gbc);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        btnPanel.add(connectBtn);
        btnPanel.add(quitBtn);

        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
        add(btnPanel, gbc);

        connectBtn.addActionListener(e -> {
            String ip = ipField.getText();
            int port = Integer.parseInt(portField.getText());
            
            // This will attempt connection and open the main Board if successful
            new Bulletin(ip, port); 
            this.dispose(); // Close the connection prompt
        });

        quitBtn.addActionListener(e -> System.exit(0));
        setVisible(true);
    }
}