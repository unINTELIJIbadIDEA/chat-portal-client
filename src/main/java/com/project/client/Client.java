package com.project.client;

import com.project.utils.Message;

import java.io.*;
import java.net.Socket;
import java.time.LocalDate;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    private int port;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    public Client(int port) {

        this.port = port;
    }

    public void startSession() {

        try {
            socket = new Socket("localhost", port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            executor.submit(receiveMessages());
            sendMessages();



        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    private void sendMessages() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                String messageContent = scanner.nextLine();
                Message message = new Message(1, 0,1, messageContent, LocalDate.now());
                if (messageContent.equalsIgnoreCase("exit")) {
                    endSession();
                    break;
                }
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }



    private Callable<Void> receiveMessages() {
        return () -> {
            try {
                Message message;
                while (running && (message = (Message) in.readObject()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.out.println("Connection closed.");
                running = false;
            }
            return null;
        };
    }

    public void endSession() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            System.out.println("Client disconnected.");
        } catch (IOException e) {
            System.out.println("Error closing client: " + e.getMessage());
        } finally {
            executor.shutdown();
        }
    }


    public static void main(String[] args) {

        Client client = new Client(2137);
        client.startSession();

    }

}
