package com.project.client;

public class ClientLauncher {
    public static void main(String[] args) {
        String bearerToken = "eyJhbGciOiJIUzI1NiJ9.eyJ1c2VySWQiOjUsImlhdCI6MTc0NjI5OTc4MywiZXhwIjoxNzQ2MzM1NzgzfQ.stHz9uqXrfmTrhTwHpPepLHrmAd8lGlZrzLkl11vSdA";
        new ClientSessionManager("cosik", bearerToken).startSession();
    }
}
