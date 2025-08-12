package com.archimatetool.mcp.http.handlers;

import java.io.IOException;

import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.relations.RelationsCore;
import com.archimatetool.mcp.core.types.DeleteRelationCmd;
import com.archimatetool.mcp.core.types.GetRelationQuery;
import com.archimatetool.mcp.core.types.UpdateRelationCmd;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RelationItemHttpHandler implements HttpHandler {
    private final RelationsCore core = new RelationsCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String id = exchange.getRequestURI().getPath().substring("/relations/".length());
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            GetRelationQuery q = new GetRelationQuery(id);
            try {
                var dto = core.getRelation(q);
                ResponseUtil.ok(exchange, dto);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        } else if ("PATCH".equalsIgnoreCase(method)) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            UpdateRelationCmd cmd = new UpdateRelationCmd(id, jr.optString("name"));
            try {
                var dto = core.updateRelation(cmd);
                ResponseUtil.ok(exchange, dto);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        } else if ("DELETE".equalsIgnoreCase(method)) {
            DeleteRelationCmd cmd = new DeleteRelationCmd(id);
            try {
                core.deleteRelation(cmd);
                ResponseUtil.noContent(exchange);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }
        ResponseUtil.methodNotAllowed(exchange);
    }
}
