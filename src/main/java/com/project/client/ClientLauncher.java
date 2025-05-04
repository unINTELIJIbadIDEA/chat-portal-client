package com.project.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ClientLauncher {
    public static void main(String[] args) {
        String bearerToken = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjUsImlhdCI6MTc0NjM1MTQ5MSwiZXhwIjoxNzQ2Mzg3NDkxfQ.8-m4N8DLgZeYwkoNXGPzOmjpFsA83Mnlf6ePojzKwRE";

        ClientSessionManager sessionManager = new ClientSessionManager(
                "chat2",
                bearerToken,
                message -> System.out.println(message.sender_id() + ": " + message.content())
        );

        sessionManager.startSession();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            while (sessionManager.isRunning()) {
                String input = reader.readLine();
                if (input == null || input.equalsIgnoreCase("/exit")) {
                    sessionManager.endSession();
                    break;
                }
                sessionManager.sendMessage(input);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}