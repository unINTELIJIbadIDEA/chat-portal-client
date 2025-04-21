package com.project.client;

import com.project.utils.Config;
import java.io.*;
import java.net.Socket;

public class ClientConnection {
    private Socket socket;
    private ObjectOutputStream outputStream;
    private ObjectInputStream inputStream;

    public void connect() throws IOException {
        socket = new Socket(Config.getHOST_SERVER(), Config.getPORT_SERVER());
        outputStream = new ObjectOutputStream(socket.getOutputStream());
        outputStream.flush();
        inputStream = new ObjectInputStream(socket.getInputStream());
    }

    public void disconnect() {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing input stream: " + e.getMessage());
        }
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing output stream: " + e.getMessage());
        }
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing socket: " + e.getMessage());
        }
    }

    public ObjectInputStream getInputStream() {
        return inputStream;
    }

    public ObjectOutputStream getOutputStream() {
        return outputStream;
    }

    public boolean isConnected() {
        return socket != null && !socket.isClosed();
    }
}