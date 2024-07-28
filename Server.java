import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.security.Permissions;
import java.util.HashSet;
import java.util.Properties;

public class Server {
    private static final int PORT = 5050;
    private static HashSet<PrintWriter> clientWriters = new HashSet<>();
    private static JPanel messagePanel;
    private JTextField inputField;
    private static DefaultListModel<String> userListModel;
    private ServerSocket serverSocket;
    private static String username;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Server().createUI();
        });

    }

    private void createUI() {
        JFrame frame = new JFrame("Server");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800,600);
        frame.setLayout(new BorderLayout());

        //message area
        // Message panel in the center
        messagePanel = new JPanel();
        messagePanel.setLayout(new BoxLayout(messagePanel, BoxLayout.Y_AXIS));
        JScrollPane messageScrollPane = new JScrollPane(messagePanel);
        messageScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        frame.add(messageScrollPane, BorderLayout.CENTER);

        //message input field
        inputField = new JTextField();
        inputField.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });
        frame.add(inputField, BorderLayout.SOUTH);

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

        startServer();

    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            // Append the message to the messagePanel
            addMessageToPanel("Me: " + message);
            // Send the message to all clients
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println(username + ": " + message);
                }
            }
            inputField.setText("");
        }
    }

    private static void updateUserList(String user) {
        if (!userListModel.contains(user)) {
            userListModel.addElement(user);
        }
    }

    private static void removeUserFromList(String user) {
        if(userListModel.contains(user)) {
            userListModel.removeElement(user);
        }
    }

    //function to start the server
    private void startServer() {
        try {
            serverSocket = new ServerSocket(5050);
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Starting server on address " + hostAddress + ":" + PORT);

            //store the ip address into the system properties
            storeIPAddress(hostAddress);

            new Thread(() -> {
                while (true) {
                    try {
                        new ClientHandler(serverSocket.accept()).start();;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }     
    }

    private void storeIPAddress(String ipAddress) {
        Properties p = new Properties(System.getProperties());
        p.setProperty("ip.address", ipAddress);
        try {
            FileOutputStream fos = new FileOutputStream("system.properties");
            p.store(fos, "System Properties");
            System.setProperties(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void addMessageToPanel(String message) {
        JLabel messageLabel = new JLabel(message);
        messagePanel.add(messageLabel);
        messagePanel.revalidate();
        JScrollBar verticalScrollBar = ((JScrollPane) messagePanel.getParent().getParent()).getVerticalScrollBar();
        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }

    private void stop() {
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String clientUsername;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            System.out.println("New client connected: " + socket.getInetAddress().getHostAddress());
        }
        

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                synchronized (clientWriters) {
                    clientWriters.add(out);
                     // Get username
                    clientUsername = in.readLine();
                    if (clientUsername != null && clientUsername.startsWith("USERNAME:")) {
                    clientUsername = clientUsername.substring(9);
                    out.println("USER:" + username); // Send server username to the client
                    SwingUtilities.invokeLater(() -> updateUserList(clientUsername)); // Update user list with client username
                }
                }

                while(true) {
                    String message = in.readLine();
                    if(message == null) {
                        break;
                    }
                    System.out.println("Recceived: " + message);
                    SwingUtilities.invokeLater(() -> addMessageToPanel(message));
                }
            } catch(IOException e) {
                System.err.println("Error handling client: " + e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    socket.close();
                    System.out.println("Client disconnected: " + socket.getInetAddress().getHostAddress());
                } catch(IOException e) {
                    System.err.println("Error closing client socket: " + e.getMessage());
                    e.printStackTrace();
                }
                synchronized(clientWriters) {
                    clientWriters.remove(out);
                }
                SwingUtilities.invokeLater(() -> removeUserFromList(clientUsername));
            }
        }
    }

    
}