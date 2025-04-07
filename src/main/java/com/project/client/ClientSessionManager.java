package com.project.client;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientSessionManager {

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ClientConnection connection = new ClientConnection();
    private volatile boolean running;
    private final String chatId;
    private final String token;

    public ClientSessionManager(String chatId, String token) {
        this.chatId = chatId;
        this.token = token;
    }

    public void startSession() {
        try {
            connection.connect();
            running = true;
            ClientMessageSender sender = new ClientMessageSender(connection, chatId);
            executor.execute(new ClientMessageReceiver(connection));

            if (chatId != null) {
                sender.sendMessage("/join " + chatId, token);
            }

            new UserInputHandler(sender, this).start();
        } catch (IOException e) {
            System.out.println("Błąd połączenia: " + e.getMessage());
            endSession();
        }
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