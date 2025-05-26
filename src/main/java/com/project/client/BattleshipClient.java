package com.project.client;

import com.project.models.battleship.messages.*;
import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class BattleshipClient {
    private final String host;
    private final int port;
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private volatile boolean running = false;
    private Thread receiverThread;

    // Listenery dla różnych eventów
    private Consumer<String> gameStateListener;
    private Consumer<GameUpdateMessage> gameUpdateListener;
    private Consumer<ShotResultMessage> shotResultListener;

    public BattleshipClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());

        running = true;
        startReceiving();
    }

    public void disconnect() {
        running = false;
        try {
            if (receiverThread != null) {
                receiverThread.interrupt();
            }
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();

            System.out.println("Disconnected from battleship server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(BattleshipMessage message) {
        try {
            if (out != null) {
                out.writeObject(message);
                out.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void startReceiving() {
        receiverThread = new Thread(() -> {
            while (running && socket != null && socket.isConnected()) {
                try {
                    BattleshipMessage message = (BattleshipMessage) in.readObject();
                    handleMessage(message);
                } catch (IOException | ClassNotFoundException e) {
                    if (running) {
                        System.err.println("Error receiving battleship message: " + e.getMessage());
                    }
                    break;
                }
            }
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void handleMessage(BattleshipMessage message) {
        switch (message.getType()) {
            case GAME_UPDATE:
                GameUpdateMessage gameUpdate = (GameUpdateMessage) message;
                if (gameUpdateListener != null) {
                    gameUpdateListener.accept(gameUpdate);
                }
                // Powiadom o zmianie stanu gry
                if (gameStateListener != null) {
                    gameStateListener.accept(gameUpdate.getGame().getState().toString());
                }
                break;

            case SHOT_RESULT:
                ShotResultMessage shotResult = (ShotResultMessage) message;
                if (shotResultListener != null) {
                    shotResultListener.accept(shotResult);
                }
                break;

            case ERROR:
                System.err.println("Błąd serwera battleship: " + message);
                break;

            default:
                System.out.println("Nieznany typ wiadomości battleship: " + message.getType());
                break;
        }
    }

    // Settery dla listenerów
    public void setGameStateListener(Consumer<String> listener) {
        this.gameStateListener = listener;
    }

    public void setGameUpdateListener(Consumer<GameUpdateMessage> listener) {
        this.gameUpdateListener = listener;
    }

    public void setShotResultListener(Consumer<ShotResultMessage> listener) {
        this.shotResultListener = listener;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }

    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::disconnect));
    }

}