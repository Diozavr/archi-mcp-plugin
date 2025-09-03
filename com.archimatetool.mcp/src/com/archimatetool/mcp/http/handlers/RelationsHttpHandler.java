package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.relations.RelationsCore;
import com.archimatetool.mcp.core.types.CreateRelationItem;
import com.archimatetool.mcp.core.types.CreateRelationsCmd;
import com.archimatetool.mcp.core.types.DeleteRelationItem;
import com.archimatetool.mcp.core.types.DeleteRelationsCmd;
import com.archimatetool.mcp.core.types.UpdateRelationItem;
import com.archimatetool.mcp.core.types.UpdateRelationsCmd;
import com.archimatetool.mcp.core.types.GetRelationQuery;
import com.archimatetool.mcp.http.QueryParams;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** HTTP handler for batch relation operations. */
public class RelationsHttpHandler implements HttpHandler {
    private final RelationsCore core = new RelationsCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            handleGet(exchange);
            return;
        }
        if ("POST".equalsIgnoreCase(method)) {
            handlePost(exchange);
            return;
        }
        if ("PATCH".equalsIgnoreCase(method)) {
            handlePatch(exchange);
            return;
        }
        if ("DELETE".equalsIgnoreCase(method)) {
            handleDelete(exchange);
            return;
        }
        ResponseUtil.methodNotAllowed(exchange);
    }

    private void handleGet(HttpExchange exchange) throws IOException {
        QueryParams qp = QueryParams.from(exchange);
        List<String> ids = qp.all("ids");
        List<Map<String, Object>> res = new ArrayList<>();
        for (String id : ids) {
            try {
                res.add(core.getRelation(new GetRelationQuery(id)));
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
                return;
            }
        }
        ResponseUtil.ok(exchange, res);
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        JsonReader jr = JsonReader.fromExchange(exchange);
        List<CreateRelationItem> items = new ArrayList<>();
        if (jr.isArrayRoot()) {
            for (int i = 0; i < jr.arraySize(); i++) {
                JsonReader it = jr.at(i);
                items.add(new CreateRelationItem(it.optString("type"), it.optString("name"),
                        it.optString("sourceId"), it.optString("targetId"), it.optString("folderId"),
                        readMap(it.optObject("properties")), it.optString("documentation")));
            }
        }
        CreateRelationsCmd cmd = new CreateRelationsCmd(items);
        try {
            var dto = core.createRelations(cmd);
            ResponseUtil.created(exchange, dto);
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }

    private void handlePatch(HttpExchange exchange) throws IOException {
        JsonReader jr = JsonReader.fromExchange(exchange);
        List<UpdateRelationItem> items = new ArrayList<>();
        if (jr.isArrayRoot()) {
            for (int i = 0; i < jr.arraySize(); i++) {
                JsonReader it = jr.at(i);
                items.add(new UpdateRelationItem(it.optString("id"), it.optString("name"), it.optString("type"),
                        readMap(it.optObject("properties")), it.optString("documentation")));
            }
        }
        UpdateRelationsCmd cmd = new UpdateRelationsCmd(items);
        try {
            var dto = core.updateRelations(cmd);
            ResponseUtil.ok(exchange, dto);
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        JsonReader jr = JsonReader.fromExchange(exchange);
        List<DeleteRelationItem> items = new ArrayList<>();
        if (jr.isArrayRoot()) {
            for (int i = 0; i < jr.arraySize(); i++) {
                JsonReader it = jr.at(i);
                items.add(new DeleteRelationItem(it.optString("id")));
            }
        }
        DeleteRelationsCmd cmd = new DeleteRelationsCmd(items);
        try {
            var dto = core.deleteRelations(cmd);
            ResponseUtil.ok(exchange, dto);
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }

    private Map<String, String> readMap(JsonNode node) {
        if (node == null) return null;
        Map<String, String> m = new HashMap<>();
        node.fields().forEachRemaining(e -> m.put(e.getKey(), e.getValue().asText()));
        return m;
    }
}

