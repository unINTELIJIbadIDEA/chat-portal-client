package com.project;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ChatPortal extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        //FXMLLoader fxmlLoader = new FXMLLoader(ChatPortal.class.getResource("roomscreen.fxml"));
        FXMLLoader fxmlLoader = new FXMLLoader(ChatPortal.class.getResource("startscreen.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        stage.setTitle("TextPortal");
        stage.setScene(scene);
        stage.setWidth(1920);
        stage.setHeight(1080);
        stage.setMaximized(true);
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