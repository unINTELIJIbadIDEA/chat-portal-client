package com.project;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatPortal extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        //FXMLLoader fxmlLoader = new FXMLLoader(ChatPortal.class.getResource("roomscreen.fxml"));
        FXMLLoader fxmlLoader = new FXMLLoader(ChatPortal.class.getResource("startscreen.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("TextPortal");
        stage.getIcons().add(new Image(ChatPortal.class.getResourceAsStream("/Image/logo.png")));
        stage.setScene(scene);
        stage.show();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[CHAT PORTAL]: Application shutting down - cleaning up connections");
        }));

        stage.setOnCloseRequest(event -> {
            System.out.println("[CHAT PORTAL]: Main window closing");
            Platform.exit();
            System.exit(0);
        });
    }


    public static void main(String[] args) {

        launch();
    }
}