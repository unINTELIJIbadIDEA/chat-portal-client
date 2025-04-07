package com.project.client;

public class ClientLauncher {
    public static void main(String[] args) {
        String bearerToken = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjUsImlhdCI6MTc0NDAzNjc1MywiZXhwIjoxNzQ0MDcyNzUzfQ.2qlG-AGsao5CD_qy6SS_rKXVDQTH3TqIvjGznePBJV4";
        new ClientSessionManager("cosik", bearerToken).startSession();
    }
}
