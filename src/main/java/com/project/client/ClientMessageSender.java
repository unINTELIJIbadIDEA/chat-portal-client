package com.project.client;

import com.project.utils.Message;
import java.io.IOException;
import java.time.LocalDateTime;

public class ClientMessageSender {
    private final ClientConnection connection;
    private final String chatId;
    private final int senderId;
    private int messageId = 1;

    public ClientMessageSender(ClientConnection connection, String chatId, int senderId) {
        this.connection = connection;
        this.chatId = chatId;
        this.senderId = senderId;
    }

    public void sendMessage(String content) throws IOException {
        Message message = new Message(
                messageId++,
                chatId,
                senderId,
                content,
                LocalDateTime.now()
        );
        connection.getOutputStream().writeObject(message);
        connection.getOutputStream().flush();
    }
}