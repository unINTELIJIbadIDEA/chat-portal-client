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
    private Thread heartbeatThread;
    private JoinGameMessage lastJoinMessage;
    private Consumer<ShipSunkMessage> shipSunkListener;

    // Listenery dla różnych eventów
    private Consumer<String> gameStateListener;
    private Consumer<GameUpdateMessage> gameUpdateListener;
    private Consumer<ShotResultMessage> shotResultListener;

    private String lastGameId;
    private int lastPlayerId;
    private String lastChatId;

    // Heartbeat
    private volatile long lastMessageTime = System.currentTimeMillis();
    private static final long HEARTBEAT_INTERVAL = 10000; // 10 sekund
    private static final long CONNECTION_TIMEOUT = 30000; // 30 sekund

    public BattleshipClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void connect() throws IOException {
        disconnect(); // Zamknij poprzednie połączenie jeśli istnieje

        int maxRetries = 3;
        int retryCount = 0;

        while (retryCount < maxRetries) {
            try {
                System.out.println("[BATTLESHIP CLIENT]: Connection attempt " + (retryCount + 1) + "/" + maxRetries);

                socket = new Socket();
                socket.setSoTimeout(5000); // 5s timeout dla connect
                socket.connect(new java.net.InetSocketAddress(host, port), 10000);

                // Konfiguracja socket
                socket.setKeepAlive(true);
                socket.setTcpNoDelay(true);
                socket.setSoTimeout(15000); // 15s timeout dla operacji

                System.out.println("[BATTLESHIP CLIENT]: Socket connected to " + host + ":" + port);

                // Inicjalizacja streamów
                out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();

                Thread.sleep(500); // Dłuższe opóźnienie

                in = new ObjectInputStream(socket.getInputStream());

                running = true;
                lastMessageTime = System.currentTimeMillis();

                startReceiving();
                startHeartbeat();

                System.out.println("[BATTLESHIP CLIENT]: Successfully connected to battleship server");
                return;

            } catch (Exception e) {
                retryCount++;
                System.err.println("[BATTLESHIP CLIENT]: Connection attempt " + retryCount + " failed: " + e.getMessage());

                if (retryCount < maxRetries) {
                    try {
                        Thread.sleep(2000 * retryCount); // Zwiększaj opóźnienie
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Connection interrupted");
                    }
                }
            }
        }

        throw new IOException("Failed to connect after " + maxRetries + " attempts");
    }

    public void disconnect() {
        running = false;

        try {
            if (receiverThread != null) {
                receiverThread.interrupt();
            }
            if (heartbeatThread != null) {
                heartbeatThread.interrupt();
            }
            if (out != null) out.close();
            if (in != null) in.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.err.println("[BATTLESHIP CLIENT]: Error during disconnect: " + e.getMessage());
        }

        System.out.println("[BATTLESHIP CLIENT]: Disconnected from battleship server");
    }

    public void sendMessage(BattleshipMessage message) {
        if (!isConnected()) {
            System.err.println("[BATTLESHIP CLIENT]: Cannot send message - not connected");
            if (running) {
                attemptReconnection();
            }
            return;
        }

        try {
            System.out.println("[BATTLESHIP CLIENT]: Sending message: " + message.getType());

            // Zapisz dane do ponownego połączenia
            if (message instanceof JoinGameMessage) {
                JoinGameMessage joinMsg = (JoinGameMessage) message;
                lastGameId = joinMsg.getGameId();
                lastPlayerId = joinMsg.getPlayerId();
                lastChatId = joinMsg.getChatId();
            }

            synchronized (out) { // Synchronizacja dostępu do out
                out.writeObject(message);
                out.flush();
            }

            System.out.println("[BATTLESHIP CLIENT]: Message sent successfully");

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

                    // WAŻNE: Obsłuż wiadomość natychmiast w tym wątku
                    handleMessage(message);

                } catch (java.net.SocketTimeoutException e) {
                    // To jest normalne - timeout oznacza brak wiadomości
                    System.out.println("[BATTLESHIP CLIENT]: No message received (timeout)");
                    continue;
                } catch (IOException e) {
                    if (running) {
                        System.err.println("[BATTLESHIP CLIENT]: Error receiving message: " + e.getMessage());
                        // Spróbuj ponownie połączyć tylko jeśli to nie jest zamknięcie
                        if (!(e instanceof EOFException)) {
                            attemptReconnection();
                        }
                    }
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.println("[BATTLESHIP CLIENT]: Unknown message class: " + e.getMessage());
                }
            }
            System.out.println("[BATTLESHIP CLIENT]: Receiver thread ended");
        });
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void startStateChecker() {
        Thread stateChecker = new Thread(() -> {
            int checkCount = 0;
            while (running && checkCount < 10) { // Sprawdź max 10 razy
                try {
                    Thread.sleep(2000); // Co 2 sekundy
                    if (isConnected() && lastJoinMessage != null) {
                        System.out.println("[BATTLESHIP CLIENT]: Periodic state check #" + (checkCount + 1));
                        forceGameStateRefresh();
                    }
                    checkCount++;
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        stateChecker.setDaemon(true);
        stateChecker.start();
    }

    public void forceGameStateRefresh() {
        if (lastJoinMessage != null && isConnected()) {
            System.out.println("[BATTLESHIP CLIENT]: Forcing game state refresh...");
            sendMessage(lastJoinMessage);
        }
    }


    private void startHeartbeat() {
        heartbeatThread = new Thread(() -> {
            System.out.println("[BATTLESHIP CLIENT]: Heartbeat thread started");

            while (running) {
                try {
                    Thread.sleep(HEARTBEAT_INTERVAL);

                    if (running && isConnected()) {
                        // Wyślij heartbeat - może być dowolną wiadomością
                        if (lastGameId != null && lastPlayerId != 0) {
                            sendMessage(new JoinGameMessage(lastPlayerId, lastGameId, lastChatId));
                        }
                    }

                } catch (InterruptedException e) {
                    break;
                } catch (Exception e) {
                    System.err.println("[BATTLESHIP CLIENT]: Heartbeat error: " + e.getMessage());
                }
            }

            System.out.println("[BATTLESHIP CLIENT]: Heartbeat thread ended");
        });
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
    }

    private void attemptReconnection() {
        if (!running) return;

        System.out.println("[BATTLESHIP CLIENT]: Attempting reconnection...");

        try {
            disconnect();
            Thread.sleep(3000); // Poczekaj 3 sekundy
            connect();

            // Wyślij ponownie JOIN_GAME
            if (lastGameId != null && lastPlayerId != 0) {
                Thread.sleep(1000); // Poczekaj na ustabilizowanie połączenia
                sendMessage(new JoinGameMessage(lastPlayerId, lastGameId, lastChatId));
            }

            System.out.println("[BATTLESHIP CLIENT]: Reconnection successful");

        } catch (Exception e) {
            System.err.println("[BATTLESHIP CLIENT]: Reconnection failed: " + e.getMessage());
            // Próbuj ponownie za 5 sekund
            new Thread(() -> {
                try {
                    Thread.sleep(5000);
                    if (running) {
                        attemptReconnection();
                    }
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private void handleMessage(BattleshipMessage message) {
        System.out.println("[BATTLESHIP CLIENT]: ===== HANDLING MESSAGE =====");
        System.out.println("[BATTLESHIP CLIENT]: Message type: " + message.getType());
        System.out.println("[BATTLESHIP CLIENT]: Running: " + running);
        System.out.println("[BATTLESHIP CLIENT]: Connected: " + isConnected());

        switch (message.getType()) {
            case GAME_UPDATE:
                GameUpdateMessage gameUpdate = (GameUpdateMessage) message;
                System.out.println("[BATTLESHIP CLIENT]: Game state: " + gameUpdate.getGame().getState());
                System.out.println("[BATTLESHIP CLIENT]: Players: " + gameUpdate.getGame().getPlayerBoards().keySet());
                System.out.println("[BATTLESHIP CLIENT]: Players ready: " + gameUpdate.getGame().getPlayersReady());

                // KRYTYCZNE: Najpierw powiadom o zmianie stanu
                if (gameStateListener != null) {
                    String newState = gameUpdate.getGame().getState().toString();
                    System.out.println("[BATTLESHIP CLIENT]: Notifying state change to: " + newState);

                    // Wywołaj listener w osobnym wątku, aby nie blokować odbierania
                    new Thread(() -> {
                        try {
                            gameStateListener.accept(newState);
                        } catch (Exception e) {
                            System.err.println("[BATTLESHIP CLIENT]: Error in gameStateListener: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }).start();
                }

                // Potem wywołaj gameUpdateListener
                if (gameUpdateListener != null) {
                    System.out.println("[BATTLESHIP CLIENT]: Calling gameUpdateListener...");

                    new Thread(() -> {
                        try {
                            gameUpdateListener.accept(gameUpdate);
                        } catch (Exception e) {
                            System.err.println("[BATTLESHIP CLIENT]: Error in gameUpdateListener: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }).start();
                } else {
                    System.err.println("[BATTLESHIP CLIENT]: gameUpdateListener is NULL!");
                }
                break;

            case SHOT_RESULT:
                ShotResultMessage shotResult = (ShotResultMessage) message;
                System.out.println("[BATTLESHIP CLIENT]: Shot result: " + shotResult.getResult());
                if (shotResultListener != null) {
                    new Thread(() -> {
                        try {
                            shotResultListener.accept(shotResult);
                        } catch (Exception e) {
                            System.err.println("[BATTLESHIP CLIENT]: Error in shotResultListener: " + e.getMessage());
                        }
                    }).start();
                }
                break;

            case SHIP_SUNK:  // NOWY CASE
                ShipSunkMessage shipSunkMsg = (ShipSunkMessage) message;
                System.out.println("[BATTLESHIP CLIENT]: Ship sunk with " + shipSunkMsg.getShipPositions().size() + " positions");
                if (shipSunkListener != null) {
                    new Thread(() -> {
                        try {
                            shipSunkListener.accept(shipSunkMsg);
                        } catch (Exception e) {
                            System.err.println("[BATTLESHIP CLIENT]: Error in shipSunkListener: " + e.getMessage());
                        }
                    }).start();
                }
                break;

            case ERROR:
                System.err.println("[BATTLESHIP CLIENT]: Server error: " + message);
                break;

            default:
                System.out.println("[BATTLESHIP CLIENT]: Unknown message type: " + message.getType());
                break;
        }

        System.out.println("[BATTLESHIP CLIENT]: ===== MESSAGE HANDLING COMPLETE =====");
    }

    public void requestGameUpdate() {
        if (lastGameId != null && lastPlayerId != 0 && lastChatId != null && isConnected()) {
            System.out.println("[BATTLESHIP CLIENT]: Requesting game update...");
            JoinGameMessage requestUpdate = new JoinGameMessage(lastPlayerId, lastGameId, lastChatId);
            sendMessage(requestUpdate);
        } else if (lastJoinMessage != null && isConnected()) {
            System.out.println("[BATTLESHIP CLIENT]: Requesting game update using last join message...");
            sendMessage(lastJoinMessage);
        } else {
            System.err.println("[BATTLESHIP CLIENT]: Cannot request game update - no game info available");
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

    public void setShipSunkListener(Consumer<ShipSunkMessage> listener) {
        this.shipSunkListener = listener;
    }
}