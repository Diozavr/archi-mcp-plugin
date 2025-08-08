package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.Map;

import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.archimatetool.mcp.server.ModelApi;
import com.archimatetool.mcp.service.ServiceRegistry;
import com.archimatetool.mcp.util.StringCaseUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ElementsHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("POST".equalsIgnoreCase(method)) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            String type = jr.optString("type");
            String name = jr.optString("name");
            String folderId = jr.optString("folderId");
            if (type == null || name == null) { ResponseUtil.badRequest(exchange, "type and name required"); return; }
            var model = ServiceRegistry.activeModel().getActiveModel();
            if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
            try {
                var el = ServiceRegistry.elements().createElement(model, StringCaseUtil.toCamelCase(type), name, folderId);
                ResponseUtil.created(exchange, ModelApi.elementToDto(el));
            } catch (Exception ex) { ResponseUtil.json(exchange, 400, Map.of("error", ex.getMessage())); }
            return;
        } else if ("GET".equalsIgnoreCase(method)) {
            ResponseUtil.methodNotAllowed(exchange); // list/search via /search
            return;
        }
        ResponseUtil.methodNotAllowed(exchange);
    }
}


