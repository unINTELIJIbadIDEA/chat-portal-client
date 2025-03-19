package com.project.client;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientSessionManager {

    // fatalnie rozwiązałem i w przyszłości do poprawy, ale na razie mi się nie chce tego ruszać (nie działa jak trzeba xd)
    private static final AtomicInteger SENDER_ID_GENERATOR = new AtomicInteger(1);

    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final ClientConnection connection = new ClientConnection();
    private volatile boolean running;
    private final String chatId;
    private final int senderId;

    public ClientSessionManager(String chatId) {
        this.chatId = chatId;
        this.senderId = SENDER_ID_GENERATOR.getAndIncrement();
    }

    public void startSession() {
        try {
            connection.connect();
            running = true; // Ustawienie flagi przed uruchomieniem pętli użytkownika
            ClientMessageSender sender = new ClientMessageSender(connection, chatId, senderId);
            executor.execute(new ClientMessageReceiver(connection));

            if (chatId != null) {
                sender.sendMessage("/join " + chatId);
            }

            new UserInputHandler(sender, this).start();
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            endSession();
        }
    }

    public void endSession() {
        running = false;
        connection.disconnect();
        executor.shutdown();
        System.out.println("Session ended");
    }

    public boolean isRunning() {
        return running;
    }
}
