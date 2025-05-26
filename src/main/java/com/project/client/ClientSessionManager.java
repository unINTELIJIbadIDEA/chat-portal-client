package com.project.client;

import com.project.models.message.Message;
import com.project.utils.Config;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ClientSessionManager {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ClientConnection connection = new ClientConnection();
    private volatile boolean running;
    private final String chatId;
    private final String token;
    private ClientMessageSender sender;
    private Consumer<Message> messageConsumer;


    public ClientSessionManager(String chatId, String token, Consumer<Message> messageConsumer) {
        this.chatId = chatId;
        this.token = token;
        this.messageConsumer = messageConsumer;
    }

    public void startSession() {
        try {
            String host = Config.getHOST_SERVER();
            int port = Config.getPORT_SERVER();

            System.out.println("=== CLIENT CONNECTION DEBUG ===");
            System.out.println("Connecting to: " + host + ":" + port);
            System.out.println("Chat ID: " + chatId);
            System.out.println("Token length: " + (token != null ? token.length() : "null"));

            connection.connect();
            running = true;
            sender = new ClientMessageSender(connection, chatId);
            ClientMessageReceiver receiver = new ClientMessageReceiver(connection);
            receiver.setMessageHandler(messageConsumer);
            executor.execute(receiver);

            if (chatId != null) {
                System.out.println("Sending join message...");
                sender.sendMessage("/join " + chatId, token);
                System.out.println("Join message sent successfully");
            }

            System.out.println("Session started successfully");
        } catch (IOException e) {
            System.err.println("=== CONNECTION ERROR ===");
            System.err.println("Host: " + Config.getHOST_SERVER());
            System.err.println("Port: " + Config.getPORT_SERVER());
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            endSession();
        }
    }

    public void sendMessage(String message) throws IOException {
        sender.sendMessage(message, token);
    }

    public void endSession() {
        running = false;
        connection.disconnect();
        executor.shutdownNow();
        System.out.println("Sesja zakończona");
    }

    public boolean isRunning() {
        return running;
    }

    public String getToken() {
        return token;
    }
}