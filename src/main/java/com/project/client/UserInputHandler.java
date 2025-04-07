package com.project.client;

import java.io.IOException;
import java.util.Scanner;

public class UserInputHandler {
    private final ClientMessageSender messageSender;
    private final ClientSessionManager sessionManager;

    public UserInputHandler(ClientMessageSender messageSender, ClientSessionManager sessionManager) {
        this.messageSender = messageSender;
        this.sessionManager = sessionManager;
    }

    public void start() {
        try (Scanner scanner = new Scanner(System.in)) {
            while (sessionManager.isRunning()) {
                String input = scanner.nextLine();
                handleInput(input);
            }
        }
    }

    private void handleInput(String input) {
        if ("exit".equalsIgnoreCase(input)) {
            sessionManager.endSession();
        } else {
            try {
                String token = sessionManager.getToken();
                messageSender.sendMessage(input, token);
            } catch (IOException e) {
                System.out.println("Błąd podczas wysyłania wiadomości: " + e.getMessage());
            }
        }
    }
}