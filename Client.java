import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.util.Properties;

public class Client {

    private static JPanel messagePanel;
    private JTextField inputField;
    private DefaultListModel<String> userListModel;
    private PrintWriter out;
    private Socket socket;
    private String username;
    private static String serverAddress;
    private static final int PORT = 5050;
    private static final int RECONNECT_INTERVAL = 5000; // 5 seconds

    public static void main(String[] args) {
        if(args.length != 1) {
            System.out.println("Usage: java Client <Destination IP Address>");
            System.exit(1);
        }
        serverAddress = args[0];
        SwingUtilities.invokeLater(() -> {
            new Client().createUI();
        });
    }

    private void createUI() {
        JFrame frame = new JFrame("Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLayout(new BorderLayout());

        // Message area
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        JScrollPane messageScrollPane = new JScrollPane(messagePanel);
        messageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        frame.add(messageScrollPane, BorderLayout.CENTER);

        // Message input field
        inputField = new JTextField();
        inputField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        frame.add(inputField, BorderLayout.SOUTH);

        // User list on the side
        userListModel = new DefaultListModel<>();
        JList<String> userList = new JList<>(userListModel);
        JScrollPane userScrollPane = new JScrollPane(userList);
        userScrollPane.setPreferredSize(new Dimension(150, 0));
        frame.add(userScrollPane, BorderLayout.WEST);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                stop();
            }
        });

        frame.setVisible(true);

        // Prompt for username
        username = JOptionPane.showInputDialog(frame, "Enter your username:", "Username", JOptionPane.PLAIN_MESSAGE);

        if (username == null || username.trim().isEmpty()) {
            username = "Anonymous";
        }

        updateUserList(username);

        connectToServer(serverAddress);

    }

    private void connectToServer(String serverAddress) {
        new Thread(() -> {
            while (true) {
                try {
                    socket = new Socket(serverAddress, PORT);
                    out = new PrintWriter(socket.getOutputStream(), true);

                    // Send username to the server
                    out.println("USERNAME: " + username);

                    new Thread(new ReceivedMessageHandler(socket)).start();
                    break; // Successfully connected, exit the loop

                } catch (IOException e) {
                    System.out.println("Failed to connect to the server. Retrying in " + RECONNECT_INTERVAL / 1000 + " seconds...");
                    try {
                        Thread.sleep(RECONNECT_INTERVAL);
                    } catch (InterruptedException ie) {
                        ie.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void storeIPAddress(String address) {
        Properties p = new Properties(System.getProperties());
        p.setProperty("ip.address", address);
        try {
            FileOutputStream fos = new FileOutputStream("client.properties");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            // Append the message to the messagePanel
            addMessageToPanel("Me: " + message);
            // Send the message to the server
            out.println(username + ": " + message);
            inputField.setText("");
        }
    }

    private void updateUserList(String user) {
        if (!userListModel.contains(user)) {
            userListModel.addElement(user);
        }
    }

    private static void addMessageToPanel(String message) {
        JLabel messageLabel = new JLabel(message);
        messagePanel.add(messageLabel);
        messagePanel.revalidate();
        JScrollBar verticalScrollBar = ((JScrollPane) messagePanel.getParent().getParent()).getVerticalScrollBar();
        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }

    private class ReceivedMessageHandler implements Runnable {
        private Socket socket;

        public ReceivedMessageHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                while (true) {
                    String message = in.readLine();
                    if (message == null) {
                        break;
                    }
                    System.out.println("Received: " + message);
                    SwingUtilities.invokeLater(() -> {
                        if (message.startsWith("USER:")) {
                            updateUserList(message.substring(5));
                        } else {
                            addMessageToPanel(message);
                        }
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void stop() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
