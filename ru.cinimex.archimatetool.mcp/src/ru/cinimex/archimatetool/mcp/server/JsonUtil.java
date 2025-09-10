package ru.cinimex.archimatetool.mcp.server;

import java.io.IOException;
import java.io.OutputStream;

import com.sun.net.httpserver.HttpExchange;

/**
 * JSON response helper backed by Jackson.
 */
public final class JsonUtil {
    private JsonUtil() {}

    public static void writeJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = JacksonJson.writeBytes(body);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }
}
