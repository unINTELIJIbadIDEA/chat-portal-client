package com.project.client;

import com.project.models.message.ClientMessage;
import java.io.IOException;

public class ClientMessageSender {
    private final ClientConnection connection;
    private final String chatId;

    public ClientMessageSender(ClientConnection connection, String chatId) {
        this.connection = connection;
        this.chatId = chatId;
    }

    public void sendMessage(String content, String token) throws IOException {
        ClientMessage clientMessage = new ClientMessage(content, chatId, token);
        connection.getOutputStream().writeObject(clientMessage);
        connection.getOutputStream().flush();
    }
}