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

public class CreateChatController {
    @FXML private TextField idchatField;
    @FXML private PasswordField passwordField;

    @FXML
    private void handleCreateChat() {
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
        String requestBody = String.format("{\"roomId\":\"%s\",\"password\":\"%s\"}", roomId, password);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/conversations"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 201) {
                showAlert("Sukces", "Czat został utworzony");
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
            case 409:
                showAlert("Błąd", "Czat o podanym kodzie już istnieje");
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