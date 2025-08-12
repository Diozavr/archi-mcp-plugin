package com.archimatetool.mcp.http.handlers;

import java.io.IOException;

import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.relations.RelationsCore;
import com.archimatetool.mcp.core.types.CreateRelationCmd;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class RelationsHttpHandler implements HttpHandler {
    private final RelationsCore core = new RelationsCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("POST".equalsIgnoreCase(method)) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            CreateRelationCmd cmd = new CreateRelationCmd(jr.optString("type"), jr.optString("name"),
                    jr.optString("sourceId"), jr.optString("targetId"), jr.optString("folderId"));
            try {
                var dto = core.createRelation(cmd);
                ResponseUtil.created(exchange, dto);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }
        ResponseUtil.methodNotAllowed(exchange);
    }
}
