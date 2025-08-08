package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.archimatetool.mcp.server.ModelApi;
import com.archimatetool.mcp.service.ServiceRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ViewsHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
        var model = ServiceRegistry.activeModel().getActiveModel();
            if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
            var views = com.archimatetool.mcp.server.ModelApi.listViews(model).stream().map(ModelApi::viewToDto).collect(Collectors.toList());
            ResponseUtil.ok(exchange, views);
            return;
        } else if ("POST".equalsIgnoreCase(method)) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            String type = jr.optString("type");
            String name = jr.optString("name");
            if (type == null || name == null) { ResponseUtil.badRequest(exchange, "type and name required"); return; }
            if (!type.toLowerCase().contains("archimate")) { ResponseUtil.json(exchange, 400, Map.of("error","only archimate view supported in MVP")); return; }
        var model = com.archimatetool.mcp.service.ServiceRegistry.activeModel().getActiveModel();
            if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
            var view = ServiceRegistry.views().createArchimateView(model, name);
            ResponseUtil.created(exchange, ModelApi.viewToDto(view));
            return;
        }
        ResponseUtil.methodNotAllowed(exchange);
    }
}


