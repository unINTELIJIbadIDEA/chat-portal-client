package com.project.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import java.io.IOException;

public class BattleBoardController {
    @FXML
    private StackPane playerBoardPane;

    @FXML
    private StackPane enemyBoardPane;

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

            FXMLLoader enemyLoader = new FXMLLoader(getClass().getResource("/com/project/shipboard.fxml"));
            Node enemyBoard = enemyLoader.load();
            enemyBoardPane.getChildren().add(enemyBoard);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
