package com.project.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = Config.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                System.out.println("Sorry, unable to find config.properties");
            }
            properties.load(input);
        } catch (IOException e) {
            System.out.println(e.getMessage());
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
