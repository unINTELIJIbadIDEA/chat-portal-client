package com.project.controllers;

import com.project.utils.Config;
import com.project.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class AddChatController {
    @FXML private TextField idchatField;
    @FXML private PasswordField passwordField;

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
}