package com.project.controllers;

import com.project.client.ClientSessionManager;
import com.project.models.message.Message;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

import java.io.IOException;

public class ChatController {
    @FXML
    private ListView<String> messageList;
    @FXML
    private TextField messageField;
    @FXML
    private Button sendButton;

    private ClientSessionManager clientSessionManager;
    private String bearerToken;

    public void setChatSession(String chatId, String bearerToken) {
        this.bearerToken = bearerToken;

        clientSessionManager = new ClientSessionManager(chatId, bearerToken, this::handleMessage);
        clientSessionManager.startSession();
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
