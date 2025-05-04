package com.project.controllers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.project.adapters.LocalDateTimeAdapter;
import com.project.client.ClientSessionManager;
import com.project.models.message.Message;
import com.project.utils.Config;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

public class ChatController {
    @FXML
    private ListView<String> messageList;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;

    private ClientSessionManager clientSessionManager;
    private String bearerToken;
    private String chatId;
    private static final String API_URL = "http://"+ Config.getHOST_SERVER() +":"+Config.getPORT_API()+"/api/messages";
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
                            messageList.getItems().add(msg.sender_id() + ": " + msg.content());
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
            messageList.getItems().add(message.sender_id() + ": " + message.content());
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
}
