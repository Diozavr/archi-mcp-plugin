package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.archimatetool.mcp.json.JsonReader;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.server.ModelApi;
import com.archimatetool.mcp.service.ServiceRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RelationItemHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String id = exchange.getRequestURI().getPath().substring("/relations/".length());
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
        Object o = com.archimatetool.mcp.service.ServiceRegistry.activeModel().findById(model, id);
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            if (!(o instanceof com.archimatetool.model.IArchimateRelationship)) { ResponseUtil.notFound(exchange, "not found"); return; }
            ResponseUtil.ok(exchange, ModelApi.relationToDto((com.archimatetool.model.IArchimateRelationship)o));
            return;
        } else if ("PATCH".equalsIgnoreCase(method)) {
            if (!(o instanceof com.archimatetool.model.IArchimateRelationship)) { ResponseUtil.notFound(exchange, "not found"); return; }
            JsonReader jr = JsonReader.fromExchange(exchange);
            String name = jr.optString("name");
            com.archimatetool.model.IArchimateRelationship r = (com.archimatetool.model.IArchimateRelationship)o;
            if (name != null) {
                final String n = name;
                org.eclipse.swt.widgets.Display.getDefault().syncExec(() -> r.setName(n));
            }
            ResponseUtil.ok(exchange, ModelApi.relationToDto(r));
            return;
        } else if ("DELETE".equalsIgnoreCase(method)) {
            if (!(o instanceof com.archimatetool.model.IArchimateRelationship)) { ResponseUtil.notFound(exchange, "not found"); return; }
            boolean ok = com.archimatetool.mcp.service.ServiceRegistry.relations().deleteRelation((com.archimatetool.model.IArchimateRelationship)o);
            if (ok) { ResponseUtil.noContent(exchange); } else { ResponseUtil.badRequest(exchange, "cannot delete"); }
            return;
        }
        ResponseUtil.methodNotAllowed(exchange);
    }
}


