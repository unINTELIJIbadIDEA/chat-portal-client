package com.project.client;

import com.project.models.message.Message;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.function.Consumer;

public class ClientMessageReceiver implements Runnable {
    private final ClientConnection connection;
    private volatile boolean running = true;
    private Consumer<Message> messageHandler;

    public ClientMessageReceiver(ClientConnection connection) {
        this.connection = connection;
    }

    public void setMessageHandler(Consumer<Message> messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream input = connection.getInputStream();
            while (running && connection.isConnected()) {
                try {
                    Message message = (Message) input.readObject();

                    if(messageHandler != null)
                        messageHandler.accept(message);

                } catch (IOException | ClassNotFoundException e) {
                    System.out.println("Błąd odbierania wiadomości lub połączenie zakończone.");
                    break;
                }
            }
        } finally {
            System.out.println("Wątek odbioru zakończył działanie.");
        }
    }

    public void stop() {
        running = false;
    }
}
