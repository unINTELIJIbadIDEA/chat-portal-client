package com.project.client;

public class ClientLauncher {
    public static void main(String[] args) {
        String bearerToken = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjUsImlhdCI6MTc0NjE4NDEyMCwiZXhwIjoxNzQ2MjIwMTIwfQ.ojquoucBR29sVqZfKzaSQV0GsSQg9BM3l0I7M4rVyyg";
        new ClientSessionManager("cosik", bearerToken).startSession();
    }
}
