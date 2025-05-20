package com.project.controllers;

import com.google.gson.Gson;
import com.project.models.Post;
import com.project.utils.Config;
import com.project.utils.SessionManager;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ResourceBundle;

public class ForumPostController implements Initializable {
    public VBox postsView;
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        HttpRequest getPostsRequest = HttpRequest.newBuilder()
                .uri(URI.create("http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/posts/exclude"))
                .header("Authorization", "Bearer " + SessionManager.getInstance().getToken())
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(getPostsRequest, HttpResponse.BodyHandlers.ofString());
            Post[] posts = gson.fromJson(response.body(), Post[].class);

            for (Post post : posts) {
                // Nagłówek z imieniem i nazwiskiem
                Label authorLabel = new Label(post.getName() + " " + post.getSurname());
                authorLabel.setStyle("""
                -fx-font-weight: bold;
                -fx-font-size: 14px;
                -fx-text-fill: #333333;
            """);

                // Treść posta
                Label contentLabel = new Label(post.getContent());
                contentLabel.setWrapText(true);
                contentLabel.setStyle("""
                -fx-font-size: 16px;
                -fx-font-family: 'Verdana';
                -fx-padding: 5px 0 0 0;
                -fx-text-fill: #000000;
            """);

                // Kontener posta
                VBox postBox = new VBox(5, authorLabel, contentLabel);
                postBox.setStyle("""
    -fx-background-color: #f9f9f9;
    -fx-border-color: #dddddd;
    -fx-border-radius: 10px;
    -fx-background-radius: 10px;
    -fx-padding: 10px;
""");
                postBox.setMaxWidth(Double.MAX_VALUE);

// HBox wrapper dla pełnej szerokości
                HBox wrapper = new HBox(postBox);
                wrapper.setStyle("-fx-padding: 10px;");
                wrapper.setMaxWidth(Double.MAX_VALUE);
                HBox.setHgrow(postBox, Priority.ALWAYS);

                postsView.getChildren().add(wrapper);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }


}
