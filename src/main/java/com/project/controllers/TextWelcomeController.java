package com.project.controllers;

import com.project.HelloApplication;
import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;

public class TextWelcomeController {

    @FXML
    private Label welcomeLabel;

    @FXML
    private StackPane rootPane;

    public void initialize() {
        welcomeLabel.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                playIntroAnimation();
            }
        });
    }

    private void playIntroAnimation() {
        ScaleTransition scaleTransition = new ScaleTransition(Duration.seconds(1.3), welcomeLabel);
        scaleTransition.setFromX(1.0);
        scaleTransition.setFromY(1.0);
        scaleTransition.setToX(1.1);
        scaleTransition.setToY(1.1);

        scaleTransition.setOnFinished(event -> {
            FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.3), welcomeLabel);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);

            fadeOut.setOnFinished(e -> {
                try {
                    URL resource = HelloApplication.class.getResource("chatscreen.fxml");
                    FXMLLoader loader = new FXMLLoader(resource);
                    Parent chatRoot = loader.load();

                    Scene chatScene = new Scene(chatRoot);

                    Stage stage = (Stage) welcomeLabel.getScene().getWindow();
                    stage.setScene(chatScene);
                    stage.setWidth(1920); // Szerokość okna na pełny ekran
                    stage.setHeight(1080); // Wysokość okna na pełny ekran
                    stage.setMaximized(true); // Ustawienie przed .show()
                    stage.show();

                    // Fade-in nowego widoku
                    FadeTransition fadeIn = new FadeTransition(Duration.seconds(1), chatRoot);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);
                    fadeIn.play();

                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });

            fadeOut.play();
        });

        scaleTransition.play();
    }
}