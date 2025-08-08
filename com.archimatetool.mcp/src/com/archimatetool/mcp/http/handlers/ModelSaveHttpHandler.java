package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.server.ModelApi;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ModelSaveHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        boolean ok = com.archimatetool.mcp.service.ServiceRegistry.activeModel().saveActiveModel();
        Map<String,Object> resp = new HashMap<>();
        resp.put("saved", ok);
        var model = com.archimatetool.mcp.service.ServiceRegistry.activeModel().getActiveModel();
        if (model != null) {
            resp.put("modelId", model.getId());
            if (model.getFile() != null) resp.put("path", model.getFile().getAbsolutePath());
        }
        ResponseUtil.ok(exchange, resp);
    }
}


