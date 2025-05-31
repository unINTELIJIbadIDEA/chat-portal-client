package com.project.controllers;

import com.project.client.BattleshipClient;
import com.project.models.battleship.*;
import com.project.models.battleship.messages.TakeShotMessage;
import com.project.models.battleship.messages.ShotResultMessage;
import com.project.models.battleship.messages.GameUpdateMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
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
    private BattleshipGame currentGame;

    @FXML
    public void initialize() {
        if (backgroundImage != null && root != null) {
            backgroundImage.fitWidthProperty().bind(root.widthProperty());
            backgroundImage.fitHeightProperty().bind(root.heightProperty());

            // Obsługa zamykania okna
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
            // Ładuj planszę gracza
            FXMLLoader playerLoader = new FXMLLoader(getClass().getResource("/com/project/shipboard.fxml"));
            Node playerBoardNode = playerLoader.load();
            if (playerBoardNode instanceof GridPane) {
                playerBoard = (GridPane) playerBoardNode;
                playerBoardPane.getChildren().add(playerBoardNode);
            }

            // Ładuj planszę przeciwnika - użyj shipboard.fxml zamiast battleboard.fxml
            FXMLLoader enemyLoader = new FXMLLoader(getClass().getResource("/com/project/shipboard.fxml"));
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

        // Shutdown hook
        if (this.battleshipClient != null) {
            this.battleshipClient.addShutdownHook();
        }

        // Ustaw listenery
        battleshipClient.setShotResultListener(this::handleShotResult);
        battleshipClient.setGameUpdateListener(this::handleGameUpdate);

        // Poproś o aktualny stan gry
        battleshipClient.requestGameUpdate();

        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText("Gra rozpoczęta! Oczekiwanie na dane...");
            }
        });
    }

    private void setupEnemyBoardClicks() {
        if (enemyBoard == null) return;

        for (Node node : enemyBoard.getChildren()) {
            if (node instanceof Pane && !(node instanceof Label)) {
                Pane cell = (Pane) node;

                // Ustaw kursor na rękę dla komórek planszy
                cell.setOnMouseEntered(e -> {
                    if (isMyTurn && !cell.isDisabled()) {
                        cell.setStyle(cell.getStyle() + "; -fx-cursor: hand;");
                    }
                });

                cell.setOnMouseExited(e -> {
                    cell.setStyle(cell.getStyle().replace("; -fx-cursor: hand;", ""));
                });

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
            // Znajdź odpowiednią komórkę na planszy
            if (shotResult.getShooterId() == playerId) {
                // To był nasz strzał - aktualizuj planszę przeciwnika
                updateEnemyBoard(shotResult.getX(), shotResult.getY(), shotResult.getResult());
            } else {
                // To był strzał przeciwnika - aktualizuj naszą planszę
                updatePlayerBoard(shotResult.getX(), shotResult.getY(), shotResult.getResult());
            }

            // Aktualizuj status
            updateStatusAfterShot(shotResult);
        });
    }

    private void handleGameUpdate(GameUpdateMessage gameUpdate) {
        this.currentGame = gameUpdate.getGame();

        Platform.runLater(() -> {
            // Wyświetl statki gracza na jego planszy
            displayPlayerShips();

            // Aktualizuj stan gry
            int currentPlayerId = gameUpdate.getGame().getCurrentPlayer();
            updateGameState(currentPlayerId);
        });
    }

    private void displayPlayerShips() {
        if (currentGame == null || playerBoard == null) return;

        GameBoard myBoard = currentGame.getPlayerBoards().get(playerId);
        if (myBoard == null) return;

        // Wyczyść poprzednie oznaczenia
        for (Node node : playerBoard.getChildren()) {
            if (node instanceof Pane && !(node instanceof Label)) {
                node.getStyleClass().removeAll("ship-placed", "cell-hit", "cell-miss");
            }
        }

        // Wyświetl statki
        for (PlacedShip placedShip : myBoard.getShips()) {
            for (Position pos : placedShip.getPositions()) {
                Pane cell = getCellAt(playerBoard, pos.getX() + 1, pos.getY() + 1); // +1 dla nagłówków
                if (cell != null) {
                    cell.getStyleClass().add("ship-placed");
                }
            }
        }

        // Wyświetl strzały przeciwnika
        Cell[][] board = myBoard.getBoard();
        for (int x = 0; x < GameBoard.getBoardSize(); x++) {
            for (int y = 0; y < GameBoard.getBoardSize(); y++) {
                if (board[x][y].isShot()) {
                    Pane cell = getCellAt(playerBoard, x + 1, y + 1);
                    if (cell != null) {
                        if (board[x][y].hasShip()) {
                            cell.getStyleClass().add("cell-hit");
                        } else {
                            cell.getStyleClass().add("cell-miss");
                        }
                    }
                }
            }
        }
    }

    private void updateEnemyBoard(int x, int y, ShotResult result) {
        if (enemyBoard == null) return;

        Pane cell = getCellAt(enemyBoard, x + 1, y + 1); // +1 dla nagłówków
        if (cell != null) {
            switch (result) {
                case HIT:
                case SUNK:
                    cell.getStyleClass().add("cell-hit");
                    break;
                case MISS:
                    cell.getStyleClass().add("cell-miss");
                    break;
            }
        }
    }

    private void updatePlayerBoard(int x, int y, ShotResult result) {
        if (playerBoard == null) return;

        Pane cell = getCellAt(playerBoard, x + 1, y + 1); // +1 dla nagłówków
        if (cell != null) {
            switch (result) {
                case HIT:
                case SUNK:
                    if (!cell.getStyleClass().contains("cell-hit")) {
                        cell.getStyleClass().add("cell-hit");
                    }
                    break;
                case MISS:
                    if (!cell.getStyleClass().contains("cell-miss")) {
                        cell.getStyleClass().add("cell-miss");
                    }
                    break;
            }
        }
    }

    private Pane getCellAt(GridPane board, int col, int row) {
        for (Node node : board.getChildren()) {
            Integer nodeCol = GridPane.getColumnIndex(node);
            Integer nodeRow = GridPane.getRowIndex(node);
            if (nodeCol == null) nodeCol = 0;
            if (nodeRow == null) nodeRow = 0;

            if (nodeCol == col && nodeRow == row && node instanceof Pane && !(node instanceof Label)) {
                return (Pane) node;
            }
        }
        return null;
    }

    private void updateStatusAfterShot(ShotResultMessage shotResult) {
        if (statusLabel != null) {
            if (shotResult.getShooterId() == playerId) {
                // Nasz strzał
                switch (shotResult.getResult()) {
                    case HIT:
                        statusLabel.setText("Trafienie! Strzelaj ponownie!");
                        statusLabel.getStyleClass().removeAll("your-turn", "opponent-turn");
                        statusLabel.getStyleClass().add("your-turn");
                        isMyTurn = true;
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
                        showAlert("Zwycięstwo!", "Gratulacje! Wygrałeś grę!");
                        statusLabel.setText("Gra zakończona - Zwycięstwo!");
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
            } else {
                // Strzał przeciwnika
                switch (shotResult.getResult()) {
                    case HIT:
                    case SUNK:
                        statusLabel.setText("Przeciwnik trafił! Jego tura kontynuowana.");
                        statusLabel.getStyleClass().removeAll("your-turn", "opponent-turn");
                        statusLabel.getStyleClass().add("opponent-turn");
                        break;
                    case MISS:
                        statusLabel.setText("Przeciwnik spudłował! Twoja tura.");
                        statusLabel.getStyleClass().removeAll("your-turn", "opponent-turn");
                        statusLabel.getStyleClass().add("your-turn");
                        isMyTurn = true;
                        break;
                    case GAME_OVER:
                        showAlert("Porażka", "Przeciwnik wygrał grę.");
                        statusLabel.setText("Gra zakończona - Porażka");
                        statusLabel.getStyleClass().removeAll("your-turn", "opponent-turn");
                        break;
                }
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