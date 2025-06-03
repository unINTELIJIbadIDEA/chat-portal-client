package com.project.controllers;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.util.Duration;

public class SectionChatController {

    private Runnable refreshCallback;

    @FXML
    private Button createChatButton;

    @FXML
    private Button addChatButton;

    @FXML
    private void initialize() {
        createChatButton.setOnAction(this::handleCreateChat);
        addChatButton.setOnAction(this::handleAddChat);

        createChatButton.setOnMouseEntered(this::onMouseEntered);
        createChatButton.setOnMouseExited(this::onMouseExited);
        createChatButton.setOnMousePressed(this::onMousePressed);
        createChatButton.setOnMouseReleased(this::onMouseReleased);

        addChatButton.setOnMouseEntered(this::onMouseEntered);
        addChatButton.setOnMouseExited(this::onMouseExited);
        addChatButton.setOnMousePressed(this::onMousePressed);
        addChatButton.setOnMouseReleased(this::onMouseReleased);
    }

    public void setRefreshCallback(Runnable callback) {
        this.refreshCallback = callback;
    }

    private void handleCreateChat(ActionEvent event) {
        openWindow("/com/project/createchat.fxml", "Create Chat");
        closeCurrentWindow(event);
    }

    private void handleAddChat(ActionEvent event) {
        openWindow("/com/project/addchat.fxml", "Join Chat");
        closeCurrentWindow(event);
    }

    private void closeCurrentWindow(ActionEvent event) {
        ((Stage) ((Node) event.getSource()).getScene().getWindow()).close();
    }


    private void openWindow(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();

            if (loader.getController() instanceof CreateChatController) {
                ((CreateChatController) loader.getController()).setRefreshCallback(refreshCallback);
            } else if (loader.getController() instanceof AddChatController) {
                ((AddChatController) loader.getController()).setRefreshCallback(refreshCallback);
            }

            Stage stage = new Stage();
            stage.setTitle(title);
            //stage.setResizable(false);
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onMouseEntered(MouseEvent event) {
        Button button = (Button) event.getSource();
        button.setStyle("-fx-background-color: #ffe0b2; -fx-border-color: grey; -fx-border-radius: 5px; -fx-background-radius: 5px;");
    }

    private void onMouseExited(MouseEvent event) {
        Button button = (Button) event.getSource();
        button.setStyle("-fx-background-color: #fff8e1; -fx-border-color: grey; -fx-border-radius: 5px; -fx-background-radius: 5px;");
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