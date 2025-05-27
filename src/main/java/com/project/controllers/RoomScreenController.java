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

    public void initializeGame(String gameId, String chatId, int battleshipPort, String token) {
        this.gameId = gameId;
        this.chatId = chatId;
        this.playerId = extractCurrentUserId();

        Platform.runLater(() -> {
            if (waitingLabel != null) {
                waitingLabel.setText("Oczekiwanie na drugiego gracza...");
            }
        });

        // Połącz z serwerem battleship w osobnym wątku
        new Thread(() -> connectToBattleshipServer()).start();

        // DODAJ TU - obsługa zamykania okna
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

            battleshipClient = new BattleshipClient(battleshipHost, battleshipPort);

            // USTAW LISTENERY PRZED POŁĄCZENIEM
            battleshipClient.setGameStateListener(this::onGameStateChanged);
            battleshipClient.setGameUpdateListener(this::onGameUpdate); // DODAJ TO

            battleshipClient.connect();
            battleshipClient.addShutdownHook();

            // Dołącz do gry
            battleshipClient.sendMessage(new JoinGameMessage(playerId, gameId, chatId));

            System.out.println("Connected to battleship server: " + battleshipHost + ":" + battleshipPort);

        } catch (IOException e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                if (waitingLabel != null) {
                    waitingLabel.setText("Błąd połączenia z serwerem gry");
                }
            });
        }
    }

    private void onGameStateChanged(String newState) {
        System.out.println("[ROOM CONTROLLER]: ===== GAME STATE CHANGED =====");
        System.out.println("[ROOM CONTROLLER]: New state: " + newState);

        Platform.runLater(() -> {
            if (waitingLabel == null) {
                System.err.println("[ROOM CONTROLLER]: waitingLabel is null!");
                return;
            }

            switch (newState) {
                case "WAITING_FOR_PLAYERS":
                    System.out.println("[ROOM CONTROLLER]: Setting waiting message...");
                    waitingLabel.setText("Oczekiwanie na drugiego gracza...");
                    break;
                case "SHIP_PLACEMENT":
                    System.out.println("[ROOM CONTROLLER]: Second player joined! Starting ship placement...");
                    waitingLabel.setText("Drugi gracz dołączył! Przygotowanie do gry...");

                    // Krótkie opóźnienie przed przejściem
                    new Thread(() -> {
                        try {
                            System.out.println("[ROOM CONTROLLER]: Waiting 2 seconds before opening ship placement...");
                            Thread.sleep(2000);
                            System.out.println("[ROOM CONTROLLER]: Opening ship placement window...");
                            Platform.runLater(this::openShipPlacementWindow);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                    break;
                case "PLAYING":
                    System.out.println("[ROOM CONTROLLER]: Game started! Opening game window...");
                    openGameWindow();
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
    private void openShipPlacementWindow() {
        try {
            System.out.println("[ROOM CONTROLLER]: Opening ship placement window...");

            // SPRAWDŹ CZY KOMPONENTY SĄ DOSTĘPNE
            if (waitingLabel == null) {
                System.err.println("[ROOM CONTROLLER]: waitingLabel is null - using alternative method");
                // Alternatywne rozwiązanie bez waitingLabel
                openShipPlacementDirectly();
                return;
            }

            Stage currentStage = (Stage) waitingLabel.getScene().getWindow();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project/screenchoose.fxml"));
            Scene scene = new Scene(loader.load());

            ScreenChooseController controller = loader.getController();
            controller.initializeShipPlacement(gameId, playerId, battleshipClient);

            currentStage.setScene(scene);
            currentStage.setTitle("Gra w statki - Ustawianie statków");

            System.out.println("Initialized ship placement for game: " + gameId + ", player: " + playerId);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ROOM CONTROLLER]: Error opening ship placement window: " + e.getMessage());
            // Fallback - spróbuj otworzyć bezpośrednio
            openShipPlacementDirectly();
        }
    }

    private void openShipPlacementDirectly() {
        try {
            System.out.println("[ROOM CONTROLLER]: Opening ship placement directly...");

            // Znajdź aktywne okno aplikacji
            Stage currentStage = null;
            for (Stage stage : Stage.getWindows().stream()
                    .filter(window -> window instanceof Stage)
                    .map(window -> (Stage) window)
                    .filter(Stage::isShowing)
                    .toList()) {
                currentStage = stage;
                break;
            }

            if (currentStage == null) {
                System.err.println("[ROOM CONTROLLER]: No active stage found!");
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project/screenchoose.fxml"));
            Scene scene = new Scene(loader.load());

            ScreenChooseController controller = loader.getController();
            controller.initializeShipPlacement(gameId, playerId, battleshipClient);

            currentStage.setScene(scene);
            currentStage.setTitle("Gra w statki - Ustawianie statków");

            System.out.println("[ROOM CONTROLLER]: Ship placement opened successfully via direct method");

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[ROOM CONTROLLER]: Error in direct ship placement opening: " + e.getMessage());
        }
    }

    private void openGameWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project/screenship.fxml"));
            Parent root = loader.load();

            ScreenShipController controller = loader.getController();
            if (controller != null) {
                controller.initializeGame(gameId, playerId, battleshipClient);
            }

            Stage stage = (Stage) waitingLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Gra w statki - " + gameId);

        } catch (IOException e) {
            e.printStackTrace();
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
                System.out.println(e.getMessage());
            }
        }
        return -1;
    }

    private void onGameUpdate(GameUpdateMessage gameUpdate) {
        System.out.println("[ROOM CONTROLLER]: ===== GAME UPDATE RECEIVED =====");
        System.out.println("[ROOM CONTROLLER]: Game state: " + gameUpdate.getGame().getState());
        System.out.println("[ROOM CONTROLLER]: Players: " + gameUpdate.getGame().getPlayerBoards().keySet());

        Platform.runLater(() -> {
            // Bezpieczna aktualizacja UI
            if (waitingLabel != null) {
                switch (gameUpdate.getGame().getState()) {
                    case WAITING_FOR_PLAYERS:
                        waitingLabel.setText("Oczekiwanie na drugiego gracza...");
                        break;
                    case SHIP_PLACEMENT:
                        waitingLabel.setText("Drugi gracz dołączył! Przygotowanie do gry...");
                        break;
                    case PLAYING:
                        waitingLabel.setText("Gra rozpoczęta!");
                        break;
                    case FINISHED:
                        waitingLabel.setText("Gra zakończona!");
                        break;
                }
            } else {
                System.out.println("[ROOM CONTROLLER]: waitingLabel is null, skipping UI update");
            }

            // Automatyczne przejście do ship placement
            if (gameUpdate.getGame().getState() == GameState.SHIP_PLACEMENT) {
                new Thread(() -> {
                    try {
                        System.out.println("[ROOM CONTROLLER]: Game update - waiting 1 second before ship placement...");
                        Thread.sleep(1000); // Skróć czas oczekiwania
                        System.out.println("[ROOM CONTROLLER]: Game update - opening ship placement window...");
                        Platform.runLater(this::openShipPlacementWindow);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        });
    }

}