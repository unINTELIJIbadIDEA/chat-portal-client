package com.project.client;

import com.project.models.message.Message;

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
            connection.connect();
            running = true;
            sender = new ClientMessageSender(connection, chatId);
            ClientMessageReceiver receiver = new ClientMessageReceiver(connection);
            receiver.setMessageHandler(messageConsumer); // Dodano
            executor.execute(receiver);

            if (chatId != null) {
                sender.sendMessage("/join " + chatId, token);
            }
        } catch (IOException e) {
            System.out.println("Błąd połączenia: " + e.getMessage());
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