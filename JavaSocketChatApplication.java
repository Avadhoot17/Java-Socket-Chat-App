// ===============================
// FILE: ChatServer.java
// ===============================

import java.io.*;
import java.net.*;
import java.util.*;

public class ChatServer {

    private static final int PORT = 5000;
    private static Set<ClientHandler> clients = new HashSet<>();

    public static void main(String[] args) {
        System.out.println("Server started on port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("New client connected: " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket);
                clients.add(handler);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Broadcast message to all clients
    public static void broadcast(String message, ClientHandler excludeUser) {
        for (ClientHandler client : clients) {
            if (client != excludeUser) {
                client.sendMessage(message);
            }
        }
    }

    // Remove disconnected client
    public static void removeClient(ClientHandler client) {
        clients.remove(client);
    }
}


// ===============================
// FILE: ClientHandler.java
// ===============================

class ClientHandler implements Runnable {

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String userName;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            out.println("Enter your name:");
            userName = in.readLine();

            ChatServer.broadcast(userName + " joined the chat!", this);

            String message;
            while ((message = in.readLine()) != null) {
                if (message.equalsIgnoreCase("exit")) {
                    break;
                }

                ChatServer.broadcast(userName + ": " + message, this);
            }

        } catch (IOException e) {
            System.out.println("Connection error with client: " + userName);
        } finally {
            try {
                socket.close();
            } catch (IOException ignored) {}

            ChatServer.removeClient(this);
            ChatServer.broadcast(userName + " left the chat.", this);
            System.out.println(userName + " disconnected.");
        }
    }

    public void sendMessage(String message) {
        out.println(message);
    }
}


// ===============================
// FILE: ChatClient.java
// ===============================

import java.io.*;
import java.net.*;

public class ChatClient {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;

    public static void main(String[] args) {
        System.out.println("Connecting to chat server...");

        try (Socket socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {

            // Thread to read messages from server
            new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = in.readLine()) != null) {
                        System.out.println(serverMsg);
                    }
                } catch (IOException e) {
                    System.out.println("Disconnected from server.");
                }
            }).start();

            // Send messages from console to server
            String userInput;
            while ((userInput = console.readLine()) != null) {
                out.println(userInput);

                if (userInput.equalsIgnoreCase("exit")) {
                    break;
                }
            }

        } catch (IOException e) {
            System.out.println("Unable to connect to server.");
            e.printStackTrace();
        }
    }
}
