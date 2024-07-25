import javax.swing.*;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.HashSet;

public class Server {
    private static final int PORT = 5050;
    private static HashSet<PrintWriter> clientWriters = new HashSet<>();
    private JTextArea messageArea;
    private static JPanel messagePanel;
    private JTextField inputField;
    private DefaultListModel<String> userListModel;
    private PrintWriter out;
    private ServerSocket serverSocket;

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

        frame.setVisible(true);

        startServer();

    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            // Append the message to the messagePanel
            addMessageToPanel("Server: " + message);
            // Send the message to all clients
            synchronized (clientWriters) {
                for (PrintWriter writer : clientWriters) {
                    writer.println("Server: " + message);
                }
            }
            inputField.setText("");
        }
    }

    //function to start the server
    private void startServer() {
        try {
            serverSocket = new ServerSocket(5050);
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Starting server on address " + hostAddress + ":" + PORT);

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

    private static void addMessageToPanel(String message) {
        JLabel messageLabel = new JLabel(message);
        messagePanel.add(messageLabel);
        messagePanel.revalidate();
        JScrollBar verticalScrollBar = ((JScrollPane) messagePanel.getParent().getParent()).getVerticalScrollBar();
        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }

    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;

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
            }
        }
    }
}