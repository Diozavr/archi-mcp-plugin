package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.archimatetool.mcp.core.elements.ElementsCore;
import com.archimatetool.mcp.core.errors.BadRequestException;
import com.archimatetool.mcp.core.errors.ConflictException;
import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.errors.NotFoundException;
import com.archimatetool.mcp.core.errors.UnprocessableException;
import com.archimatetool.mcp.core.model.ModelCore;
import com.archimatetool.mcp.core.types.CreateElementCmd;
import com.archimatetool.mcp.core.types.GetElementQuery;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.server.JacksonJson;
import com.archimatetool.mcp.server.JsonUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Basic JSON-RPC 2.0 handler delegating to the core layer.
 */
public class JsonRpcHttpHandler implements HttpHandler {

    private final ElementsCore elements = new ElementsCore();
    private final ModelCore model = new ModelCore();

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
            writeError(exchange, -32700, "parse error", null);
            return;
        }
        JsonNode idNode = root.get("id");
        if (!"2.0".equals(root.path("jsonrpc").asText()) || !root.has("method")) {
            writeError(exchange, -32600, "invalid request", idNode);
            return;
        }
        String method = root.get("method").asText();
        JsonNode params = root.get("params");
        try {
            Object result;
            switch (method) {
            case "elements.create": {
                CreateElementCmd cmd = new CreateElementCmd();
                if (params != null) {
                    cmd.type = params.path("type").asText(null);
                    cmd.name = params.path("name").asText(null);
                    if (params.has("folderId")) {
                        cmd.folderId = params.path("folderId").asText(null);
                    }
                }
                result = elements.createElement(cmd);
                break;
            }
            case "elements.get": {
                GetElementQuery q = new GetElementQuery();
                if (params != null) {
                    q.id = params.path("id").asText(null);
                    if (params.has("includeRelations")) {
                        q.includeRelations = params.path("includeRelations").asBoolean();
                    }
                    if (params.has("includeElements")) {
                        q.includeElements = params.path("includeElements").asBoolean();
                    }
                }
                result = elements.getElement(q);
                break;
            }
            case "model.save": {
                result = model.saveModel();
                break;
            }
            default:
                writeError(exchange, -32601, "method not found", idNode);
                return;
            }
            writeResult(exchange, result, idNode);
        } catch (CoreException ex) {
            writeError(exchange, mapCoreError(ex), ex.getMessage(), idNode);
        } catch (Exception ex) {
            writeError(exchange, -32603, "internal error", idNode);
        }
    }

    private int mapCoreError(CoreException ex) {
        if (ex instanceof BadRequestException) {
            return -32602;
        } else if (ex instanceof NotFoundException) {
            return -32004;
        } else if (ex instanceof ConflictException) {
            return -409;
        } else if (ex instanceof UnprocessableException) {
            return -422;
        }
        return -32603;
    }

    private void writeResult(HttpExchange exchange, Object result, JsonNode idNode) throws IOException {
        Object id = idNode != null && !idNode.isNull()
                ? JacksonJson.mapper().convertValue(idNode, Object.class)
                : null;
        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("result", result);
        resp.put("id", id);
        JsonUtil.writeJson(exchange, 200, resp);
    }

    private void writeError(HttpExchange exchange, int code, String msg, JsonNode idNode) throws IOException {
        Object id = idNode != null && !idNode.isNull()
                ? JacksonJson.mapper().convertValue(idNode, Object.class)
                : null;
        Map<String, Object> error = new HashMap<>();
        error.put("code", code);
        error.put("message", msg);
        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("error", error);
        resp.put("id", id);
        JsonUtil.writeJson(exchange, 200, resp);
    }
}

