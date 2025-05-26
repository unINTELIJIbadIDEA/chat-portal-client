package com.project.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.image.ImageView;

import java.io.IOException;
public class ScreenChooseControler {
    @FXML
    private StackPane playerBoardPane;

    @FXML
    private StackPane scrollPane;

    @FXML
    private StackPane root;

    @FXML
    private ImageView backgroundImage;

    @FXML
    public void initialize() {
        backgroundImage.fitWidthProperty().bind(root.widthProperty());
        backgroundImage.fitHeightProperty().bind(root.heightProperty());

        try {
            FXMLLoader playerLoader = new FXMLLoader(getClass().getResource("/com/project/shipboard.fxml"));
            Node playerBoard = playerLoader.load();
            playerBoardPane.getChildren().add(playerBoard);

            FXMLLoader enemyLoader = new FXMLLoader(getClass().getResource("/com/project/sectionship.fxml"));
            Node enemyBoard = enemyLoader.load();
            scrollPane.getChildren().add(enemyBoard);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
