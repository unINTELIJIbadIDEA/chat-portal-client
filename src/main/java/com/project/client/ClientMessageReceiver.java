package com.project.client;

import com.project.models.message.Message;
import java.io.IOException;
import java.io.ObjectInputStream;

public class ClientMessageReceiver implements Runnable {
    private final ClientConnection connection;
    private volatile boolean running = true;

    public ClientMessageReceiver(ClientConnection connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        try {
            ObjectInputStream input = connection.getInputStream();
            while (running && connection.isConnected()) {
                try {
                    Message message = (Message) input.readObject();
                    System.out.println(message);
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
