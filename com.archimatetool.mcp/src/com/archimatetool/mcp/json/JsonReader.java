package com.archimatetool.mcp.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.archimatetool.mcp.server.JacksonJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

/**
 * Null-safe JSON reader wrapper around Jackson.
 */
public final class JsonReader {

    private final JsonNode root;

    private JsonReader(JsonNode root) {
        this.root = root == null ? JacksonJson.mapper().createObjectNode() : root;
    }

    public static JsonReader empty() {
        return new JsonReader(JacksonJson.mapper().createObjectNode());
    }

    public static JsonReader fromExchange(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            return from(is);
        }
    }

    public static JsonReader from(InputStream is) throws IOException {
        if (is == null) return empty();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(Math.max(32, is.available()));
        byte[] buf = new byte[4096];
        int r;
        while ((r = is.read(buf)) >= 0) {
            if (r == 0) continue;
            bos.write(buf, 0, r);
        }
        return fromBytes(bos.toByteArray());
    }

    public static JsonReader fromBytes(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return empty();
        String s = new String(bytes, StandardCharsets.UTF_8);
        return fromString(s);
    }

    public static JsonReader fromString(String s) {
        if (s == null || s.isBlank()) return empty();
        try {
            JsonNode v = JacksonJson.mapper().readTree(s);
            return new JsonReader(v != null ? v : JacksonJson.mapper().createObjectNode());
        } catch (IOException ex) {
            return empty();
        }
    }

    public String optString(String key) {
        if (key == null) return null;
        JsonNode v = root.get(key);
        if (v == null || v.isNull()) return null;
        if (v.isTextual()) return v.asText();
        if (v.isNumber() || v.isBoolean()) return v.asText();
        return v.toString();
    }

    public String optString(String key, String defaultValue) {
        String v = optString(key);
        return v != null ? v : defaultValue;
    }

    public Integer optInt(String key) {
        if (key == null) return null;
        JsonNode v = root.get(key);
        if (v == null || v.isNull()) return null;
        if (v.canConvertToInt()) return v.intValue();
        if (v.isTextual()) {
            try { return Integer.valueOf(v.asText()); } catch (Exception ignore) {}
        }
        return null;
    }

    public int optInt(String key, int defaultValue) {
        Integer v = optInt(key);
        return v != null ? v.intValue() : defaultValue;
    }

    public Boolean optBool(String key) {
        if (key == null) return null;
        JsonNode v = root.get(key);
        if (v == null || v.isNull()) return null;
        if (v.isBoolean()) return v.booleanValue();
        if (v.isTextual()) {
            String s = v.asText();
            return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s);
        }
        return null;
    }

    public boolean optBool(String key, boolean defaultValue) {
        Boolean v = optBool(key);
        return v != null ? v.booleanValue() : defaultValue;
    }

    public JsonNode optObject(String key) {
        if (key == null) return null;
        JsonNode v = root.get(key);
        if (v == null || v.isNull() || !v.isObject()) return null;
        return v;
    }

    public Integer optIntWithin(String objectName, String key) {
        if (objectName == null || key == null) return null;
        JsonNode obj = optObject(objectName);
        if (obj == null) return null;
        JsonNode v = obj.get(key);
        if (v == null || v.isNull()) return null;
        if (v.canConvertToInt()) return v.intValue();
        if (v.isTextual()) {
            try { return Integer.valueOf(v.asText()); } catch (Exception ignore) {}
        }
        return null;
    }

    public int optIntWithin(String objectName, String key, int defaultValue) {
        Integer v = optIntWithin(objectName, key);
        return v != null ? v.intValue() : defaultValue;
    }
}
