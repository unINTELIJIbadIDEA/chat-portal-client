package com.project.utils;

public class SessionManager {
    private static SessionManager instance;
    private String token;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    public void clearToken() {
        this.token = null;
    }

    public boolean isAuthenticated() {
        return token != null;
    }
}

