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
    private JoinGameMessage lastJoinMessage;

    // Listenery dla różnych eventów
    private Consumer<String> gameStateListener;
    private Consumer<GameUpdateMessage> gameUpdateListener;
    private Consumer<ShotResultMessage> shotResultListener;

    public BattleshipClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        try {
            System.out.println("[BATTLESHIP CLIENT]: Connecting to " + host + ":" + port);
            socket = new Socket(host, port);

            // DODAJ: Ustaw timeout dla socket
            socket.setSoTimeout(30000); // 30 sekund timeout
            socket.setKeepAlive(true);   // Włącz keep-alive

            System.out.println("[BATTLESHIP CLIENT]: Socket connected");

            System.out.println("[BATTLESHIP CLIENT]: Initializing output stream...");
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            System.out.println("[BATTLESHIP CLIENT]: Output stream ready");

            // Krótsze opóźnienie
            Thread.sleep(100);

            System.out.println("[BATTLESHIP CLIENT]: Initializing input stream...");
            in = new ObjectInputStream(socket.getInputStream());
            System.out.println("[BATTLESHIP CLIENT]: Input stream ready");

            running = true;
            startReceiving();

            System.out.println("[BATTLESHIP CLIENT]: Successfully connected to battleship server");

        } catch (Exception e) {
            System.err.println("[BATTLESHIP CLIENT]: Connection failed: " + e.getMessage());
            e.printStackTrace();
            throw new IOException("Failed to connect to battleship server", e);
        }
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

            System.out.println("[BATTLESHIP CLIENT]: Disconnected from battleship server");
        } catch (IOException e) {
            System.err.println("[BATTLESHIP CLIENT]: Error during disconnect: " + e.getMessage());
        }
    }

    public void sendMessage(BattleshipMessage message) {
        try {
            if (out != null && running && socket != null && socket.isConnected()) {
                System.out.println("[BATTLESHIP CLIENT]: Sending message: " + message.getType());

                // ZAPISZ JOIN_GAME message do ponownego wysłania
                if (message instanceof JoinGameMessage) {
                    lastJoinMessage = (JoinGameMessage) message;
                }

                out.writeObject(message);
                out.flush();
                System.out.println("[BATTLESHIP CLIENT]: Message sent successfully");
            } else {
                System.err.println("[BATTLESHIP CLIENT]: Cannot send message - not connected");
                // PRÓBUJ ponowne połączenie
                if (running) {
                    attemptReconnection();
                }
            }
        } catch (IOException e) {
            System.err.println("[BATTLESHIP CLIENT]: Error sending message: " + e.getMessage());
            if (running) {
                attemptReconnection();
            }
        }
    }

    private void startReceiving() {
        receiverThread = new Thread(() -> {
            while (running && socket != null && socket.isConnected() && !socket.isClosed()) {
                try {
                    System.out.println("[BATTLESHIP CLIENT]: Waiting for message from server...");
                    BattleshipMessage message = (BattleshipMessage) in.readObject();
                    System.out.println("[BATTLESHIP CLIENT]: Received message: " + message.getType());
                    handleMessage(message);
                } catch (java.net.SocketTimeoutException e) {
                    // To jest normalne - timeout oznacza brak wiadomości
                    System.out.println("[BATTLESHIP CLIENT]: No message received (timeout)");
                    continue;
                } catch (IOException | ClassNotFoundException e) {
                    if (running) {
                        System.err.println("[BATTLESHIP CLIENT]: Error receiving message: " + e.getMessage());
                        if (e instanceof StreamCorruptedException) {
                            System.err.println("[BATTLESHIP CLIENT]: Stream corrupted - this may be caused by proxy/tunnel");
                        }
                        // DODAJ: Próba ponownego połączenia
                        attemptReconnection();
                    }
                    break;
                }
            }
            System.out.println("[BATTLESHIP CLIENT]: Receiver thread ended");
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void attemptReconnection() {
        if (!running) return;

        System.out.println("[BATTLESHIP CLIENT]: Attempting to reconnect...");
        try {
            disconnect();
            Thread.sleep(2000);
            connect();

            // Wyślij ponownie JOIN_GAME jeśli mamy dane
            if (lastJoinMessage != null) {
                sendMessage(lastJoinMessage);
            }

        } catch (Exception e) {
            System.err.println("[BATTLESHIP CLIENT]: Reconnection failed: " + e.getMessage());
            running = false;
        }
    }

    private void handleMessage(BattleshipMessage message) {
        System.out.println("[BATTLESHIP CLIENT]: ===== HANDLING MESSAGE =====");
        System.out.println("[BATTLESHIP CLIENT]: Message type: " + message.getType());

        switch (message.getType()) {
            case GAME_UPDATE:
                GameUpdateMessage gameUpdate = (GameUpdateMessage) message;
                System.out.println("[BATTLESHIP CLIENT]: Game state: " + gameUpdate.getGame().getState());
                System.out.println("[BATTLESHIP CLIENT]: Players: " + gameUpdate.getGame().getPlayerBoards().keySet());
                System.out.println("[BATTLESHIP CLIENT]: Players ready: " + gameUpdate.getGame().getPlayersReady());

                if (gameUpdateListener != null) {
                    System.out.println("[BATTLESHIP CLIENT]: Calling gameUpdateListener...");
                    gameUpdateListener.accept(gameUpdate);
                } else {
                    System.err.println("[BATTLESHIP CLIENT]: gameUpdateListener is NULL!");
                }

                // Powiadom o zmianie stanu gry
                if (gameStateListener != null) {
                    System.out.println("[BATTLESHIP CLIENT]: Calling gameStateListener with state: " + gameUpdate.getGame().getState());
                    gameStateListener.accept(gameUpdate.getGame().getState().toString());
                } else {
                    System.err.println("[BATTLESHIP CLIENT]: gameStateListener is NULL!");
                }
                break;

            case SHOT_RESULT:
                ShotResultMessage shotResult = (ShotResultMessage) message;
                System.out.println("[BATTLESHIP CLIENT]: Shot result: " + shotResult.getResult());
                if (shotResultListener != null) {
                    shotResultListener.accept(shotResult);
                }
                break;

            case ERROR:
                System.err.println("[BATTLESHIP CLIENT]: Server error: " + message);
                break;

            default:
                System.out.println("[BATTLESHIP CLIENT]: Unknown message type: " + message.getType());
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

    public void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::disconnect));
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && running;
    }
}