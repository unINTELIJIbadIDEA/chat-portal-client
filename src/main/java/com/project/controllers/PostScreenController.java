package com.project.controllers;
import com.project.ChatPortal;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public class PostScreenController {
    @FXML
    private Button chatButton;
    @FXML
    private StackPane postArea;
    @FXML
    private Button myscreenButton;

    @FXML
    private Button forumscreenButton;

    @FXML
    private Button logoutButton;

    public void initialize() {
        chatButton.setOnAction(event -> handleChatButton());
        myscreenButton.setOnAction(event -> handleMyScreenButton());
        forumscreenButton.setOnAction(event -> handleForumScreenButton());
        logoutButton.setOnAction(event -> handleLogOutButtonAction());

        myscreenButton.setOnMouseEntered(this::onMouseEntered);
        myscreenButton.setOnMouseExited(this::onMouseExited);
        myscreenButton.setOnMousePressed(this::onMousePressed);
        myscreenButton.setOnMouseReleased(this::onMouseReleased);

        forumscreenButton.setOnMouseEntered(this::onMouseEntered);
        forumscreenButton.setOnMouseExited(this::onMouseExited);
        forumscreenButton.setOnMousePressed(this::onMousePressed);
        forumscreenButton.setOnMouseReleased(this::onMouseReleased);

    }

    @FXML
    private void handleLogOutButtonAction() {
        try {

            URL resource = ChatPortal.class.getResource("startscreen.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Stage newStage = new Stage();
            newStage.setTitle("TextPortal");
            newStage.setScene(new Scene(root));
            newStage.show();

            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            currentStage.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleChatButton() {
        try {
            URL resource = ChatPortal.class.getResource("chatscreen.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            Parent chatRoot = loader.load();

            Scene chatScene = new Scene(chatRoot);

            Stage stage = (Stage) chatButton.getScene().getWindow();
            stage.setScene(chatScene);
            stage.setWidth(1920); // Szerokość okna na pełny ekran
            stage.setHeight(1080); // Wysokość okna na pełny ekran
            stage.setMaximized(true); // Ustawienie przed .show()
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleMyScreenButton() {
        try {
            URL resource = ChatPortal.class.getResource("accountpost.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            Pane chatPane = loader.load();
            postArea.getChildren().setAll(chatPane);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleForumScreenButton() {
        try {
            URL resource = ChatPortal.class.getResource("forumpost.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            Pane chatPane = loader.load();
            postArea.getChildren().setAll(chatPane);
        } catch (IOException e) {
            throw new RuntimeException(e);
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



