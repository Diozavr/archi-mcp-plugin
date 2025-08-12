package com.archimatetool.mcp.http.handlers;

import java.io.IOException;

import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.types.CreateViewCmd;
import com.archimatetool.mcp.core.views.ViewsCore;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** HTTP handler for collection of views. */
public class ViewsHttpHandler implements HttpHandler {
    private final ViewsCore core = new ViewsCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            try {
                var views = core.listViews();
                ResponseUtil.ok(exchange, views);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        } else if ("POST".equalsIgnoreCase(method)) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            CreateViewCmd cmd = new CreateViewCmd(jr.optString("type"), jr.optString("name"));
            try {
                var dto = core.createView(cmd);
                ResponseUtil.created(exchange, dto);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }
        ResponseUtil.methodNotAllowed(exchange);
    }
}
