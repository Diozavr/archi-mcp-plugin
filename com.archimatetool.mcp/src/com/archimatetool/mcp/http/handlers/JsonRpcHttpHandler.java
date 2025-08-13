package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.archimatetool.mcp.core.elements.ElementsCore;
import com.archimatetool.mcp.core.errors.BadRequestException;
import com.archimatetool.mcp.core.errors.ConflictException;
import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.errors.NotFoundException;
import com.archimatetool.mcp.core.errors.UnprocessableException;
import com.archimatetool.mcp.core.folders.FoldersCore;
import com.archimatetool.mcp.core.model.ModelCore;
import com.archimatetool.mcp.core.relations.RelationsCore;
import com.archimatetool.mcp.core.search.SearchCore;
import com.archimatetool.mcp.core.views.ViewsCore;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.server.JacksonJson;
import com.archimatetool.mcp.server.JsonUtil;
import com.archimatetool.mcp.server.tools.Tool;
import com.archimatetool.mcp.server.tools.ToolRegistry;
import com.archimatetool.mcp.server.tools.Param;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * JSON-RPC 2.0 handler backed by a ToolRegistry.
 */
public class JsonRpcHttpHandler implements HttpHandler {

    private final ToolRegistry registry;

    /** Creates handler with default core implementations. */
    public JsonRpcHttpHandler() {
        this(ToolRegistry.createDefault(new ElementsCore(), new RelationsCore(), new ViewsCore(),
                new SearchCore(), new FoldersCore(), new ModelCore()));
    }

    /** Creates handler with injected registry. */
    public JsonRpcHttpHandler(ToolRegistry registry) {
        this.registry = registry;
    }

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
            writeError(exchange, -32700, "parse error", null, null);
            return;
        }
        if (root.isArray()) {
            List<Map<String, Object>> responses = new ArrayList<>();
            for (JsonNode node : root) {
                responses.add(processRequest(node));
            }
            JsonUtil.writeJson(exchange, 200, responses);
        } else {
            Map<String, Object> resp = processRequest(root);
            JsonUtil.writeJson(exchange, 200, resp);
        }
    }

    private Map<String, Object> processRequest(JsonNode node) {
        JsonNode idNode = node.get("id");
        Object id = idNode != null && !idNode.isNull()
                ? JacksonJson.mapper().convertValue(idNode, Object.class)
                : null;
        if (!"2.0".equals(node.path("jsonrpc").asText()) || !node.has("method")) {
            return error(-32600, "invalid request", id, null);
        }
        String method = node.get("method").asText();
        JsonNode params = node.get("params");
        try {
            Object result;
            switch (method) {
            case "status/ping":
                result = Map.of("ok", true);
                break;
            case "tools/list":
                List<Map<String, Object>> meta = new ArrayList<>();
                for (Tool t : registry.all()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", t.getName());
                    m.put("description", t.getDescription());
                    Map<String, Object> ps = new HashMap<>();
                    for (Param p : t.getParams().values()) {
                        Map<String, Object> pd = new HashMap<>();
                        pd.put("type", p.type);
                        pd.put("description", p.description);
                        pd.put("required", p.required);
                        ps.put(p.name, pd);
                    }
                    m.put("params", ps);
                    meta.add(m);
                }
                result = meta;
                break;
            case "tools/call":
                if (params == null || !params.has("name")) {
                    throw new BadRequestException("missing name");
                }
                String name = params.get("name").asText();
                Tool tool = registry.get(name);
                if (tool == null) {
                    throw new BadRequestException("unknown tool");
                }
                JsonNode args = params.get("args");
                result = tool.getInvoker().invoke(args);
                break;
            default:
                return error(-32601, "method not found", id, null);
            }
            return result(result, id);
        } catch (CoreException ex) {
            return error(mapCoreError(ex), ex.getMessage(), id, null);
        } catch (Exception ex) {
            return error(-32603, "internal error", id, null);
        }
    }

    private Map<String, Object> result(Object result, Object id) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("result", result);
        resp.put("id", id);
        return resp;
    }

    private Map<String, Object> error(int code, String msg, Object id, Object data) {
        Map<String, Object> err = new HashMap<>();
        err.put("code", code);
        err.put("message", msg);
        if (data != null) {
            err.put("data", data);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("jsonrpc", "2.0");
        resp.put("error", err);
        resp.put("id", id);
        return resp;
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

    private void writeError(HttpExchange exchange, int code, String msg, JsonNode idNode, Object data) throws IOException {
        Object id = idNode != null && !idNode.isNull()
                ? JacksonJson.mapper().convertValue(idNode, Object.class)
                : null;
        Map<String, Object> err = error(code, msg, id, data);
        JsonUtil.writeJson(exchange, 200, err);
    }
}
