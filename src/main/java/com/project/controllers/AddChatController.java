package com.project.controllers;

import com.project.utils.Config;
import com.project.utils.SessionManager;
import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AddChatController implements Initializable {
    @FXML private TextField idchatField;
    @FXML private PasswordField passwordField;
    @FXML private Button addChatButton;

    @Override
    public void initialize(java.net.URL url, java.util.ResourceBundle resourceBundle) {
        addChatButton.setOnMouseEntered(this::onMouseEntered);
        addChatButton.setOnMouseExited(this::onMouseExited);
        addChatButton.setOnMousePressed(this::onMousePressed);
        addChatButton.setOnMouseReleased(this::onMouseReleased);
    }

    @FXML
    private void handleAddChat() {
        String roomId = idchatField.getText().trim();
        String password = passwordField.getText().trim();

        if (roomId.isEmpty() || password.isEmpty()) {
            showAlert("Błąd", "Wypełnij wszystkie pola");
            return;
        }

        String token = SessionManager.getInstance().getToken();
        if (token == null) {
            showAlert("Błąd", "Brak autoryzacji");
            return;
        }

        HttpClient client = HttpClient.newHttpClient();
        String requestBody = String.format("{\"password\":\"%s\"}", password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/conversations/" + roomId + "/join"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                showAlert("Sukces", "Pomyślnie dołączono do czatu");
                idchatField.getScene().getWindow().hide();
            } else {
                handleErrorResponse(response.statusCode());
            }
        } catch (Exception e) {
            showAlert("Błąd", "Problem z połączeniem: " + e.getMessage());
        }
    }

    private void handleErrorResponse(int statusCode) {
        switch (statusCode) {
            case 401:
                showAlert("Błąd", "Nieprawidłowe hasło lub kod czatu");
                break;
            case 404:
                showAlert("Błąd", "Czat nie istnieje");
                break;
            default:
                showAlert("Błąd", "Wystąpił błąd: " + statusCode);
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void onMouseEntered(MouseEvent event) {
        Button button = (Button) event.getSource();
        button.setStyle("-fx-background-color: #ffe0b2; -fx-border-color: grey; -fx-border-radius: 5px; -fx-background-radius: 5px;");
    }

    private void onMouseExited(MouseEvent event) {
        Button button = (Button) event.getSource();
        button.setStyle("-fx-background-color: #fff8e1; -fx-border-color: grey; -fx-border-radius: 5px; -fx-background-radius: 5px;");
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