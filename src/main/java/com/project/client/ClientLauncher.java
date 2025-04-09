package com.project.client;

public class ClientLauncher {
    public static void main(String[] args) {
        String bearerToken = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjUsImlhdCI6MTc0NDA0MjY3MSwiZXhwIjoxNzQ0MDc4NjcxfQ.HSttbo4D8wCrdJgZSUIvzLPhffJ6hxm71kzvpGlHUhc";
        new ClientSessionManager("cosik", bearerToken).startSession();
    }
}
