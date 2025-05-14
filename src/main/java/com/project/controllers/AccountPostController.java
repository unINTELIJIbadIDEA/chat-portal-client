package com.project.controllers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.project.models.Post;
import com.project.utils.Config;
import com.project.utils.SessionManager;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ResourceBundle;

public class AccountPostController implements Initializable {
    public Button addPostButton;
    public TextArea postTextArea;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    public VBox postsView;
    private final Gson gson = new Gson();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        HttpRequest requestGet = HttpRequest.newBuilder()
                .uri(URI.create("http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/posts/"))
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(requestGet, HttpResponse.BodyHandlers.ofString());
            String responseBody = response.body();
            Post[] posts = gson.fromJson(responseBody, Post[].class);
            for (Post post : posts) {
                // Label z treścią posta
                Label contentLabel = new Label(post.getContent());
                contentLabel.setWrapText(true);
                contentLabel.setStyle("""
                -fx-font-size: 16px;
                -fx-font-family: 'Verdana';
                -fx-text-fill: #000000;
            """);

                // Przycisk Usuń
                Button deleteButton = new Button("Usuń");
                deleteButton.setStyle("""
                -fx-background-color: #ff4d4d;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 5px;
            """);

                deleteButton.setOnAction(e -> {
                    HttpRequest requestDelete = HttpRequest.newBuilder()
                            .uri(URI.create("http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/posts/" + post.getPostId()))
                            .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                            .DELETE()
                            .build();

                    try {
                        HttpResponse<String> delResponse = httpClient.send(requestDelete, HttpResponse.BodyHandlers.ofString());

                        if (delResponse.statusCode() == 200 || delResponse.statusCode() == 204) {
                            postsView.getChildren().clear();
                            initialize(null, null);
                        }
                    } catch (IOException | InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                });

                // Przycisk Modyfikuj
                Button editButton = new Button("Modyfikuj");
                editButton.setStyle("""
                -fx-background-color: #4d94ff;
                -fx-text-fill: white;
                -fx-font-weight: bold;
                -fx-background-radius: 5px;
            """);

                editButton.setOnAction(e -> {
                    showEditDialog(contentLabel, post);
                });

                HBox buttonBox = new HBox(10, editButton, deleteButton);
                buttonBox.setStyle("-fx-padding: 5px 0 0 0;");

                VBox postBox = new VBox(10, contentLabel, buttonBox);
                postBox.setStyle("""
                -fx-background-color: #f2f2f2;
                -fx-border-color: #cccccc;
                -fx-border-radius: 10px;
                -fx-background-radius: 10px;
                -fx-padding: 10px;
            """);
                postBox.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(postBox, Priority.ALWAYS);
                VBox.setVgrow(postBox, Priority.ALWAYS);

                postsView.getChildren().add(postBox);
            }

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


    public void addPostButtonClicked() throws IOException, InterruptedException {
        String postContent = postTextArea.getText();

        JsonObject json = new JsonObject();
        json.addProperty("token", SessionManager.getInstance().getToken());
        json.addProperty("content", postContent);
        String requestBody = json.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/posts"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if(response.statusCode() == 200) {
            postsView.getChildren().clear();
            initialize(null, null);
            postTextArea.setText("");
        }
    }

    private void showEditDialog(Label postLabel, Post post) {
        Stage dialogStage = new Stage();
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        dialogStage.setTitle("Edytuj post");

        TextField textField = new TextField(postLabel.getText());
        textField.setPrefWidth(400); // Szerokość pola tekstowego

        Button confirmButton = new Button("Zatwierdź");

        confirmButton.setOnAction(event -> {
            String newContent = textField.getText();

            JsonObject json = new JsonObject();
            json.addProperty("content", newContent);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/posts/" + post.getPostId()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                    .PUT(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();

            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 204) {
                    postsView.getChildren().clear();
                    initialize(null, null);
                } else {
                    System.out.println("Błąd edycji posta: " + response.statusCode());
                }
            } catch (Exception e) {
                System.out.println("Błąd podczas wysyłania PUT: " + e.getMessage());
            }

            dialogStage.close();
        });

        VBox dialogVBox = new VBox(10, textField, confirmButton);
        dialogVBox.setPadding(new Insets(20));
        dialogVBox.setPrefSize(450, 150); // ustawienie rozmiaru okna

        Scene dialogScene = new Scene(dialogVBox);
        dialogStage.setScene(dialogScene);
        dialogStage.setWidth(500);  // ustawienie szerokości okna
        dialogStage.setHeight(200); // ustawienie wysokości okna
        dialogStage.showAndWait();
    }

}
