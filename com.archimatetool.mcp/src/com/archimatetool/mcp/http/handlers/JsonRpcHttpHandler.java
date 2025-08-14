package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.server.JacksonJson;
import com.archimatetool.mcp.server.JsonUtil;
import com.archimatetool.mcp.server.tools.Tool;
import com.archimatetool.mcp.server.tools.ToolParam;
import com.archimatetool.mcp.server.tools.ToolRegistry;
import com.archimatetool.mcp.core.errors.BadRequestException;
import com.archimatetool.mcp.core.errors.ConflictException;
import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.errors.NotFoundException;
import com.archimatetool.mcp.core.errors.UnprocessableException;
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
        if (!"POST".equals(exchange.getRequestMethod())) {
            ResponseUtil.methodNotAllowed(exchange);
            return;
        }
        JsonNode root;
        try {
            root = JacksonJson.readTree(exchange.getRequestBody());
        } catch (IOException ex) {
            JsonUtil.writeJson(exchange, 200, error(null, -32700, "parse error", null));
            return;
        }
        if (root.isArray()) {
            List<Object> responses = new ArrayList<>();
            for (JsonNode node : root) {
                Object resp = process(node);
                if (resp != null) {
                    responses.add(resp);
                }
            }
            if (responses.isEmpty()) {
                exchange.sendResponseHeaders(204, -1);
            } else {
                JsonUtil.writeJson(exchange, 200, responses);
            }
        } else {
            Object resp = process(root);
            if (resp == null) {
                exchange.sendResponseHeaders(204, -1);
            } else {
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
        Tool tool = ToolRegistry.get(method);
        if (tool == null || tool.getInvoker() == null) {
            return isNotification ? null : error(idNode, -32601, "method '"+method+"' not found", null);
        }
        Map<String, Object> params = Collections.emptyMap();
        JsonNode paramsNode = node.get("params");
        if (paramsNode != null) {
            if (!paramsNode.isObject()) {
                return isNotification ? null : error(idNode, -32602, "invalid params", Map.of("error", "params must be object"));
            }
            params = JacksonJson.mapper().convertValue(paramsNode, Map.class);
        }
        try {
            params = validateParams(tool, params);
        } catch (ParamException pe) {
            return isNotification ? null : error(idNode, -32602, "invalid params", Map.of("error", pe.getMessage()));
        }
        try {
            Object result = tool.getInvoker().invoke(params);
            return isNotification ? null : success(idNode, result);
        } catch (CoreException ce) {
            return isNotification ? null : error(idNode, mapCoreException(ce), ce.getMessage(), null);
        } catch (Exception ex) {
            return isNotification ? null : error(idNode, -32603, "internal error", null);
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
        }
        return -32603;
    }

    private static class ParamException extends Exception {
        ParamException(String msg) {
            super(msg);
        }
    }
}

