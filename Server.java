import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 5050;
    private static HashSet<PrintWriter> clientWriters = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Chat server starting...");
        try(ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Chat server started on port " + PORT);
            while(true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch(IOException e) {
            System.err.println("Could not start server: " + e.getMessage());
            e.printStackTrace();
        }
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

                String message;
                while ((message = in.readLine()) != null) {
                    System.out.println("Recceived: " + message);
                    for(PrintWriter writer : clientWriters) {
                        writer.println(message);
                    }
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