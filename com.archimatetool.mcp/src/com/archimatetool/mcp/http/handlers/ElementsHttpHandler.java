package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import com.archimatetool.mcp.core.elements.ElementsCore;
import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.types.CreateElementCmd;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ElementsHttpHandler implements HttpHandler {
    private final ElementsCore core = new ElementsCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("POST".equalsIgnoreCase(method)) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            CreateElementCmd cmd = new CreateElementCmd(jr.optString("type"), jr.optString("name"), jr.optString("folderId"));
            try {
                var dto = core.createElement(cmd);
                ResponseUtil.created(exchange, dto);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        } else if ("GET".equalsIgnoreCase(method)) {
            ResponseUtil.methodNotAllowed(exchange); // list/search via /search
            return;
        }
        ResponseUtil.methodNotAllowed(exchange);
    }
}


