package com.project.client;

import com.project.models.Message;
import java.io.IOException;
import java.io.ObjectInputStream;


//Z tym będzie trzeba się próbować łączyć poprzez frontend. Nie wiem jak zrobić łatwiejszy dostęp (może coś jeszcze wymyślę)
public class ClientMessageReceiver implements Runnable {
    private final ClientConnection connection;
    private volatile boolean running = true;

    public ClientMessageReceiver(ClientConnection connection) {
        this.connection = connection;
    }

    @Override
    public void run() {
        try {
            while (running && connection.isConnected()) {
                ObjectInputStream input = connection.getInputStream();
                Message message = (Message) input.readObject();
                System.out.println(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            if (running) System.out.println("Connection closed");
        }
    }

    public void stop() {
        running = false;
    }
}