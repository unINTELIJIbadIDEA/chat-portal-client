package com.project.controllers;

import com.project.client.BattleshipClient;
import com.project.models.battleship.ShotResult;
import com.project.models.battleship.messages.TakeShotMessage;
import com.project.models.battleship.messages.ShotResultMessage;
import com.project.models.battleship.messages.GameUpdateMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class ScreenShipController {
    @FXML
    private StackPane playerBoardPane;

    @FXML
    private StackPane enemyBoardPane;

    @FXML
    private StackPane root;

    @FXML
    private ImageView backgroundImage;

    private GridPane playerBoard;
    private GridPane enemyBoard;
    private BattleshipClient battleshipClient;
    private String gameId;
    private int playerId;
    private boolean isMyTurn = false;
    private Label statusLabel;

    @FXML
    public void initialize() {
        if (backgroundImage != null && root != null) {
            backgroundImage.fitWidthProperty().bind(root.widthProperty());
            backgroundImage.fitHeightProperty().bind(root.heightProperty());

            // DODAJ TU - obsługa zamykania okna
            Platform.runLater(() -> {
                if (root.getScene() != null) {
                    root.getScene().getWindow().setOnCloseRequest(event -> {
                        if (battleshipClient != null) {
                            battleshipClient.disconnect();
                        }
                    });
                }
            });
        }

        try {
            // Ładuj plansze
            FXMLLoader playerLoader = new FXMLLoader(getClass().getResource("/com/project/shipboard.fxml"));
            Node playerBoardNode = playerLoader.load();
            if (playerBoardNode instanceof GridPane) {
                playerBoard = (GridPane) playerBoardNode;
                playerBoardPane.getChildren().add(playerBoardNode);
            }

            FXMLLoader enemyLoader = new FXMLLoader(getClass().getResource("/com/project/battleboard.fxml"));
            Node enemyBoardNode = enemyLoader.load();
            if (enemyBoardNode instanceof GridPane) {
                enemyBoard = (GridPane) enemyBoardNode;
                setupEnemyBoardClicks();
                enemyBoardPane.getChildren().add(enemyBoardNode);
            }

            // Dodaj label statusu
            statusLabel = new Label("Oczekiwanie na rozpoczęcie gry...");
            statusLabel.getStyleClass().add("status-label");
            if (root != null) {
                root.getChildren().add(statusLabel);
                StackPane.setAlignment(statusLabel, javafx.geometry.Pos.TOP_CENTER);
                StackPane.setMargin(statusLabel, new javafx.geometry.Insets(20, 0, 0, 0));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeGame(String gameId, int playerId, BattleshipClient client) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.battleshipClient = client;

        // DODAJ TU - shutdown hook
        if (this.battleshipClient != null) {
            this.battleshipClient.addShutdownHook();
        }

        // Ustaw listenery
        battleshipClient.setShotResultListener(this::handleShotResult);
        battleshipClient.setGameUpdateListener(this::handleGameUpdate);

        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText("Gra rozpoczęta! Oczekiwanie na turę...");
            }
        });
    }

    private void setupEnemyBoardClicks() {
        if (enemyBoard == null) return;

        for (Node node : enemyBoard.getChildren()) {
            if (node instanceof javafx.scene.control.Button) {
                javafx.scene.control.Button cell = (javafx.scene.control.Button) node;
                cell.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY && isMyTurn) {
                        Integer col = GridPane.getColumnIndex(cell);
                        Integer row = GridPane.getRowIndex(cell);
                        if (col == null) col = 0;
                        if (row == null) row = 0;

                        // Pomijamy nagłówki (kolumna 0 i wiersz 0)
                        if (col > 0 && row > 0 && !cell.isDisabled()) {
                            takeShot(col - 1, row - 1); // Przesunięcie o nagłówki
                            cell.setDisable(true); // Zablokuj pole
                        }
                    }
                });
            }
        }
    }

    private void takeShot(int x, int y) {
        if (!isMyTurn) {
            showAlert("Nie twoja tura!", "Poczekaj na swoją kolej.");
            return;
        }

        if (battleshipClient != null) {
            battleshipClient.sendMessage(new TakeShotMessage(playerId, gameId, x, y));

            Platform.runLater(() -> {
                if (statusLabel != null) {
                    statusLabel.setText("Oczekiwanie na wynik strzału...");
                }
                isMyTurn = false;
            });
        }
    }

    private void handleShotResult(ShotResultMessage shotResult) {
        Platform.runLater(() -> {
            // Znajdź odpowiednią komórkę na planszy przeciwnika
            updateEnemyBoard(shotResult.getX(), shotResult.getY(), shotResult.getResult());

            // Aktualizuj status
            if (statusLabel != null) {
                switch (shotResult.getResult()) {
                    case HIT:
                        statusLabel.setText("Trafienie! Strzelaj ponownie!");
                        statusLabel.getStyleClass().removeAll("your-turn", "opponent-turn");
                        statusLabel.getStyleClass().add("your-turn");
                        isMyTurn = true; // Kolejny strzał
                        break;
                    case MISS:
                        statusLabel.setText("Pudło! Tura przeciwnika.");
                        statusLabel.getStyleClass().removeAll("your-turn", "opponent-turn");
                        statusLabel.getStyleClass().add("opponent-turn");
                        isMyTurn = false;
                        break;
                    case SUNK:
                        statusLabel.setText("Zatopiony! Strzelaj ponownie!");
                        statusLabel.getStyleClass().removeAll("your-turn", "opponent-turn");
                        statusLabel.getStyleClass().add("your-turn");
                        isMyTurn = true;
                        break;
                    case GAME_OVER:
                        if (shotResult.getShooterId() == playerId) {
                            showAlert("Zwycięstwo!", "Gratulacje! Wygrałeś grę!");
                        } else {
                            showAlert("Porażka", "Przeciwnik wygrał grę.");
                        }
                        statusLabel.setText("Gra zakończona!");
                        statusLabel.getStyleClass().removeAll("your-turn", "opponent-turn");
                        break;
                    case ALREADY_SHOT:
                        statusLabel.setText("To pole było już ostrzeliwane!");
                        isMyTurn = true;
                        break;
                    case INVALID:
                        statusLabel.setText("Nieprawidłowy strzał!");
                        isMyTurn = true;
                        break;
                }
            }
        });
    }

    private void handleGameUpdate(GameUpdateMessage gameUpdate) {
        Platform.runLater(() -> {
            // Aktualizuj stan gry
            int currentPlayerId = gameUpdate.getGame().getCurrentPlayer();
            updateGameState(currentPlayerId);
        });
    }

    private void updateEnemyBoard(int x, int y, ShotResult result) {
        if (enemyBoard == null) return;

        for (Node node : enemyBoard.getChildren()) {
            Integer col = GridPane.getColumnIndex(node);
            Integer row = GridPane.getRowIndex(node);
            if (col == null) col = 0;
            if (row == null) row = 0;

            // Uwzględnij nagłówki (+1)
            if (col == x + 1 && row == y + 1 && node instanceof javafx.scene.control.Button) {
                javafx.scene.control.Button cell = (javafx.scene.control.Button) node;

                switch (result) {
                    case HIT:
                    case SUNK:
                        cell.getStyleClass().add("cell-hit");
                        cell.setText("X");
                        break;
                    case MISS:
                        cell.getStyleClass().add("cell-miss");
                        cell.setText("•");
                        break;
                }
                break;
            }
        }
    }

    private void updateGameState(int currentPlayerId) {
        isMyTurn = (currentPlayerId == playerId);

        if (statusLabel != null) {
            if (isMyTurn) {
                statusLabel.setText("Twoja tura! Kliknij na planszę przeciwnika aby strzelać.");
                statusLabel.getStyleClass().removeAll("your-turn", "opponent-turn");
                statusLabel.getStyleClass().add("your-turn");
            } else {
                statusLabel.setText("Tura przeciwnika. Czekaj...");
                statusLabel.getStyleClass().removeAll("your-turn", "opponent-turn");
                statusLabel.getStyleClass().add("opponent-turn");
            }
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}