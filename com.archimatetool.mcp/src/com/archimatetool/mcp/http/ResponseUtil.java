package com.archimatetool.mcp.http;

import java.io.IOException;
import java.util.Map;

import com.archimatetool.mcp.server.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

public final class ResponseUtil {
    private ResponseUtil() {}

    public static void json(HttpExchange exchange, int status, Object body) throws IOException {
        JsonUtil.writeJson(exchange, status, body);
    }

    public static void ok(HttpExchange exchange, Object body) throws IOException {
        json(exchange, 200, body);
    }

    public static void created(HttpExchange exchange, Object body) throws IOException {
        json(exchange, 201, body);
    }

    public static void noContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
    }

    public static void methodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(405, -1);
    }

    public static void notFound(HttpExchange exchange, String msg) throws IOException {
        json(exchange, 404, Map.of("error", msg));
    }

    public static void badRequest(HttpExchange exchange, String msg) throws IOException {
        json(exchange, 400, Map.of("error", msg));
    }

    public static void conflictNoActiveModel(HttpExchange exchange) throws IOException {
        json(exchange, 409, Map.of("error", "no active model"));
    }

    public static void notImplemented(HttpExchange exchange, String msg) throws IOException {
        json(exchange, 501, Map.of("ok", false, "error", msg));
    }
}


