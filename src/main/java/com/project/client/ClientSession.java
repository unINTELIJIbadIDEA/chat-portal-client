package com.project.client;

import com.project.server.ServerProperties;
import com.project.utils.Message;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientSession {

    private static int MESSAGE_ID = 1;
    private static int SENDER_ID = 1;

    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;
    private final ExecutorService executor;
    private volatile boolean running = true;

    private final int senderId;
    private final String chatId;

    public ClientSession(String chatId) {
        this.executor = Executors.newSingleThreadExecutor();
        this.senderId = SENDER_ID++;
        this.chatId = chatId;
    }

    public void startSession() {

        try {
            socket = new Socket(ServerProperties.HOST, ServerProperties.PORT);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            if (chatId != null) {
                sendMessage("/join " + chatId);
            }

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
                if (messageContent.equalsIgnoreCase("exit")) {
                    endSession();
                }
                sendMessage(messageContent);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private void sendMessage(String messageContent) throws IOException {
        Message message = new Message(MESSAGE_ID++, chatId, senderId, messageContent, LocalDateTime.now());
        out.writeObject(message);
        out.flush();
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

        ClientSession client = new ClientSession("cosik");
        client.startSession();

    }

}
