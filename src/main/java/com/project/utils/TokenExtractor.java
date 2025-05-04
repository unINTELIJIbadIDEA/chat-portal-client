package com.project.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class TokenExtractor {
    public static String extractToken(String json) {
        JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
        return jsonObject.get("token").getAsString();
    }
}
