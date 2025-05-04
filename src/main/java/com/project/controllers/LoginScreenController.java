package com.project.controllers;
import com.google.gson.JsonObject;
import com.project.ChatPortal;
import com.project.utils.Config;
import com.project.utils.SessionManager;
import com.project.utils.TokenExtractor;
import javafx.animation.ScaleTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginScreenController {
    @FXML
    public PasswordField passwordField;
    @FXML
    public TextField usernameField;
    @FXML
    private Button loginButton;
    @FXML
    private Button backButton;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @FXML
    private void initialize() {
        loginButton.setOnAction(event -> handleLoginAction());
        backButton.setOnAction(event -> handleBackAction());

        loginButton.setOnMouseEntered(this::onMouseEntered);
        loginButton.setOnMouseExited(this::onMouseExited);
        loginButton.setOnMousePressed(this::onMousePressed);
        loginButton.setOnMouseReleased(this::onMouseReleased);

        backButton.setOnMouseEntered(this::onMouseEntered);
        backButton.setOnMouseExited(this::onMouseExited);
        backButton.setOnMousePressed(this::onMousePressed);
        backButton.setOnMouseReleased(this::onMouseReleased);
    }

    @FXML
    private void handleLoginAction() {
        String email = usernameField.getText().trim();
        String password = passwordField.getText();

        loginButton.setDisable(true);

        Task<HttpResponse<String>> loginTask = new Task<>() {
            @Override
            protected HttpResponse<String> call() throws Exception {
                return sendLoginRequest(email, password);
            }
        };

        loginTask.setOnSucceeded(event -> {
            loginButton.setDisable(false);
            processLoginResponse(loginTask.getValue());
        });

        loginTask.setOnFailed(event -> {
            loginButton.setDisable(false);
            System.err.println("Błąd logowania: " + loginTask.getException().getMessage());
        });

        executor.submit(loginTask);
    }

    private HttpResponse<String> sendLoginRequest(String email, String password) throws IOException, InterruptedException {
        JsonObject json = new JsonObject();
        json.addProperty("email", email);
        json.addProperty("password", password);
        String requestBody = json.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void processLoginResponse(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body();

        System.out.println("Response code: " + status);
        System.out.println("Response body: " + body);

        if (status == 200) {
            try {
                String token = TokenExtractor.extractToken(body);
                System.out.println("Token: " + token);
                SessionManager.getInstance().setToken(token);

                URL resource = ChatPortal.class.getResource("textwelcome.fxml");
                FXMLLoader fxmlLoader = new FXMLLoader(resource);
                Parent textwelcome = fxmlLoader.load();
                Stage stage = (Stage) loginButton.getScene().getWindow();
                stage.setScene(new Scene(textwelcome));
                stage.setMaximized(true);
                stage.show();
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
        } else if (status == 401) {
            System.err.println("Błędny email lub hasło");
        } else {
            System.err.println("Inny błąd: " + status);
        }
    }

    @FXML
    private void handleBackAction() {
        System.out.println("Powrót do poprzedniego ekranu.");
        try {
            URL resource = ChatPortal.class.getResource("startscreen.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(resource);
            AnchorPane loginScreen = fxmlLoader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(loginScreen));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void onMouseEntered(MouseEvent event) {
        Button button = (Button) event.getSource();
        button.setStyle("-fx-background-color: #ffe0b2; -fx-border-color: grey; -fx-border-radius: 10px; -fx-background-radius: 10px;");
    }

    private void onMouseExited(MouseEvent event) {
        Button button = (Button) event.getSource();
        button.setStyle("-fx-background-color: #fff8e1; -fx-border-color: grey; -fx-border-radius: 10px; -fx-background-radius: 10px;");
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