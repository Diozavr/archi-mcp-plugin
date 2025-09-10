package ru.cinimex.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ru.cinimex.archimatetool.mcp.server.JsonUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class StatusHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("ok", Boolean.TRUE);
        resp.put("service", "archi-mcp");
        resp.put("version", "0.1.0");
        JsonUtil.writeJson(exchange, 200, resp);
    }
}


