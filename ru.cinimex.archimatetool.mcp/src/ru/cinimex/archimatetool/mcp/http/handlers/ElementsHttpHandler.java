/*
 * Copyright 2025 Cinimex
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.cinimex.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.cinimex.archimatetool.mcp.core.elements.ElementsCore;
import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import ru.cinimex.archimatetool.mcp.core.types.CreateElementItem;
import ru.cinimex.archimatetool.mcp.core.types.CreateElementsCmd;
import ru.cinimex.archimatetool.mcp.core.types.DeleteElementItem;
import ru.cinimex.archimatetool.mcp.core.types.DeleteElementsCmd;
import ru.cinimex.archimatetool.mcp.core.types.GetElementQuery;
import ru.cinimex.archimatetool.mcp.core.types.UpdateElementItem;
import ru.cinimex.archimatetool.mcp.core.types.UpdateElementsCmd;
import ru.cinimex.archimatetool.mcp.http.QueryParams;
import ru.cinimex.archimatetool.mcp.http.ResponseUtil;
import ru.cinimex.archimatetool.mcp.json.JsonReader;
import ru.cinimex.archimatetool.mcp.util.McpLogger;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** HTTP handler for batch element operations. */
public class ElementsHttpHandler implements HttpHandler {
    private final ElementsCore core = new ElementsCore();

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
        McpLogger.logApiOperationCall("GET /elements");
        
        QueryParams qp = QueryParams.from(exchange);
        List<String> ids = qp.all("ids");
        String include = qp.first("include");
        if (include == null) include = qp.first("with");
        boolean includeRelations = false;
        if (include != null) {
            for (String s : include.split(",")) {
                if (s.trim().equalsIgnoreCase("relations")) {
                    includeRelations = true;
                    break;
                }
            }
        }
        boolean includeElements = qp.getBool("includeElements", false);
        
        Map<String, Object> inputParams = Map.of(
            "ids", ids,
            "includeRelations", includeRelations,
            "includeElements", includeElements
        );
        McpLogger.logApiOperationInput("GET /elements", inputParams);
        
        List<Map<String, Object>> res = new ArrayList<>();
        for (String id : ids) {
            GetElementQuery q = new GetElementQuery(id, includeRelations, includeElements);
            try {
                res.add(core.getElement(q));
            } catch (CoreException ex) {
                McpLogger.logApiOperationError("GET /elements", ex);
                ResponseUtil.handleCoreException(exchange, ex);
                return;
            }
        }
        
        McpLogger.logApiOperationOutput("GET /elements", Map.of("resultCount", res.size()));
        ResponseUtil.ok(exchange, res);
    }

    private void handlePost(HttpExchange exchange) throws IOException {
        McpLogger.logApiOperationCall("POST /elements");
        
        JsonReader jr = JsonReader.fromExchange(exchange);
        List<CreateElementItem> items = new ArrayList<>();
        if (jr.isArrayRoot()) {
            for (int i = 0; i < jr.arraySize(); i++) {
                JsonReader it = jr.at(i);
                items.add(new CreateElementItem(null, it.optString("type"), it.optString("name"),
                        it.optString("folderId"), readMap(it.optObject("properties")), it.optString("documentation")));
            }
        }
        
        McpLogger.logApiOperationInput("POST /elements", Map.of("itemCount", items.size()));
        
        CreateElementsCmd cmd = new CreateElementsCmd(items);
        try {
            var dto = core.createElements(cmd);
            McpLogger.logApiOperationOutput("POST /elements", Map.of("createdCount", dto.size()));
            ResponseUtil.created(exchange, dto);
        } catch (CoreException ex) {
            McpLogger.logApiOperationError("POST /elements", ex);
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }

    private void handlePatch(HttpExchange exchange) throws IOException {
        JsonReader jr = JsonReader.fromExchange(exchange);
        List<UpdateElementItem> items = new ArrayList<>();
        if (jr.isArrayRoot()) {
            for (int i = 0; i < jr.arraySize(); i++) {
                JsonReader it = jr.at(i);
                items.add(new UpdateElementItem(it.optString("id"), it.optString("name"), it.optString("type"),
                        it.optString("folderId"), readMap(it.optObject("properties")), it.optString("documentation")));
            }
        }
        UpdateElementsCmd cmd = new UpdateElementsCmd(items);
        try {
            var dto = core.updateElements(cmd);
            ResponseUtil.ok(exchange, dto);
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }

    private void handleDelete(HttpExchange exchange) throws IOException {
        JsonReader jr = JsonReader.fromExchange(exchange);
        List<DeleteElementItem> items = new ArrayList<>();
        if (jr.isArrayRoot()) {
            for (int i = 0; i < jr.arraySize(); i++) {
                JsonReader it = jr.at(i);
                items.add(new DeleteElementItem(it.optString("id")));
            }
        }
        DeleteElementsCmd cmd = new DeleteElementsCmd(items);
        try {
            var dto = core.deleteElements(cmd);
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

