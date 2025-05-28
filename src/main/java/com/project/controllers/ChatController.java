package com.project.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.project.adapters.LocalDateTimeAdapter;
import com.project.client.ClientSessionManager;
import com.project.models.message.Message;
import com.project.utils.Config;
import com.project.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ChatController {
    @FXML
    private ListView<HBox> messageList;

    @FXML
    private TextField messageField;

    @FXML
    private Button sendButton;

    @FXML
    private Button createGameButton;

    private ClientSessionManager clientSessionManager;
    private String bearerToken;
    private String chatId;
    private HttpClient httpClient = HttpClient.newHttpClient();
    private volatile boolean battleshipWindowOpened = false;

    private static final String API_URL = "http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/messages";
    private static final String BATTLESHIP_API_URL = "http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/battleship";

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    public void setChatSession(String chatId, String bearerToken) {
        this.bearerToken = bearerToken;
        this.chatId = chatId;

        loadMessages();

        clientSessionManager = new ClientSessionManager(chatId, bearerToken, this::handleMessage);
        clientSessionManager.startSession();
    }

    @FXML
    public void initialize() {
        if (createGameButton != null) {
            createGameButton.setOnAction(e -> createBattleshipGame());
        }
    }

    // NOWA METODA - tworzenie gry w statki
    private void createBattleshipGame() {
        try {
            String requestBody = gson.toJson(Map.of(
                    "gameName", "Gra w statki - " + chatId,
                    "chatId", chatId
            ));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BATTLESHIP_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + bearerToken)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                // Nowa gra utworzona
                System.out.println("Nowa gra utworzona pomyślnie!");
            } else if (response.statusCode() == 200) {
                // Użytkownik już ma grę - obsłuż rejoin
                JsonObject responseData = JsonParser.parseString(response.body()).getAsJsonObject();
                String action = responseData.get("action").getAsString();

                if ("rejoin".equals(action)) {
                    Platform.runLater(() -> {
                        // Automatycznie otwórz okno gry
                        System.out.println("Rejoining existing game...");
                        openBattleshipWindow(responseData);
                    });
                }
            } else {
                System.err.println("Błąd tworzenia gry: " + response.statusCode());
                System.err.println("Response: " + response.body());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadMessages() {
        new Thread(() -> {
            try {
                URL url = new URL(API_URL + "?chatId=" + chatId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
                conn.setRequestProperty("Accept", "application/json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String responseBody = reader.lines().collect(Collectors.joining());
                    reader.close();

                    Type messageListType = new TypeToken<List<Message>>() {}.getType();
                    List<Message> messages = gson.fromJson(responseBody, messageListType);

                    Platform.runLater(() -> {
                        for (Message msg : messages) {
                            messageList.getItems().add(createMessageBubble(msg));
                        }
                        messageList.scrollTo(messageList.getItems().size() - 1);
                    });
                } else {
                    System.out.println("Nie udało się pobrać wiadomości. Kod: " + conn.getResponseCode());
                }

            } catch (Exception e) {
                System.out.println("Błąd przy pobieraniu wiadomości: " + e.getMessage());
            }
        }).start();
    }

    private void handleMessage(Message message) {
        Platform.runLater(() -> {
            messageList.getItems().add(createMessageBubble(message));
            messageList.scrollTo(messageList.getItems().size() - 1);
        });
    }

    @FXML
    public void sendMessage() {
        String text = messageField.getText().trim();
        if (!text.isEmpty()) {
            try {
                clientSessionManager.sendMessage(text);
                messageField.clear();
            } catch (IOException e) {
                System.out.println("Błąd wysyłania: " + e.getMessage());
            }
        }
    }

    private HBox createMessageBubble(Message msg) {
        // NOWA LOGIKA - sprawdź czy wiadomość zawiera informacje o grze
        if (msg.content().startsWith("[BATTLESHIP_GAME]")) {
            return createGameMessageBubble(msg);
        }

        Label messageLabel = createMessageLabel(msg.content());
        int currentUserId = extractCurrentUserId();

        boolean isCurrentUser = currentUserId != -1 && msg.sender_id() == currentUserId;
        styleMessageLabel(messageLabel, isCurrentUser);

        HBox hBox = new HBox(messageLabel);
        hBox.setPadding(new Insets(5, 10, 5, 10));
        hBox.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        return hBox;
    }

    // NOWA METODA - tworzenie kafelka z grą
    private HBox createGameMessageBubble(Message msg) {
        try {
            // Parsuj dane gry z wiadomości
            String gameData = msg.content().substring("[BATTLESHIP_GAME]".length());
            JsonObject gameInfo = JsonParser.parseString(gameData).getAsJsonObject();

            VBox gameBox = new VBox(5);
            gameBox.setStyle("-fx-background-color: #e3f2fd; -fx-border-color: #1976d2; -fx-border-width: 2px; -fx-border-radius: 10px; -fx-background-radius: 10px; -fx-padding: 10px;");

            Label gameLabel = new Label("🚢 " + gameInfo.get("gameName").getAsString());
            gameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

            Label statusLabel = new Label("Status: " + gameInfo.get("status").getAsString());

            Button joinButton = new Button("Dołącz do gry");
            joinButton.setStyle("-fx-background-color: #1976d2; -fx-text-fill: white; -fx-background-radius: 5px;");
            joinButton.setOnAction(e -> joinBattleshipGame(gameInfo));

            gameBox.getChildren().addAll(gameLabel, statusLabel, joinButton);

            HBox hBox = new HBox(gameBox);
            hBox.setPadding(new Insets(5, 10, 5, 10));
            hBox.setAlignment(Pos.CENTER_LEFT);

            return hBox;
        } catch (Exception e) {
            // Jeśli parsing się nie uda, zwróć normalną wiadomość
            Label errorLabel = new Label("Gra w statki została utworzona");
            HBox hBox = new HBox(errorLabel);
            hBox.setPadding(new Insets(5, 10, 5, 10));
            return hBox;
        }
    }

    // NOWA METODA - dołączanie do gry
    private void joinBattleshipGame(JsonObject gameInfo) {
        try {
            String gameId = gameInfo.get("gameId").getAsString();
            String chatId = gameInfo.get("chatId").getAsString();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(BATTLESHIP_API_URL + "/chat/" + chatId + "/join"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + bearerToken)
                    .POST(HttpRequest.BodyPublishers.ofString("{}"))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Parsuj odpowiedź i otwórz okno gry
                JsonObject joinResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                String action = joinResponse.has("action") ? joinResponse.get("action").getAsString() : "joined";

                Platform.runLater(() -> {
                    if ("rejoin".equals(action)) {
                        System.out.println("Rejoining existing game...");
                    } else {
                        System.out.println("Joined new game...");
                    }
                    openBattleshipWindow(joinResponse);
                });
            } else {
                System.err.println("Błąd dołączania do gry: " + response.statusCode());
                System.err.println("Response: " + response.body());

                // Pokaż błąd użytkownikowi
                Platform.runLater(() -> {
                    try {
                        JsonObject errorResponse = JsonParser.parseString(response.body()).getAsJsonObject();
                        String errorMessage = errorResponse.get("error").getAsString();
                        showGameError("Błąd dołączania do gry", errorMessage);
                    } catch (Exception e) {
                        showGameError("Błąd dołączania do gry", "Nie można dołączyć do gry");
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Platform.runLater(() -> {
                showGameError("Błąd połączenia", "Problem z połączeniem do serwera");
            });
        }
    }

    // NOWA METODA - otwieranie okna gry
    private void openBattleshipWindow(JsonObject gameData) {
        if (battleshipWindowOpened) {
            System.out.println("[CHAT CONTROLLER]: Battleship window already opened, ignoring request");
            return;
        }

        battleshipWindowOpened = true;
        System.out.println("[CHAT CONTROLLER]: Opening battleship window...");

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project/roomscreen.fxml"));
            Parent root = loader.load();

            // Przekaż dane gry do kontrolera
            RoomScreenController controller = loader.getController();
            if (controller != null) {
                controller.initializeGame(
                        gameData.get("gameId").getAsString(),
                        gameData.get("chatId").getAsString(),
                        gameData.get("battleshipServerPort").getAsInt(),
                        bearerToken
                );
            }

            // Otwórz w nowym oknie, nie zastępuj okna czatu
            Stage battleshipStage = new Stage();
            battleshipStage.setTitle("Gra w statki - Poczekalnia");
            battleshipStage.setScene(new Scene(root, 1200, 800));

            // KRYTYCZNE: Reset flagi po zamknięciu okna
            battleshipStage.setOnCloseRequest(event -> {
                battleshipWindowOpened = false;
                System.out.println("[CHAT CONTROLLER]: Battleship window closed, flag reset");
            });

            battleshipStage.show();
            System.out.println("[CHAT CONTROLLER]: Battleship window opened successfully");

        } catch (IOException e) {
            e.printStackTrace();
            battleshipWindowOpened = false; // Reset flagi w przypadku błędu
            System.err.println("[CHAT CONTROLLER]: Error opening battleship window: " + e.getMessage());
        }
    }

    private Label createMessageLabel(String content) {
        Label label = new Label(content);
        label.setWrapText(true);
        label.setPadding(new Insets(10));
        label.setMaxWidth(400);
        return label;
    }

    private int extractCurrentUserId() {
        String token = SessionManager.getInstance().getToken();
        if (token != null && !token.isEmpty()) {
            try {
                String[] tokenParts = token.split("\\.");
                if (tokenParts.length >= 2) {
                    String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]), StandardCharsets.UTF_8);
                    JsonObject jsonPayload = JsonParser.parseString(payload).getAsJsonObject();
                    return jsonPayload.get("userId").getAsInt();
                }
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
        }
        return -1;
    }

    private void styleMessageLabel(Label label, boolean isCurrentUser) {
        if (isCurrentUser) {
            label.setStyle("-fx-background-color: #fff8e1; -fx-background-radius: 10; -fx-border-radius: 10;");
        } else {
            label.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10; -fx-border-radius: 10;");
        }
    }

    private void showGameError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }



}