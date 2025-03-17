package com.project.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Client {

    private int port;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean running = true;

    public Client(int port) {

        this.port = port;
    }

    public void startSession() {

        try (Socket tempSocket = new Socket("localhost", port)){

            socket = new Socket("localhost", port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            executor.submit(receiveMessages());
            sendMessages();



        } catch (IOException e) {
            System.out.println(e.getMessage());
        }

    }

    private void sendMessages() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (running) {
                String message = scanner.nextLine();
                if (message.equalsIgnoreCase("exit")) {
                    endSession();
                    break;
                }
                out.println(message);
            }
        }
    }

    private Callable<Void> receiveMessages() {
        return () -> {
            try {
                String message;
                while (running && (message = in.readLine()) != null) {
                    System.out.println(message);
                }
            } catch (IOException e) {
                System.out.println("Connection closed.");
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
            executor.shutdownNow();
            System.out.println("Client disconnected.");
        } catch (IOException e) {
            System.out.println("Error closing client: " + e.getMessage());
        }
    }


    public static void main(String[] args) {

        Client client = new Client(2137);
        client.startSession();

    }

}
