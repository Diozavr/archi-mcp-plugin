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

import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import ru.cinimex.archimatetool.mcp.core.types.CreateViewCmd;
import ru.cinimex.archimatetool.mcp.core.views.ViewsCore;
import ru.cinimex.archimatetool.mcp.http.ResponseUtil;
import ru.cinimex.archimatetool.mcp.json.JsonReader;
import ru.cinimex.archimatetool.mcp.util.McpLogger;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** HTTP handler for collection of views. */
public class ViewsHttpHandler implements HttpHandler {
    private final ViewsCore core = new ViewsCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        if ("GET".equalsIgnoreCase(method)) {
            McpLogger.logApiOperationCall("GET /views");
            try {
                var views = core.listViews();
                McpLogger.logApiOperationOutput("GET /views", 
                    java.util.Map.of("viewCount", views.size()));
                ResponseUtil.ok(exchange, views);
            } catch (CoreException ex) {
                McpLogger.logApiOperationError("GET /views", ex);
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        } else if ("POST".equalsIgnoreCase(method)) {
            McpLogger.logApiOperationCall("POST /views");
            JsonReader jr = JsonReader.fromExchange(exchange);
            CreateViewCmd cmd = new CreateViewCmd(jr.optString("type"), jr.optString("name"));
            McpLogger.logApiOperationInput("POST /views", 
                java.util.Map.of("type", cmd.type, "name", cmd.name));
            try {
                var dto = core.createView(cmd);
                McpLogger.logApiOperationOutput("POST /views", 
                    java.util.Map.of("createdViewId", dto.get("id")));
                ResponseUtil.created(exchange, dto);
            } catch (CoreException ex) {
                McpLogger.logApiOperationError("POST /views", ex);
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }
        ResponseUtil.methodNotAllowed(exchange);
    }
}
