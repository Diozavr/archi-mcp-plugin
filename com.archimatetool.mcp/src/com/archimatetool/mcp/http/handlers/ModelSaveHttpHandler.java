package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.Map;

import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.model.ModelCore;
import com.archimatetool.mcp.http.ResponseUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** HTTP handler for saving the active model via the core layer. */
public class ModelSaveHttpHandler implements HttpHandler {
    private final ModelCore core = new ModelCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        try {
            Map<String,Object> resp = core.saveModel();
            ResponseUtil.ok(exchange, resp);
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }
}


