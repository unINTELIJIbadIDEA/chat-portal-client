package com.project.controllers;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.project.ChatPortal;
import com.project.utils.Config;
import com.project.utils.SessionManager;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import javafx.scene.input.MouseEvent;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

public class ChatScreenController {

    public Button postsButton;
    @FXML
    private ListView<String> chatListView;

    @FXML
    private StackPane chatArea;

    @FXML
    private Button addChatButton;

    @FXML
    private Button logoutButton;

    public void initialize() {
        loadConversations();

        chatListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadChat(newVal);
            }
        });

        addChatButton.setOnAction(event -> handleAddChatButton());
        postsButton.setOnAction(event -> handlePostsButton());
        logoutButton.setOnAction(event -> handleLogOutButtonAction());
    }

    private void loadChat(String chatId) {
        try {
            URL resource = ChatPortal.class.getResource("chat.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            AnchorPane chatPane = loader.load();

            ChatController controller = loader.getController();
            controller.setChatSession(chatId, SessionManager.getInstance().getToken());

            chatArea.getChildren().setAll(chatPane);
            AnchorPane.setTopAnchor(chatPane, 0.0);
            AnchorPane.setRightAnchor(chatPane, 0.0);
            AnchorPane.setBottomAnchor(chatPane, 0.0);
            AnchorPane.setLeftAnchor(chatPane, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadConversations() {
        String token = SessionManager.getInstance().getToken();
        if (token == null) {
            showAlert("Błąd", "Brak autoryzacji");
            return;
        }

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://" + Config.getHOST_SERVER() + ":" + Config.getPORT_API() + "/api/conversations"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                Type listType = new TypeToken<List<String>>(){}.getType();
                List<String> conversations = new Gson().fromJson(response.body(), listType);
                Platform.runLater(() -> chatListView.getItems().setAll(conversations));
            } else {
                showAlert("Błąd", "Nie udało się załadować konwersacji: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            showAlert("Błąd", "Problem z połączeniem: " + e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Błąd", "Wystąpił nieoczekiwany błąd");
        }
    }

    @FXML
    private void handleAddChatButton() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/project/sectionchat.fxml"));
            Parent root = loader.load();

            SectionChatController controller = loader.getController();
            controller.setRefreshCallback(this::loadConversations);

            Stage stage = new Stage();
            stage.setTitle("Dodaj nowy czat");
            stage.getIcons().add(new Image(ChatPortal.class.getResourceAsStream("/image/logo.png")));
            stage.setResizable(false);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogOutButtonAction() {
        try {

            URL resource = ChatPortal.class.getResource("startscreen.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();

            Stage newStage = new Stage();
            newStage.setTitle("TextPortal");
            newStage.getIcons().add(new Image(ChatPortal.class.getResourceAsStream("/image/logo.png")));
            newStage.setScene(new Scene(root));
            newStage.show();

            Stage currentStage = (Stage) logoutButton.getScene().getWindow();
            currentStage.close();
            SessionManager.getInstance().clearToken();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }

    @FXML
    private void handlePostsButton() {
        try {
            URL resource = ChatPortal.class.getResource("postscreen.fxml");
            FXMLLoader loader = new FXMLLoader(resource);
            Parent chatRoot = loader.load();

            Scene chatScene = new Scene(chatRoot);

            Stage stage = (Stage) postsButton.getScene().getWindow();
            stage.setScene(chatScene);
            stage.setWidth(1920);
            stage.setHeight(1080);
            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @FXML
    private void onButtonHover(MouseEvent event) {
        Button button = (Button) event.getSource();
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(150), button);
        scaleUp.setToX(0.9);
        scaleUp.setToY(0.9);
        scaleUp.play();
    }

    @FXML
    private void onButtonExit(MouseEvent event) {
        Button button = (Button) event.getSource();
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(150), button);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        scaleDown.play();
    }
}
