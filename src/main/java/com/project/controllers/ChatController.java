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
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class ChatController implements Initializable {
    @FXML
    private ListView<HBox> messageList;

    @FXML
    private TextField messageField;

    @FXML
    private Button sendButton;

    private ClientSessionManager clientSessionManager;
    private String bearerToken;
    private String chatId;

    private static final String API_URL = "http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/messages";
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
        Label messageLabel = createMessageLabel(msg.content());
        int currentUserId = extractCurrentUserId();

        boolean isCurrentUser = currentUserId != -1 && msg.sender_id() == currentUserId;
        styleMessageLabel(messageLabel, isCurrentUser);

        HBox hBox = new HBox(messageLabel);
        hBox.setPadding(new Insets(5, 10, 5, 10));
        hBox.setAlignment(isCurrentUser ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);

        return hBox;
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

    @Override
    public void initialize(java.net.URL url, java.util.ResourceBundle resourceBundle) {
        sendButton.setOnMouseEntered(this::onMouseEntered);
        sendButton.setOnMouseExited(this::onMouseExited);
        sendButton.setOnMousePressed(this::onMousePressed);
        sendButton.setOnMouseReleased(this::onMouseReleased);
    }

    private void onMouseEntered(MouseEvent event) {
        Button button = (Button) event.getSource();
        button.setStyle("-fx-background-color: #ffe0b2; -fx-border-color: grey; -fx-border-radius: 10px; -fx-background-radius: 10px;");
    }

    private void onMouseExited(MouseEvent event) {
        Button button = (Button) event.getSource();
        button.setStyle("-fx-background-color: #fff8e1; -fx-border-color: grey; -fx-border-radius: 10px; -fx-background-radius: 10px;");
    }

    private void onMousePressed(MouseEvent event) {
        Button button = (Button) event.getSource();
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(150), button);
        scaleTransition.setToX(0.9);
        scaleTransition.setToY(0.9);
        scaleTransition.play();
    }

    private void onMouseReleased(MouseEvent event) {
        Button button = (Button) event.getSource();
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(150), button);
        scaleTransition.setToX(1.0);
        scaleTransition.setToY(1.0);
        scaleTransition.play();
    }

}