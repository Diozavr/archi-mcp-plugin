package com.archimatetool.mcp.json;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.sun.net.httpserver.HttpExchange;

/**
 * Minimal, null-safe JSON reader wrapper around minimal-json.
 * Focused on simple object payloads used by REST handlers.
 */
public final class JsonReader {

    private final JsonObject root;

    private JsonReader(JsonObject root) {
        this.root = root == null ? new JsonObject() : root;
    }

    public static JsonReader empty() {
        return new JsonReader(new JsonObject());
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
        JsonValue v;
        try {
            v = Json.parse(s);
        } catch (Exception ex) {
            // Graceful fallback to empty for malformed input; handlers validate required fields explicitly
            return empty();
        }
        return new JsonReader(v.isObject() ? v.asObject() : new JsonObject());
    }

    public String optString(String key) {
        if (key == null) return null;
        JsonValue v = root.get(key);
        if (v == null || v.isNull()) return null;
        if (v.isString()) return v.asString();
        // Accept numbers/bools as strings as a best-effort fallback
        if (v.isNumber()) return String.valueOf(v.asInt());
        if (v.isBoolean()) return String.valueOf(v.asBoolean());
        return v.toString();
    }

    public Integer optInt(String key) {
        if (key == null) return null;
        JsonValue v = root.get(key);
        if (v == null || v.isNull()) return null;
        try {
            if (v.isNumber()) return v.asInt();
            if (v.isString()) return Integer.valueOf(v.asString());
        } catch (Exception ignore) {
            // fallthrough to null
        }
        return null;
    }

    public Boolean optBool(String key) {
        if (key == null) return null;
        JsonValue v = root.get(key);
        if (v == null || v.isNull()) return null;
        if (v.isBoolean()) return v.asBoolean();
        if (v.isString()) {
            String s = v.asString();
            return "true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s);
        }
        return null;
    }

    public JsonObject optObject(String key) {
        if (key == null) return null;
        JsonValue v = root.get(key);
        if (v == null || v.isNull() || !v.isObject()) return null;
        return v.asObject();
    }

    public Integer optIntWithin(String objectName, String key) {
        if (objectName == null || key == null) return null;
        JsonObject obj = optObject(objectName);
        if (obj == null) return null;
        JsonValue v = obj.get(key);
        if (v == null || v.isNull()) return null;
        try {
            if (v.isNumber()) return v.asInt();
            if (v.isString()) return Integer.valueOf(v.asString());
        } catch (Exception ignore) {}
        return null;
    }
}


