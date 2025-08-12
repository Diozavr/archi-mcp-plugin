package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.folders.FoldersCore;
import com.archimatetool.mcp.http.ResponseUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** HTTP handler for listing folders using the core layer. */
public class FoldersHttpHandler implements HttpHandler {
    private final FoldersCore core = new FoldersCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        try {
            List<Map<String,Object>> roots = core.listFolders();
            ResponseUtil.ok(exchange, roots);
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }
}


