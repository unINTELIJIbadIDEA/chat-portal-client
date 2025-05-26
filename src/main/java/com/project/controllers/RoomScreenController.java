package com.project.controllers;

import com.project.client.BattleshipClient;
import com.project.models.battleship.messages.JoinGameMessage;
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
        new Thread(() -> connectToBattleshipServer("localhost", battleshipPort)).start();

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

    private void connectToBattleshipServer(String host, int port) {
        try {
            battleshipClient = new BattleshipClient(host, port);
            battleshipClient.setGameStateListener(this::onGameStateChanged);
            battleshipClient.connect();

            // DODAJ TU - shutdown hook
            battleshipClient.addShutdownHook();

            // Dołącz do gry
            battleshipClient.sendMessage(new JoinGameMessage(playerId, gameId, chatId));

            System.out.println("Connected to battleship server: " + host + ":" + port);

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
        Platform.runLater(() -> {
            if (waitingLabel == null) return;

            switch (newState) {
                case "WAITING_FOR_PLAYERS":
                    waitingLabel.setText("Oczekiwanie na drugiego gracza...");
                    break;
                case "SHIP_PLACEMENT":
                    waitingLabel.setText("Drugi gracz dołączył! Przygotowanie do gry...");
                    // Po krótkim czasie przejdź do ustawiania statków
                    new Thread(() -> {
                        try {
                            Thread.sleep(2000);
                            Platform.runLater(this::openShipPlacementWindow);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                    break;
                case "PLAYING":
                    openGameWindow();
                    break;
                case "FINISHED":
                    waitingLabel.setText("Gra zakończona!");
                    break;
            }
        });
    }

    private void openShipPlacementWindow() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project/screenchoose.fxml"));
            Parent root = loader.load();

            ScreenChooseController controller = loader.getController();
            if (controller != null) {
                controller.initializeShipPlacement(gameId, playerId, battleshipClient);
            }

            Stage stage = (Stage) waitingLabel.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Ustawianie statków - " + gameId);

        } catch (IOException e) {
            e.printStackTrace();
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
}