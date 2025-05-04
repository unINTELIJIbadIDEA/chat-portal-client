package com.project.controllers;

import com.project.ChatPortal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;

public class ChatScreenController {

    @FXML
    private ListView<String> chatListView;

    @FXML
    private StackPane chatArea;

    @FXML
    private Button addChatButton;

    private final String bearerToken = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjExLCJpYXQiOjE3NDYzNTQ1NTgsImV4cCI6MTc0NjM5MDU1OH0.foLg-JGlH5IIJN8hYuXTvsrnr8tp1H5fJrZvC5whTrM";

    public void initialize() {
        chatListView.getItems().addAll("chat1", "chat2", "chat3");

        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadChat(newVal);
            }
        });

        addChatButton.setOnAction(event -> handleAddChatButton());
    }

    private void loadChat(String chatId) {
        try {
            URL resource = ChatPortal.class.getResource("chat.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            AnchorPane chatPane = loader.load();

            ChatController controller = loader.getController();
            controller.setChatSession(chatId, bearerToken); // przekazanie dynamicznego chatId

            chatArea.getChildren().setAll(chatPane);
            AnchorPane.setTopAnchor(chatPane, 0.0);
            AnchorPane.setRightAnchor(chatPane, 0.0);
            AnchorPane.setBottomAnchor(chatPane, 0.0);
            AnchorPane.setLeftAnchor(chatPane, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddChatButton() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project/sectionchat.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Dodaj nowy czat");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();

            // po dodaniu nowego chatu możesz zaktualizować listę
            // np. chatListView.getItems().add("nowyChatId");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
