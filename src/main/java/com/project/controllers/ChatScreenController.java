package com.project.controllers;

import com.project.HelloApplication;
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

    public void initialize() {
        chatListView.getItems().addAll("Janek", "Kasia", "Adam");

        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadChat(newVal);
            }
        });

        addChatButton.setOnAction(event -> handleAddChatButton());
    }

    private void loadChat(String chatName) {
        try {
            URL resource = HelloApplication.class.getResource("chat.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            AnchorPane chatPane = loader.load();

            ChatController controller = loader.getController();

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project/AddChat.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Dodaj nowy czat");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}