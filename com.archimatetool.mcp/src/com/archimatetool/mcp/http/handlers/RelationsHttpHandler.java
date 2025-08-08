package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.Map;

import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.archimatetool.mcp.server.ModelApi;
import com.archimatetool.mcp.service.ServiceRegistry;
import com.archimatetool.mcp.util.StringCaseUtil;
import com.archimatetool.model.IArchimateElement;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RelationsHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if (!"POST".equalsIgnoreCase(method)) { ResponseUtil.methodNotAllowed(exchange); return; }
        JsonReader jr = JsonReader.fromExchange(exchange);
        String type = jr.optString("type");
        String name = jr.optString("name");
        String sourceId = jr.optString("sourceId");
        String targetId = jr.optString("targetId");
        String folderId = jr.optString("folderId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
        Object so = com.archimatetool.mcp.service.ServiceRegistry.activeModel().findById(model, sourceId);
        Object to = com.archimatetool.mcp.service.ServiceRegistry.activeModel().findById(model, targetId);
        if (!(so instanceof IArchimateElement) || !(to instanceof IArchimateElement)) { ResponseUtil.notFound(exchange, "source or target not found"); return; }
        try {
            var rel = ServiceRegistry.relations().createRelation(model, StringCaseUtil.toCamelCase(type), name != null ? name : "", (IArchimateElement)so, (IArchimateElement)to, folderId);
            ResponseUtil.created(exchange, ModelApi.relationToDto(rel));
        } catch (Exception ex) { ResponseUtil.json(exchange, 400, Map.of("error", ex.getMessage())); }
    }
}


