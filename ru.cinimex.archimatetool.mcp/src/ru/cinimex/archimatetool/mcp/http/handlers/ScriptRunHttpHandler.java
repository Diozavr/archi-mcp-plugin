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
import java.util.HashMap;
import java.util.Map;

import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import ru.cinimex.archimatetool.mcp.core.script.ScriptingCore;
import ru.cinimex.archimatetool.mcp.core.script.ScriptRequest;
import ru.cinimex.archimatetool.mcp.core.script.ScriptResult;
import ru.cinimex.archimatetool.mcp.http.ResponseUtil;
import ru.cinimex.archimatetool.mcp.json.JsonReader;
import ru.cinimex.archimatetool.mcp.server.JacksonJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** HTTP handler for POST /script/run. */
public class ScriptRunHttpHandler implements HttpHandler {
    private final ScriptingCore core;

    public ScriptRunHttpHandler(ScriptingCore core) {
        this.core = core;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtil.methodNotAllowed(exchange);
            return;
        }
        if (!core.isPluginInstalled()) {
            ResponseUtil.notImplemented(exchange, "Not Implemented: install a compatible jArchi to enable /script APIs");
            return;
        }
        JsonReader jr = JsonReader.fromExchange(exchange);
        String engine = jr.optString("engine");
        String code = jr.optString("code");
        Integer timeoutMs = jr.optInt("timeoutMs");
        JsonNode bindingsNode = jr.optObject("bindings");
        Map<String, Object> bindings = null;
        if (bindingsNode != null) {
            bindings = JacksonJson.mapper().convertValue(bindingsNode, Map.class);
        }
        String modelId = jr.optString("modelId");
        ScriptRequest req = new ScriptRequest(engine, code, timeoutMs, bindings, modelId);
        try {
            ScriptResult result = core.run(req);
            Map<String, Object> resp = new HashMap<>();
            resp.put("ok", result.ok());
            if (result.result() != null) resp.put("result", result.result());
            if (result.stdout() != null) resp.put("stdout", result.stdout());
            if (result.stderr() != null) resp.put("stderr", result.stderr());
            resp.put("durationMs", result.durationMs());
            boolean hasStdErr = result.stderr() != null && !result.stderr().isEmpty();
            boolean hasStdoutMarker = result.stdout() != null && result.stdout().contains("[MCP ERROR]");
            if (hasStdErr || hasStdoutMarker) {
                String message = hasStdErr ? result.stderr() : result.stdout();
                ResponseUtil.unprocessable(exchange, message);
            } else {
                ResponseUtil.ok(exchange, resp);
            }
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }
}
