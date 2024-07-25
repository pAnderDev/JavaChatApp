import javax.swing.*;
import javax.swing.text.DefaultCaret;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Scanner;

public class Client  {

    private JTextArea messageArea;
    private static JPanel messagePanel;
    private JTextField inputField;
    private DefaultListModel<String> userListModel;
    private static HashSet<PrintWriter> serverWriters = new HashSet<>();
    private PrintWriter out;
    private Socket socket;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Client().createUI();
        });
    }

    private void createUI() {
        JFrame frame = new JFrame("Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800,600);
        frame.setLayout(new BorderLayout());

        //message area
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

        connectToServer();
    }

    private void connectToServer() {
        try {
            String serverAddress = "localhost"; //change for each new server starting location
            int serverPORT = 5050;
            socket = new Socket(serverAddress, serverPORT);
            out = new PrintWriter(socket.getOutputStream(), true);

            new Thread(new RecievedMessageHandler(socket)).start();
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            // Append the message to the messagePanel
            addMessageToPanel("Client: " + message);
            // Send the message to all clients
            out.println("Client: " + message);
            inputField.setText("");
        }
    }

    private static void addMessageToPanel(String message) {
        JLabel messageLabel = new JLabel(message);
        messagePanel.add(messageLabel);
        messagePanel.revalidate();
        JScrollBar verticalScrollBar = ((JScrollPane) messagePanel.getParent().getParent()).getVerticalScrollBar();
        verticalScrollBar.setValue(verticalScrollBar.getMaximum());
    }

    private class RecievedMessageHandler implements Runnable {
        private Socket socket;

        public RecievedMessageHandler(Socket socket) {
            this.socket = socket;

        }

        public void run() {
            try(BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                while(true) {
                    String message = in.readLine();
                    if(message == null) {
                        break;
                    }
                    System.out.println("Recceived: " + message);
                    SwingUtilities.invokeLater(() -> addMessageToPanel(message));
                }
            } catch(IOException e) {
                e.printStackTrace();
            } 
        }

        private void stop() {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
    }
}