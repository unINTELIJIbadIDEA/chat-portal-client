package com.project.controllers;
import com.project.ChatPortal;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

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

    public void initialize() {
        chatButton.setOnAction(event -> handleChatButton());
        myscreenButton.setOnAction(event -> handleMyScreenButton());
        forumscreenButton.setOnAction(event -> handleForumScreenButton());

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
            Pane chatPane = loader.load(); // używamy Pane ogólnie, pasuje do StackPane
            postArea.getChildren().setAll(chatPane); // bez Anchorów, bo postArea to StackPane
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleForumScreenButton() {
        try {
            URL resource = ChatPortal.class.getResource("forumpost.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            Pane chatPane = loader.load(); // StackPane też dziedziczy po Pane
            postArea.getChildren().setAll(chatPane); // wstawienie i nadpisanie poprzedniego widoku
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}



