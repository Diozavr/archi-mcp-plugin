package ru.cinimex.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.Map;

import ru.cinimex.archimatetool.mcp.http.ResponseUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class NotImplementedHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        ResponseUtil.notImplemented(exchange, "Not Implemented: install a compatible jArchi to enable /script APIs");
    }
}


