package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import com.archimatetool.mcp.http.QueryParams;
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
        QueryParams qp = QueryParams.from(exchange);
        String id = qp.first("id");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
        Object obj = ServiceRegistry.activeModel().findById(model, id);
        if (!(obj instanceof IDiagramModel)) { ResponseUtil.notFound(exchange, "view not found"); return; }
        IDiagramModel v = (IDiagramModel) obj;
        ResponseUtil.ok(exchange, ModelApi.viewContentToDto(v));
    }
}


