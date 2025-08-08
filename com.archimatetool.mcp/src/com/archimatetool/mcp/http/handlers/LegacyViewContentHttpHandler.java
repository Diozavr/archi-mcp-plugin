package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.server.ModelApi;
import com.archimatetool.mcp.service.ServiceRegistry;
import com.archimatetool.model.IDiagramModel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LegacyViewContentHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        var query = exchange.getRequestURI().getQuery();
        String id = null;
        if (query != null) {
            for (String p: query.split("&")) {
                int i=p.indexOf('=');
                if (i>0 && p.substring(0,i).equals("id")) {
                    id = URLDecoder.decode(p.substring(i+1), StandardCharsets.UTF_8);
                }
            }
        }
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
        Object obj = ServiceRegistry.activeModel().findById(model, id);
        if (!(obj instanceof IDiagramModel)) { ResponseUtil.notFound(exchange, "view not found"); return; }
        IDiagramModel v = (IDiagramModel) obj;
        ResponseUtil.ok(exchange, ModelApi.viewContentToDto(v));
    }
}


