package com.project.controllers;

import com.project.client.BattleshipClient;
import com.project.models.battleship.ShipType;
import com.project.models.battleship.messages.PlaceShipMessage;
import com.project.models.battleship.messages.PlayerReadyMessage;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.*;

public class ScreenChooseController {
    @FXML
    private StackPane playerBoardPane;

    @FXML
    private StackPane scrollPane;

    @FXML
    private StackPane root;

    @FXML
    private ImageView backgroundImage;

    private GridPane gameBoard;
    private VBox shipContainer;
    private BattleshipClient battleshipClient;
    private String gameId;
    private int playerId;
    private Button readyButton;
    private boolean isReady = false;
    private volatile boolean gameWindowOpened = false;


    // Mapa statków do umieszczenia
    private Map<ShipType, Rectangle> availableShips = new HashMap<>();
    private Map<Rectangle, ShipType> shipTypeMap = new HashMap<>();
    private List<PlacedShipInfo> placedShips = new ArrayList<>();
    private boolean allShipsPlaced = false;

    @FXML
    public void initialize() {
        if (backgroundImage != null && root != null) {
            backgroundImage.fitWidthProperty().bind(root.widthProperty());
            backgroundImage.fitHeightProperty().bind(root.heightProperty());

            // DODAJ TU - obsługa zamykania okna
            Platform.runLater(() -> {
                if (root.getScene() != null) {
                    root.getScene().getWindow().setOnCloseRequest(event -> {
                        System.out.println("[SHIP PLACEMENT]: Window closing - forcing battleship client disconnect");
                        if (battleshipClient != null) {
                            forceDisconnectFromPlacement();
                        }
                    });

                    root.getScene().getWindow().setOnHiding(event -> {
                        System.out.println("[SHIP PLACEMENT]: Window hiding - forcing battleship client disconnect");
                        if (battleshipClient != null) {
                            forceDisconnectFromPlacement();
                        }
                    });
                }
            });
        }

        try {
            // Ładuj planszę gracza
            FXMLLoader playerLoader = new FXMLLoader(getClass().getResource("/com/project/shipboard.fxml"));
            Node playerBoard = playerLoader.load();

            // Konwertuj Pane na elementy obsługujące drop
            if (playerBoard instanceof GridPane) {
                gameBoard = (GridPane) playerBoard;
                convertPanesToDropTargets(gameBoard);
                playerBoardPane.getChildren().add(playerBoard);
            }

            // Ładuj panel ze statkami
            createShipSelection();

            // Dodaj przycisk "Gotowy"
            createReadyButton();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void initializeShipPlacement(String gameId, int playerId, BattleshipClient client) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.battleshipClient = client;

        // DODAJ TU - shutdown hook
        if (this.battleshipClient != null) {
            this.battleshipClient.addShutdownHook();
        }

        System.out.println("Initialized ship placement for game: " + gameId + ", player: " + playerId);
    }


    private void createShipSelection() {
        shipContainer = new VBox(10);
        shipContainer.setPadding(new Insets(10));
        shipContainer.setStyle("-fx-background-color: rgba(207, 216, 220, 0.9); -fx-background-radius: 10;");

        Label instructionLabel = new Label("Przeciągnij statki na planszę\nKliknij prawym przyciskiem aby obrócić");
        instructionLabel.setStyle("-fx-font-size: 14px; -fx-text-alignment: center; -fx-font-weight: bold;");
        shipContainer.getChildren().add(instructionLabel);

        // Twórz statki różnych typów
        createDraggableShip(ShipType.CARRIER, Color.DARKBLUE);
        createDraggableShip(ShipType.BATTLESHIP, Color.BLUE);
        createDraggableShip(ShipType.CRUISER, Color.LIGHTBLUE);
        createDraggableShip(ShipType.SUBMARINE, Color.CYAN);
        createDraggableShip(ShipType.DESTROYER, Color.LIGHTCYAN);

        if (scrollPane != null) {
            scrollPane.getChildren().add(shipContainer);
        }
    }

    private void createDraggableShip(ShipType shipType, Color color) {
        Rectangle ship = new Rectangle();
        ship.setWidth(shipType.getLength() * 35); // 35px na segment
        ship.setHeight(35);
        ship.setFill(color);
        ship.setStroke(Color.BLACK);
        ship.setStrokeWidth(2);
        ship.getStyleClass().add("draggable-ship");

        // Dodaj label z nazwą statku
        Label shipLabel = new Label(shipType.getName() + " (" + shipType.getLength() + ")");
        shipLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");

        VBox shipBox = new VBox(5);
        shipBox.getChildren().addAll(shipLabel, ship);
        shipBox.setStyle("-fx-alignment: center; -fx-padding: 5px;");

        // Drag & Drop
        ship.setOnDragDetected(event -> {
            Dragboard dragboard = ship.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(shipType.name());
            dragboard.setContent(content);
            event.consume();
        });

        // Obracanie prawym przyciskiem
        ship.setOnMouseClicked(event -> {
            if (event.getButton() == MouseButton.SECONDARY) {
                rotateShip(ship);
            }
        });

        availableShips.put(shipType, ship);
        shipTypeMap.put(ship, shipType);
        shipContainer.getChildren().add(shipBox);
    }

    private void rotateShip(Rectangle ship) {
        double width = ship.getWidth();
        double height = ship.getHeight();
        ship.setWidth(height);
        ship.setHeight(width);
    }

    private void convertPanesToDropTargets(GridPane board) {
        // Iteruj przez wszystkie dzieci GridPane
        for (Node node : board.getChildren()) {
            if (node instanceof Pane) {
                Pane cell = (Pane) node;
                setupDropTarget(cell);
            }
        }
    }

    private void setupDropTarget(Pane cell) {
        cell.setOnDragOver(event -> {
            if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        cell.setOnDragEntered(event -> {
            if (event.getGestureSource() != cell && event.getDragboard().hasString()) {
                cell.getStyleClass().add("drag-over");
            }
            event.consume();
        });

        cell.setOnDragExited(event -> {
            cell.getStyleClass().remove("drag-over");
            event.consume();
        });

        cell.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;

            if (dragboard.hasString()) {
                try {
                    ShipType shipType = ShipType.valueOf(dragboard.getString());
                    Rectangle ship = availableShips.get(shipType);

                    if (ship != null) {
                        // Pobierz pozycję na planszy
                        Integer col = GridPane.getColumnIndex(cell);
                        Integer row = GridPane.getRowIndex(cell);
                        if (col == null) col = 0;
                        if (row == null) row = 0;

                        // Pomijamy nagłówki (kolumna 0 i wiersz 0)
                        if (col > 0 && row > 0) {
                            col--; // Przesunięcie o nagłówek
                            row--;

                            // Sprawdź czy można umieścić statek
                            boolean horizontal = ship.getWidth() > ship.getHeight();
                            if (canPlaceShip(shipType, col, row, horizontal)) {
                                placeShipOnBoard(shipType, ship, col, row, horizontal);
                                success = true;
                            }
                        }
                    }
                } catch (IllegalArgumentException e) {
                    System.err.println("Invalid ship type: " + dragboard.getString());
                }
            }

            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean canPlaceShip(ShipType shipType, int startCol, int startRow, boolean horizontal) {
        int length = shipType.getLength();

        // Sprawdź czy statek mieści się na planszy (10x10)
        if (horizontal) {
            if (startCol + length > 10) return false;
        } else {
            if (startRow + length > 10) return false;
        }

        // Sprawdź czy komórki są wolne i czy nie ma statków w otoczeniu
        for (int i = 0; i < length; i++) {
            int checkCol = horizontal ? startCol + i : startCol;
            int checkRow = horizontal ? startRow : startRow + i;

            if (isCellOccupiedWithSurrounding(checkCol, checkRow)) {
                return false;
            }
        }

        return true;
    }

    private boolean isCellOccupiedWithSurrounding(int col, int row) {
        // Sprawdź komórkę i jej otoczenie (8 kierunków + sama komórka)
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int checkCol = col + dx;
                int checkRow = row + dy;

                // Sprawdź czy w granicach planszy
                if (checkCol >= 0 && checkCol < 10 && checkRow >= 0 && checkRow < 10) {
                    if (isCellOccupied(checkCol, checkRow)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isCellOccupied(int col, int row) {
        for (PlacedShipInfo ship : placedShips) {
            if (ship.occupiesCell(col, row)) {
                return true;
            }
        }
        return false;
    }

    private void placeShipOnBoard(ShipType shipType, Rectangle originalShip, int startCol, int startRow, boolean horizontal) {
        // Usuń statek z panelu wyboru
        shipContainer.getChildren().removeIf(node -> {
            if (node instanceof VBox) {
                VBox shipBox = (VBox) node;
                return shipBox.getChildren().contains(originalShip);
            }
            return false;
        });
        availableShips.remove(shipType);

        // Umieść statek na planszy
        PlacedShipInfo placedShip = new PlacedShipInfo(shipType, startCol, startRow, horizontal);
        placedShips.add(placedShip);

        // Wizualizuj statek na planszy
        visualizeShipOnBoard(placedShip);

        // Wyślij informację do serwera
        if (battleshipClient != null) {
            battleshipClient.sendMessage(new PlaceShipMessage(playerId, gameId, shipType, startCol, startRow, horizontal));
        }

        // Sprawdź czy wszystkie statki zostały umieszczone
        checkIfAllShipsPlaced();
    }

    private void visualizeShipOnBoard(PlacedShipInfo ship) {
        for (int i = 0; i < ship.shipType.getLength(); i++) {
            int col = ship.horizontal ? ship.startCol + i : ship.startCol;
            int row = ship.horizontal ? ship.startRow : ship.startRow + i;

            // Znajdź odpowiednią komórkę i pokoloruj ją (+1 dla nagłówków)
            for (Node node : gameBoard.getChildren()) {
                Integer nodeCol = GridPane.getColumnIndex(node);
                Integer nodeRow = GridPane.getRowIndex(node);
                if (nodeCol == null) nodeCol = 0;
                if (nodeRow == null) nodeRow = 0;

                if (nodeCol == col + 1 && nodeRow == row + 1 && node instanceof Pane) {
                    Pane cell = (Pane) node;
                    cell.getStyleClass().add("ship-placed");

                    // Dodaj możliwość usunięcia statku
                    cell.setOnMouseClicked(event -> {
                        if (event.getButton() == MouseButton.SECONDARY) {
                            removeShipFromBoard(ship);
                        }
                    });
                    break;
                }
            }
        }
    }

    private void removeShipFromBoard(PlacedShipInfo ship) {
        // Usuń wizualizację z planszy
        for (int i = 0; i < ship.shipType.getLength(); i++) {
            int col = ship.horizontal ? ship.startCol + i : ship.startCol;
            int row = ship.horizontal ? ship.startRow : ship.startRow + i;

            for (Node node : gameBoard.getChildren()) {
                Integer nodeCol = GridPane.getColumnIndex(node);
                Integer nodeRow = GridPane.getRowIndex(node);
                if (nodeCol == null) nodeCol = 0;
                if (nodeRow == null) nodeRow = 0;

                if (nodeCol == col + 1 && nodeRow == row + 1 && node instanceof Pane) {
                    Pane cell = (Pane) node;
                    cell.getStyleClass().remove("ship-placed");
                    cell.setOnMouseClicked(null);
                    break;
                }
            }
        }

        // Usuń ze struktury danych
        placedShips.remove(ship);

        // Przywróć statek do panelu wyboru
        createDraggableShip(ship.shipType, getShipColor(ship.shipType));

        checkIfAllShipsPlaced();
    }

    private Color getShipColor(ShipType shipType) {
        switch (shipType) {
            case CARRIER: return Color.DARKBLUE;
            case BATTLESHIP: return Color.BLUE;
            case CRUISER: return Color.LIGHTBLUE;
            case SUBMARINE: return Color.CYAN;
            case DESTROYER: return Color.LIGHTCYAN;
            default: return Color.GRAY;
        }
    }

    private void checkIfAllShipsPlaced() {
        allShipsPlaced = placedShips.size() == ShipType.values().length;
        if (readyButton != null) {
            readyButton.setDisable(!allShipsPlaced);
        }
    }



    private void createReadyButton() {
        readyButton = new Button("Gotowy!");
        readyButton.getStyleClass().add("ready-button");
        readyButton.setPrefSize(120, 40);
        readyButton.setDisable(true);

        readyButton.setOnAction(event -> {
            if (!isReady && allShipsPlaced) {
                isReady = true;
                readyButton.setText("Oczekiwanie na przeciwnika...");
                readyButton.setDisable(true);

                System.out.println("[SHIP PLACEMENT]: Player " + playerId + " is ready! All ships placed.");

                // KRYTYCZNE: Ustaw listenery dla zmian stanu gry PRZED wysłaniem gotowości
                if (battleshipClient != null) {
                    battleshipClient.setGameStateListener(this::handleGameStateChange);
                    battleshipClient.setGameUpdateListener(this::handleGameUpdate);
                    System.out.println("[SHIP PLACEMENT]: Game listeners set for player " + playerId);

                    // NOWE: Wyślij wiadomość o gotowości do serwera
                    sendPlayerReady();
                } else {
                    System.err.println("[SHIP PLACEMENT]: BattleshipClient is null!");
                }
            }
        });




        // Dodaj przycisk do dolnej części panelu statków
        if (shipContainer != null) {
            VBox buttonContainer = new VBox(readyButton);
            buttonContainer.setStyle("-fx-alignment: center; -fx-padding: 20px;");
            shipContainer.getChildren().add(buttonContainer);
        }
    }

    private void sendPlayerReady() {
        if (battleshipClient != null && gameId != null) {
            // Użyj nowego typu wiadomości PlayerReadyMessage
            PlayerReadyMessage readyMessage = new PlayerReadyMessage(playerId, gameId);
            battleshipClient.sendMessage(readyMessage);

            System.out.println("[SHIP PLACEMENT]: Sent PLAYER_READY message for player " + playerId);
        } else {
            System.err.println("[SHIP PLACEMENT]: Cannot send ready message - client or gameId is null");
        }
    }

    // Klasa pomocnicza do przechowywania informacji o umieszczonych statkach
    private static class PlacedShipInfo {
        ShipType shipType;
        int startCol, startRow;
        boolean horizontal;

        PlacedShipInfo(ShipType shipType, int startCol, int startRow, boolean horizontal) {
            this.shipType = shipType;
            this.startCol = startCol;
            this.startRow = startRow;
            this.horizontal = horizontal;
        }

        boolean occupiesCell(int col, int row) {
            for (int i = 0; i < shipType.getLength(); i++) {
                int checkCol = horizontal ? startCol + i : startCol;
                int checkRow = horizontal ? startRow : startRow + i;
                if (checkCol == col && checkRow == row) {
                    return true;
                }
            }
            return false;
        }
    }

    private void handleGameStateChange(String newState) {
        System.out.println("[SHIP PLACEMENT]: Game state changed to: " + newState + " for player: " + playerId);

        Platform.runLater(() -> {
            if ("PLAYING".equals(newState)) {
                System.out.println("[SHIP PLACEMENT]: Game is starting! Opening game window for player: " + playerId);
                if (!gameWindowOpened) {
                    gameWindowOpened = true;
                    openGameWindow();
                }
            }
        });
    }

    private void handleGameUpdate(com.project.models.battleship.messages.GameUpdateMessage gameUpdate) {
        System.out.println("[SHIP PLACEMENT]: Game update received. State: " + gameUpdate.getGame().getState() + " for player: " + playerId);
        System.out.println("[SHIP PLACEMENT]: Players ready: " + gameUpdate.getGame().getPlayersReady());

        Platform.runLater(() -> {
            if (gameUpdate.getGame().getState() == com.project.models.battleship.GameState.PLAYING) {
                System.out.println("[SHIP PLACEMENT]: Both players ready! Opening game window for player: " + playerId);
                if (!gameWindowOpened) {
                    gameWindowOpened = true;
                    openGameWindow();
                }
            } else {
                // Aktualizuj status na przycisku
                if (readyButton != null && isReady) {
                    long readyCount = gameUpdate.getGame().getPlayersReady().values().stream()
                            .mapToLong(ready -> ready ? 1 : 0)
                            .sum();
                    readyButton.setText("Gotowych graczy: " + readyCount + "/2");
                }
            }
        });
    }

    private void openGameWindow() {
        try {
            System.out.println("[SHIP PLACEMENT]: Opening game window for player: " + playerId);

            if (root == null || root.getScene() == null) {
                System.err.println("[SHIP PLACEMENT]: Cannot get current stage");
                return;
            }

            Stage currentStage = (Stage) root.getScene().getWindow();

            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/com/project/screenship.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load());

            com.project.controllers.ScreenShipController controller = loader.getController();
            if (controller != null) {
                controller.initializeGame(gameId, playerId, battleshipClient);
                System.out.println("[SHIP PLACEMENT]: Game controller initialized for player: " + playerId);
            } else {
                System.err.println("[SHIP PLACEMENT]: Game controller is null!");
            }

            currentStage.setScene(scene);
            currentStage.setTitle("Gra w statki - Rozgrywka (Gracz " + playerId + ")");

            System.out.println("[SHIP PLACEMENT]: Game window opened successfully for player: " + playerId);

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("[SHIP PLACEMENT]: Error opening game window for player " + playerId + ": " + e.getMessage());
        }
    }

    private void forceDisconnectFromPlacement() {
        System.out.println("[SHIP PLACEMENT]: === FORCING IMMEDIATE DISCONNECT ===");

        if (battleshipClient != null) {
            battleshipClient.disconnect();
            battleshipClient = null;
        }

        System.out.println("[SHIP PLACEMENT]: === FORCED DISCONNECT COMPLETE ===");
    }

}