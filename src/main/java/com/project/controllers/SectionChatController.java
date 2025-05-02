package com.project.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class SectionChatController {

    @FXML
    private Button createChatButton;

    @FXML
    private Button addChatButton;

    @FXML
    private void initialize() {
        createChatButton.setOnAction(this::handleCreateChat);
        addChatButton.setOnAction(this::handleAddChat);
    }

    private void handleCreateChat(ActionEvent event) {
        openWindow("/com/project/createchat.fxml", "Create Chat");
    }

    private void handleAddChat(ActionEvent event) {
        openWindow("/com/project/addchat.fxml", "Join Chat");
    }

    private void openWindow(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(title);
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}