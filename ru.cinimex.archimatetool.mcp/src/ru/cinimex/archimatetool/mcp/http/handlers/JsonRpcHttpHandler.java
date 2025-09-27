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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.cinimex.archimatetool.mcp.http.ResponseUtil;
import ru.cinimex.archimatetool.mcp.server.JacksonJson;
import ru.cinimex.archimatetool.mcp.server.JsonUtil;
import ru.cinimex.archimatetool.mcp.server.tools.Tool;
import ru.cinimex.archimatetool.mcp.server.tools.ToolParam;
import ru.cinimex.archimatetool.mcp.server.tools.ToolRegistry;
import ru.cinimex.archimatetool.mcp.util.McpLogger;
import ru.cinimex.archimatetool.mcp.core.errors.BadRequestException;
import ru.cinimex.archimatetool.mcp.core.errors.ConflictException;
import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import ru.cinimex.archimatetool.mcp.core.errors.NotFoundException;
import ru.cinimex.archimatetool.mcp.core.errors.NotImplementedException;
import ru.cinimex.archimatetool.mcp.core.errors.UnprocessableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Minimal JSON-RPC 2.0 handler supporting single and batch requests,
 * dispatching calls via the tool registry.
 */
public class JsonRpcHttpHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        // Log incoming request at debug level only
        if (McpLogger.isDebugEnabled()) {
            StringBuilder headerDetails = new StringBuilder();
            exchange.getRequestHeaders().forEach((key, values) -> 
                headerDetails.append(key).append("=").append(String.join(",", values)).append("; "));
                
            McpLogger.logOperationCall("JsonRpcHttpHandler", 
                "method=" + exchange.getRequestMethod() + 
                ", uri=" + exchange.getRequestURI() + 
                ", headers=[" + headerDetails.toString() + "]");
        }
            
        if (!"POST".equals(exchange.getRequestMethod())) {
            ResponseUtil.methodNotAllowed(exchange);
            return;
        }
        
        // Read request body
        String requestBody;
        try {
            java.io.InputStream inputStream = exchange.getRequestBody();
            byte[] bodyBytes = inputStream.readAllBytes();
            requestBody = new String(bodyBytes, java.nio.charset.StandardCharsets.UTF_8);
            
            // Log raw request only at debug level
            if (McpLogger.isDebugEnabled()) {
                McpLogger.logOperationCall("JsonRpcHttpHandler", "Raw request: " + requestBody);
            }
        } catch (Exception ex) {
            McpLogger.logOperationError("JsonRpcHttpHandler", ex);
            JsonUtil.writeJson(exchange, 200, error(null, -32700, "failed to read request body: " + ex.getMessage(), null));
            return;
        }
        
        JsonNode root;
        try {
            root = JacksonJson.mapper().readTree(requestBody);
            
            // Log basic request info at info level
            JsonNode methodNode = root.get("method");
            if (methodNode != null) {
                McpLogger.logOperationCall("JsonRPC", methodNode.asText());
            }
            
            // Log parsed JSON only at debug level
            if (McpLogger.isDebugEnabled()) {
                McpLogger.logOperationCall("JsonRpcHttpHandler", "Parsed JSON: " + root.toString());
            }
        } catch (IOException ex) {
            McpLogger.logOperationError("JsonRpcHttpHandler", ex);
            JsonUtil.writeJson(exchange, 200, error(null, -32700, "parse error", null));
            return;
        }
        if (root.isArray()) {
            McpLogger.logOperationCall("JsonRPC", "batch request (" + root.size() + " items)");
            List<Object> responses = new ArrayList<>();
            for (JsonNode node : root) {
                Object resp = process(node);
                if (resp != null) {
                    responses.add(resp);
                }
            }
            if (responses.isEmpty()) {
                // MCP 2025-06-18: for batches consisting only of notifications/responses return 202 Accepted with no body
                exchange.sendResponseHeaders(202, -1);
            } else {
                JsonUtil.writeJson(exchange, 200, responses);
            }
        } else {
            Object resp = process(root);
            if (resp == null) {
                if (McpLogger.isDebugEnabled()) {
                    McpLogger.logOperationOutput("JsonRpcHttpHandler", "empty response");
                }
                // MCP 2025-06-18: for notifications return 202 Accepted with no body
                exchange.sendResponseHeaders(202, -1);
            } else {
                if (McpLogger.isDebugEnabled()) {
                    String responseJson = JacksonJson.mapper().writeValueAsString(resp);
                    McpLogger.logOperationCall("JsonRpcHttpHandler", "Sending response: " + responseJson);
                }
                JsonUtil.writeJson(exchange, 200, resp);
            }
        }
    }

    private Object process(JsonNode node) {
        if (node == null || !node.isObject()) {
            return error(null, -32600, "invalid request", null);
        }
        JsonNode idNode = node.get("id");
        if (idNode != null && idNode.isNull()) {
            // id explicitly null is an invalid request (not a notification)
            return error(null, -32600, "invalid request", null);
        }
        boolean isNotification = idNode == null;
        String jsonrpc = node.path("jsonrpc").asText();
        if (!"2.0".equals(jsonrpc) || !node.has("method")) {
            return isNotification ? null : error(idNode, -32600, "invalid request", null);
        }
        String method = node.path("method").asText();
        JsonNode paramsNode = node.get("params");
        Map<String, Object> params = paramsNode != null && paramsNode.isObject()
                ? JacksonJson.mapper().convertValue(paramsNode, Map.class)
                : Collections.emptyMap();
        switch (method) {
            case "initialize":
                Map<String, Object> result = Map.of(
                    "protocolVersion", "2025-06-18",
                    "serverInfo", Map.of("name", "Archi MCP", "version", "0.1.0"),
                    "capabilities", Map.of(
                        "tools", Map.of("listChanged", Boolean.FALSE),
                        // Cursor ожидает объекты для prompts/resources, а не boolean
                        "prompts", Map.of(),
                        "resources", Map.of(),
                        "logging", Map.of("levels", Arrays.asList("info", "warn", "error"))
                    )
                );
                return isNotification ? null : success(idNode, result);
            case "notifications/initialized":
                return isNotification ? null : success(idNode, Collections.emptyMap());
            case "tools/list": {
                Map<String, Object> payload = new HashMap<>();
                payload.put("tools", ToolRegistry.describeAll());
                payload.put("usage", String.join("\n",
                    "JSON-RPC 2.0 endpoint: POST /mcp",
                    "Call tools via method 'tools/call' and include params in 'arguments' (or legacy 'args').",
                    "Request example:",
                    "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/call\",\"params\":{\"name\":\"list_views\",\"arguments\":{}}}",
                    "Do not include a 'result' field in requests (it appears only in responses).",
                    "Parameters use strict types and snake_case names shown in inputSchema."));
                return isNotification ? null : success(idNode, payload);
            }
            case "tools/call": {
                Object nameObj = params.get("name");
                if (!(nameObj instanceof String)) {
                    return isNotification ? null : error(idNode, -32602, "invalid params", Map.of("error", "missing name"));
                }
                String name = (String) nameObj;
                // Support both "arguments" (MCP default) and legacy "args"
                @SuppressWarnings("unchecked") Map<String, Object> args =
                        params.get("arguments") instanceof Map ? (Map<String, Object>) params.get("arguments") :
                        (params.get("args") instanceof Map ? (Map<String, Object>) params.get("args") : Collections.emptyMap());
                Tool tool = ToolRegistry.get(name);
                if (tool == null || tool.getInvoker() == null) {
                    return isNotification ? null : error(idNode, -32601, "method '" + name + "' not found", null);
                }
                try {
                    args = validateParams(tool, args);
                } catch (ParamException pe) {
                    McpLogger.logOperationError(name, pe);
                    return isNotification ? null
                            : error(idNode, -32602, "invalid params", Map.of("error", pe.getMessage()));
                }
                try {
                    // Log input data at debug level
                    if (McpLogger.isDebugEnabled()) {
                        McpLogger.logOperationInput(name, args);
                    }
                    
                    Object callResult = tool.getInvoker().invoke(args);
                    
                    // Log output data at debug level
                    if (McpLogger.isDebugEnabled()) {
                        McpLogger.logOperationOutput(name, callResult);
                    }
                    
                    // Wrap result in MCP tool response format
                    Map<String, Object> mcpResult = Map.of(
                        "content", java.util.List.of(
                            Map.of(
                                "type", "text",
                                "text", JacksonJson.mapper().writeValueAsString(callResult)
                            )
                        )
                    );
                    
                    return isNotification ? null : success(idNode, mcpResult);
                } catch (CoreException ce) {
                    McpLogger.logOperationError(name, ce);
                    return isNotification ? null : error(idNode, mapCoreException(ce), ce.getMessage(), null);
                } catch (Exception ex) {
                    McpLogger.logOperationError(name, ex);
                    return isNotification ? null : error(idNode, -32603, "internal error", null);
                }
            }
            case "prompts/list": {
                Map<String, Object> payload = Map.of("prompts", Collections.emptyList());
                return isNotification ? null : success(idNode, payload);
            }
            case "resources/list": {
                Map<String, Object> payload = Map.of("resources", Collections.emptyList());
                return isNotification ? null : success(idNode, payload);
            }
            case "logging/setLevel": {
                // Accept and configure logging level
                Object lvl = params.get("level");
                String level = lvl instanceof String ? (String) lvl : "info";
                
                // Configure logging levels based on the requested level
                switch (level.toLowerCase()) {
                    case "debug":
                        McpLogger.setInfoEnabled(true);
                        McpLogger.setDebugEnabled(true);
                        break;
                    case "info":
                        McpLogger.setInfoEnabled(true);
                        McpLogger.setDebugEnabled(false);
                        break;
                    case "warn":
                    case "error":
                        McpLogger.setInfoEnabled(false);
                        McpLogger.setDebugEnabled(false);
                        break;
                    default:
                        // Default to info level
                        McpLogger.setInfoEnabled(true);
                        McpLogger.setDebugEnabled(false);
                        level = "info";
                        break;
                }
                
                if (McpLogger.isDebugEnabled()) {
                    McpLogger.logOperationCall("logging/setLevel", "level=" + level);
                }
                // Inspector schema does not accept custom fields here; return empty result
                return isNotification ? null : success(idNode, Collections.emptyMap());
            }
            default:
                return isNotification ? null : error(idNode, -32601, "method '" + method + "' not found", null);
        }
    }

    private Map<String, Object> success(JsonNode idNode, Object result) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("result", result);
        resp.put("id", idNode != null && !idNode.isNull()
                ? JacksonJson.mapper().convertValue(idNode, Object.class)
                : null);
        return resp;
    }

    private Map<String, Object> error(JsonNode idNode, int code, String message, Object data) {
        Map<String, Object> err = new HashMap<>();
        err.put("code", code);
        err.put("message", message);
        if (data != null) {
            err.put("data", data);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("error", err);
        resp.put("id", idNode != null && !idNode.isNull()
                ? JacksonJson.mapper().convertValue(idNode, Object.class)
                : null);
        return resp;
    }

    private Map<String, Object> validateParams(Tool tool, Map<String, Object> params) throws ParamException {
        Map<String, Object> validated = new HashMap<>(params);
        for (ToolParam tp : tool.getParams()) {
            Object value = validated.get(tp.getName());
            if (value == null) {
                if (tp.isRequired()) {
                    throw new ParamException("missing required param '" + tp.getName() + "'");
                }
                if (tp.getDefaultValue() != null) {
                    validated.put(tp.getName(), tp.getDefaultValue());
                }
            } else if (!isTypeValid(tp.getType(), value)) {
                throw new ParamException("param '" + tp.getName() + "' must be " + tp.getType());
            }
        }
        return validated;
    }

    private boolean isTypeValid(String type, Object value) {
        switch (type) {
            case "string":
                return value instanceof String;
            case "integer":
                return value instanceof Integer || value instanceof Long;
            case "number":
                return value instanceof Number;
            case "boolean":
                return value instanceof Boolean;
            default:
                return true;
        }
    }

    private int mapCoreException(CoreException ex) {
        if (ex instanceof BadRequestException) {
            return -32001;
        } else if (ex instanceof NotFoundException) {
            return -32004;
        } else if (ex instanceof ConflictException) {
            return -32009;
        } else if (ex instanceof UnprocessableException) {
            return -32022;
        } else if (ex instanceof NotImplementedException) {
            return -32051;
        }
        return -32603;
    }

    private static class ParamException extends Exception {
        ParamException(String msg) {
            super(msg);
        }
    }
}

