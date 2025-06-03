package com.project.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = new FileInputStream("config.properties")) {
            properties.load(input);
            System.out.println("Loaded config from file.");
        } catch (IOException e) {
            System.err.println("Could not load config.properties: " + e.getMessage());
        }
    }

    public static Integer getPORT_API() {
        return Integer.parseInt(properties.getProperty("API_PORT"));
    }

    public static Integer getPORT_SERVER() {
        return Integer.parseInt(properties.getProperty("SERVER_PORT"));
    }

    public static Integer getBATTLESHIP_PORT() {
        return Integer.parseInt(properties.getProperty("BATTLESHIP_PORT"));
    }

    public static String getHOST_SERVER() {
        return properties.getProperty("SERVER_HOST");
    }
}
