package com.project.controllers;
import com.google.gson.JsonObject;
import com.project.ChatPortal;
import com.project.utils.Config;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.awt.*;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;


public class RegisterScreenController {
    @FXML
    private Button registerButton;
    @FXML
    private Button backButton;

    @FXML
    public PasswordField confirmPasswordField;
    @FXML
    public PasswordField passwordField;
    @FXML
    public DatePicker birthdayPicker;
    @FXML
    public TextField emailField;
    @FXML
    public TextField nicknameField;
    @FXML
    public TextField surnameField;
    @FXML
    public TextField nameField;

    HttpClient httpClient = HttpClient.newHttpClient();


    @FXML
    private void initialize() {
        registerButton.setOnAction(event -> handleRegisterAction());
        backButton.setOnAction(event -> handleBackAction());

        registerButton.setOnMouseEntered(this::onMouseEntered);
        registerButton.setOnMouseExited(this::onMouseExited);
        registerButton.setOnMousePressed(this::onMousePressed);
        registerButton.setOnMouseReleased(this::onMouseReleased);

        backButton.setOnMouseEntered(this::onMouseEntered);
        backButton.setOnMouseExited(this::onMouseExited);
        backButton.setOnMousePressed(this::onMousePressed);
        backButton.setOnMouseReleased(this::onMouseReleased);
    }

    @FXML
    private void handleRegisterAction() {
        String name = nameField.getText().trim();
        String surname = surnameField.getText().trim();
        String nick = nicknameField.getText().trim();
        String email = emailField.getText().trim();
        LocalDate birthday = birthdayPicker.getValue();
        String pwd = passwordField.getText();
        String pwd2 = confirmPasswordField.getText();

        if (name.isEmpty() || surname.isEmpty() || nick.isEmpty() || email.isEmpty()
                || birthday == null || pwd.isEmpty() || pwd2.isEmpty()) {
            System.out.println("Wypełnij wszystkie pola.");
            return;
        }
        if (!pwd.equals(pwd2)) {
            System.out.println("Hasła nie są takie same.");
            return;
        }

        try {
            HttpResponse<String> response = sendRegisterRequest(name, surname, nick, email, birthday, pwd);
            processRegisterResponse(response);
        } catch (IOException | InterruptedException e) {
            System.err.println("Błąd podczas rejestracji: " + e.getMessage());
        }
    }

    private HttpResponse<String> sendRegisterRequest(String name, String surname, String nick, String email,
                                                     LocalDate birthday, String password) throws IOException, InterruptedException {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("surname", surname);
        json.addProperty("nickname", nick);
        json.addProperty("email", email);
        json.addProperty("birthday", birthday.toString());
        json.addProperty("password", password);
        String requestBody = json.toString();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/users"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private void processRegisterResponse(HttpResponse<String> response) {
        int status = response.statusCode();
        String body = response.body();

        System.out.println("Response code: " + status);
        System.out.println("Response body: " + body);

        if (status == 201) {
            try {
                URL resource = ChatPortal.class.getResource("startscreen.fxml");
                FXMLLoader fxmlLoader = new FXMLLoader(resource);
                AnchorPane loginScreen = fxmlLoader.load();
                Stage stage = (Stage) backButton.getScene().getWindow();
                stage.setScene(new Scene(loginScreen));
                stage.show();
            } catch (IOException e) {
                System.err.println("Błąd podczas ładowania panelu: " + e.getMessage());
            }
        } else if (status == 400) {
            System.err.println("Niepoprawny format danych");
        } else if (status == 409) {
            System.err.println("Użytkownik już istnieje");
        } else {
            System.err.println("Nieznany błąd: " + status);
        }
    }

    @FXML
    private void handleBackAction() {
        try {
            URL resource = ChatPortal.class.getResource("startscreen.fxml");
            FXMLLoader fxmlLoader = new FXMLLoader(resource);
            AnchorPane loginScreen = fxmlLoader.load();
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(new Scene(loginScreen));
            stage.show();
        } catch (Exception e) {
            System.out.println(e.getMessage());
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
        scaleTransition.setToX(0.9); // Zmniejsz przycisk
        scaleTransition.setToY(0.9);
        scaleTransition.play();
    }

    private void onMouseReleased(MouseEvent event) {
        Button button = (Button) event.getSource();
        ScaleTransition scaleTransition = new ScaleTransition(Duration.millis(150), button);
        scaleTransition.setToX(1.0); // Przywróć oryginalny rozmiar
        scaleTransition.setToY(1.0);
        scaleTransition.play();
    }
}
