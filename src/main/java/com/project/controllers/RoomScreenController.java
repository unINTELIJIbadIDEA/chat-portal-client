package com.project.controllers;

import com.project.client.BattleshipClient;
import com.project.models.battleship.GameState;
import com.project.models.battleship.messages.GameUpdateMessage;
import com.project.models.battleship.messages.JoinGameMessage;
import com.project.utils.Config;
import com.project.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.IOException;

public class RoomScreenController {
    @FXML
    private Label waitingLabel;

    @FXML
    private ImageView loadingIcon;

    private String gameId;
    private String chatId;
    private int playerId;
    private BattleshipClient battleshipClient;

    // KRYTYCZNE: Flagi do kontroli otwierania okien
    private volatile boolean shipPlacementOpened = false;
    private volatile boolean gameWindowOpened = false;

    public void initializeGame(String gameId, String chatId, int battleshipPort, String token) {
        this.gameId = gameId;
        this.chatId = chatId;
        this.playerId = extractCurrentUserId();

        System.out.println("[ROOM CONTROLLER]: Initializing game - ID: " + gameId + ", Player: " + playerId);

        Platform.runLater(() -> {
            if (waitingLabel != null) {
                waitingLabel.setText("Łączenie z serwerem gry...");
            }
        });

        // Połącz z serwerem battleship w osobnym wątku
        new Thread(this::connectToBattleshipServer).start();

        // Obsługa zamykania okna
        Platform.runLater(() -> {
            if (waitingLabel != null && waitingLabel.getScene() != null) {
                waitingLabel.getScene().getWindow().setOnCloseRequest(event -> {
                    if (battleshipClient != null) {
                        battleshipClient.disconnect();
                    }
                });
            }
        });
    }

    private void connectToBattleshipServer() {
        try {
            String battleshipHost = Config.getHOST_SERVER();
            int battleshipPort = Config.getBATTLESHIP_PORT();

            System.out.println("[ROOM CONTROLLER]: Connecting to battleship server: " + battleshipHost + ":" + battleshipPort);

            battleshipClient = new BattleshipClient(battleshipHost, battleshipPort);

            // KRYTYCZNE: Ustaw listenery PRZED połączeniem
            battleshipClient.setGameStateListener(this::onGameStateChanged);
            battleshipClient.setGameUpdateListener(this::onGameUpdate);
            System.out.println("[ROOM CONTROLLER]: Listeners set before connection");

            battleshipClient.connect();
            battleshipClient.addShutdownHook();

            Platform.runLater(() -> {
                if (waitingLabel != null) {
                    waitingLabel.setText("Dołączanie do gry...");
                }
            });

            // Opóźnienie przed wysłaniem JOIN_GAME
            Thread.sleep(1000);

            // Dołącz do gry
            JoinGameMessage joinMessage = new JoinGameMessage(playerId, gameId, chatId);
            battleshipClient.sendMessage(joinMessage);

            System.out.println("[ROOM CONTROLLER]: Connected to battleship server successfully");

            // Uruchom monitor połączenia
            startConnectionMonitor();

        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                if (waitingLabel != null) {
                    waitingLabel.setText("Błąd połączenia z serwerem gry: " + e.getMessage());
                }
            });
        }
    }

    private void startConnectionMonitoring() {
        Thread monitorThread = new Thread(() -> {
            while (battleshipClient != null && battleshipClient.isConnected()) {
                try {
                    Thread.sleep(5000); // Sprawdzaj co 5 sekund

                    if (battleshipClient != null && !battleshipClient.isConnected()) {
                        System.err.println("[ROOM CONTROLLER]: Connection lost! Status will be updated.");

                        Platform.runLater(() -> {
                            if (waitingLabel != null) {
                                waitingLabel.setText("Połączenie przerwane. Ponowne łączenie...");
                            }
                        });

                        // Próbuj ponownie połączyć
                        connectToBattleshipServer();
                        break;
                    }

                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void startConnectionMonitor() {
        Thread monitorThread = new Thread(() -> {
            while (battleshipClient != null && battleshipClient.isConnected()) {
                try {
                    Thread.sleep(5000); // Sprawdzaj co 5 sekund
                    if (!battleshipClient.isConnected()) {
                        System.err.println("[ROOM CONTROLLER]: Connection lost! Attempting to reconnect...");
                        Platform.runLater(() -> {
                            if (waitingLabel != null) {
                                waitingLabel.setText("Połączenie utracone. Próba ponownego połączenia...");
                            }
                        });
                        reconnectToBattleshipServer();
                        break;
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    private void reconnectToBattleshipServer() {
        try {
            if (battleshipClient != null) {
                battleshipClient.disconnect();
            }
            Thread.sleep(2000); // Poczekaj 2 sekundy
            connectToBattleshipServer();
        } catch (Exception e) {
            System.err.println("[ROOM CONTROLLER]: Reconnection failed: " + e.getMessage());
            Platform.runLater(() -> {
                if (waitingLabel != null) {
                    waitingLabel.setText("Nie można ponownie połączyć z serwerem gry");
                }
            });
        }
    }

    private void onGameStateChanged(String newState) {
        System.out.println("[ROOM CONTROLLER]: ===== GAME STATE CHANGED =====");
        System.out.println("[ROOM CONTROLLER]: New state: " + newState + " for player: " + playerId);

        Platform.runLater(() -> {
            if (waitingLabel == null) {
                System.err.println("[ROOM CONTROLLER]: waitingLabel is null!");
                return;
            }

            switch (newState) {
                case "WAITING_FOR_PLAYERS":
                    System.out.println("[ROOM CONTROLLER]: Setting waiting message...");
                    waitingLabel.setText("Oczekiwanie na drugiego gracza...");
                    shipPlacementOpened = false;
                    gameWindowOpened = false;
                    break;

                case "SHIP_PLACEMENT":
                    System.out.println("[ROOM CONTROLLER]: ===== SHIP PLACEMENT STATE =====");
                    System.out.println("[ROOM CONTROLLER]: Player " + playerId + " received SHIP_PLACEMENT state");
                    handleShipPlacementState();
                    break;

                case "PLAYING":
                    handlePlayingState();
                    break;

                case "FINISHED":
                    System.out.println("[ROOM CONTROLLER]: Game finished!");
                    waitingLabel.setText("Gra zakończona!");
                    break;

                default:
                    System.out.println("[ROOM CONTROLLER]: Unknown state: " + newState);
                    break;
            }
        });
    }

    private void onGameUpdate(GameUpdateMessage gameUpdate) {
        System.out.println("[ROOM CONTROLLER]: ===== GAME UPDATE RECEIVED =====");
        System.out.println("[ROOM CONTROLLER]: Game state: " + gameUpdate.getGame().getState() + " for player: " + playerId);
        System.out.println("[ROOM CONTROLLER]: Players: " + gameUpdate.getGame().getPlayerBoards().keySet());
        System.out.println("[ROOM CONTROLLER]: Current player ID: " + playerId);
        System.out.println("[ROOM CONTROLLER]: Ship placement opened flag: " + shipPlacementOpened);

        Platform.runLater(() -> {
            if (waitingLabel != null) {
                GameState state = gameUpdate.getGame().getState();

                switch (state) {
                    case WAITING_FOR_PLAYERS:
                        waitingLabel.setText("Oczekiwanie na drugiego gracza...");
                        // Reset flags
                        shipPlacementOpened = false;
                        gameWindowOpened = false;
                        break;

                    case SHIP_PLACEMENT:
                        // KRYTYCZNE: Sprawdź czy okno nie zostało już otwarte
                        if (!shipPlacementOpened) {
                            System.out.println("[ROOM CONTROLLER]: Transitioning to SHIP_PLACEMENT state");
                            handleShipPlacementState();
                        } else {
                            System.out.println("[ROOM CONTROLLER]: Ship placement already handled, skipping");
                        }
                        break;

                    case PLAYING:
                        if (!gameWindowOpened) {
                            handlePlayingState();
                        }
                        break;

                    case FINISHED:
                        waitingLabel.setText("Gra zakończona!");
                        break;
                }
            }
        });
    }

    private void handleShipPlacementState() {
        System.out.println("[ROOM CONTROLLER]: Handling SHIP_PLACEMENT state - opened flag: " + shipPlacementOpened);

        if (waitingLabel != null) {
            waitingLabel.setText("Drugi gracz dołączył! Przygotowanie do gry...");
        }

        // KRYTYCZNE: Ustaw flagę PRZED otwarciem okna
        if (!shipPlacementOpened) {
            shipPlacementOpened = true;
            System.out.println("[ROOM CONTROLLER]: Opening ship placement window for player: " + playerId);

            // Użyj Platform.runLater dla bezpiecznego otwarcia okna
            Platform.runLater(() -> {
                try {
                    Thread.sleep(500); // Krótkie opóźnienie dla płynności
                    openShipPlacementWindow();
                } catch (Exception e) {
                    e.printStackTrace();
                    shipPlacementOpened = false; // Reset flagi w przypadku błędu
                }
            });
        }
    }

    private void handlePlayingState() {
        System.out.println("[ROOM CONTROLLER]: Handling PLAYING state - opened flag: " + gameWindowOpened);

        if (waitingLabel != null) {
            waitingLabel.setText("Gra rozpoczęta!");
        }

        if (!gameWindowOpened) {
            gameWindowOpened = true;
            System.out.println("[ROOM CONTROLLER]: Opening game window for player: " + playerId);
            openGameWindow();
        } else {
            System.out.println("[ROOM CONTROLLER]: Game window already opened, skipping...");
        }
    }

    private void openShipPlacementWindow() {
        try {
            System.out.println("[ROOM CONTROLLER]: Opening ship placement window for player: " + playerId);

            if (waitingLabel == null || waitingLabel.getScene() == null) {
                System.err.println("[ROOM CONTROLLER]: Cannot get current stage");
                return;
            }

            Stage currentStage = (Stage) waitingLabel.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project/screenchoose.fxml"));
            Scene scene = new Scene(loader.load());

            ScreenChooseController controller = loader.getController();
            if (controller != null) {
                controller.initializeShipPlacement(gameId, playerId, battleshipClient);
                System.out.println("[ROOM CONTROLLER]: Ship placement controller initialized");
            } else {
                System.err.println("[ROOM CONTROLLER]: Ship placement controller is null");
            }

            currentStage.setScene(scene);
            currentStage.setTitle("Gra w statki - Ustawianie statków (Gracz " + playerId + ")");

            System.out.println("[ROOM CONTROLLER]: Ship placement window opened successfully");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ROOM CONTROLLER]: Error opening ship placement window: " + e.getMessage());
        }
    }

    private void openGameWindow() {
        try {
            System.out.println("[ROOM CONTROLLER]: Opening game window for player: " + playerId);

            if (waitingLabel == null || waitingLabel.getScene() == null) {
                System.err.println("[ROOM CONTROLLER]: Cannot get current stage");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project/screenship.fxml"));
            Parent root = loader.load();

            ScreenShipController controller = loader.getController();
            if (controller != null) {
                controller.initializeGame(gameId, playerId, battleshipClient);
                System.out.println("[ROOM CONTROLLER]: Game controller initialized");
            }

            Stage stage = (Stage) waitingLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gra w statki - Rozgrywka (Gracz " + playerId + ")");

            System.out.println("[ROOM CONTROLLER]: Game window opened successfully");

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("[ROOM CONTROLLER]: Error opening game window: " + e.getMessage());
        }
    }

    private int extractCurrentUserId() {
        String token = SessionManager.getInstance().getToken();
        if (token != null && !token.isEmpty()) {
            try {
                String[] tokenParts = token.split("\\.");
                if (tokenParts.length >= 2) {
                    String payload = new String(java.util.Base64.getUrlDecoder().decode(tokenParts[1]));
                    com.google.gson.JsonObject jsonPayload = com.google.gson.JsonParser.parseString(payload).getAsJsonObject();
                    return jsonPayload.get("userId").getAsInt();
                }
            } catch (Exception e) {
                System.err.println("[ROOM CONTROLLER]: Error extracting user ID: " + e.getMessage());
            }
        }
        return -1;
    }
}