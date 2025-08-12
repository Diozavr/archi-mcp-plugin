package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
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
import com.archimatetool.mcp.core.types.AddElementToViewCmd;
import com.archimatetool.mcp.core.types.AddRelationToViewCmd;
import com.archimatetool.mcp.core.types.CreateElementCmd;
import com.archimatetool.mcp.core.types.CreateRelationCmd;
import com.archimatetool.mcp.core.types.CreateViewCmd;
import com.archimatetool.mcp.core.types.DeleteElementCmd;
import com.archimatetool.mcp.core.types.DeleteRelationCmd;
import com.archimatetool.mcp.core.types.DeleteViewCmd;
import com.archimatetool.mcp.core.types.DeleteViewObjectCmd;
import com.archimatetool.mcp.core.types.EnsureFolderCmd;
import com.archimatetool.mcp.core.types.GetElementQuery;
import com.archimatetool.mcp.core.types.GetRelationQuery;
import com.archimatetool.mcp.core.types.GetViewContentQuery;
import com.archimatetool.mcp.core.types.GetViewImageQuery;
import com.archimatetool.mcp.core.types.GetViewQuery;
import com.archimatetool.mcp.core.types.ListElementRelationsQuery;
import com.archimatetool.mcp.core.types.MoveViewObjectCmd;
import com.archimatetool.mcp.core.types.SearchQuery;
import com.archimatetool.mcp.core.types.UpdateElementCmd;
import com.archimatetool.mcp.core.types.UpdateRelationCmd;
import com.archimatetool.mcp.core.types.UpdateViewObjectBoundsCmd;
import com.archimatetool.mcp.core.views.ViewsCore;
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

    private final ElementsCore elements;
    private final RelationsCore relations;
    private final ViewsCore views;
    private final SearchCore search;
    private final FoldersCore folders;
    private final ModelCore model;

    /** Creates handler with default core implementations. */
    public JsonRpcHttpHandler() {
        this(new ElementsCore(), new RelationsCore(), new ViewsCore(),
                new SearchCore(), new FoldersCore(), new ModelCore());
    }

    /**
     * Creates handler with injected core instances, useful for testing.
     */
    public JsonRpcHttpHandler(ElementsCore elements, RelationsCore relations,
            ViewsCore views, SearchCore search, FoldersCore folders,
            ModelCore model) {
        this.elements = elements;
        this.relations = relations;
        this.views = views;
        this.search = search;
        this.folders = folders;
        this.model = model;
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
                String type = params != null ? params.path("type").asText(null) : null;
                String name = params != null ? params.path("name").asText(null) : null;
                String folderId = params != null && params.has("folderId") ? params.path("folderId").asText(null) : null;
                result = elements.createElement(new CreateElementCmd(type, name, folderId));
                break;
            }
            case "elements.get": {
                String id = params != null ? params.path("id").asText(null) : null;
                boolean incRel = params != null && params.path("includeRelations").asBoolean(false);
                boolean incEl = params != null && params.path("includeElements").asBoolean(false);
                result = elements.getElement(new GetElementQuery(id, incRel, incEl));
                break;
            }
            case "elements.update": {
                String id = params != null ? params.path("id").asText(null) : null;
                String name = params != null ? params.path("name").asText(null) : null;
                result = elements.updateElement(new UpdateElementCmd(id, name));
                break;
            }
            case "elements.delete": {
                String id = params != null ? params.path("id").asText(null) : null;
                elements.deleteElement(new DeleteElementCmd(id));
                result = Map.of("deleted", true);
                break;
            }
            case "elements.listRelations": {
                String id = params != null ? params.path("id").asText(null) : null;
                String direction = params != null ? params.path("direction").asText(null) : null;
                boolean includeElements = params != null && params.path("includeElements").asBoolean(false);
                result = elements.listRelations(new ListElementRelationsQuery(id, direction, includeElements));
                break;
            }
            case "relations.create": {
                String type = params != null ? params.path("type").asText(null) : null;
                String name = params != null ? params.path("name").asText(null) : null;
                String sourceId = params != null ? params.path("sourceId").asText(null) : null;
                String targetId = params != null ? params.path("targetId").asText(null) : null;
                String folderId = params != null && params.has("folderId") ? params.path("folderId").asText(null) : null;
                result = relations.createRelation(new CreateRelationCmd(type, name, sourceId, targetId, folderId));
                break;
            }
            case "relations.get": {
                String id = params != null ? params.path("id").asText(null) : null;
                result = relations.getRelation(new GetRelationQuery(id));
                break;
            }
            case "relations.update": {
                String id = params != null ? params.path("id").asText(null) : null;
                String name = params != null ? params.path("name").asText(null) : null;
                result = relations.updateRelation(new UpdateRelationCmd(id, name));
                break;
            }
            case "relations.delete": {
                String id = params != null ? params.path("id").asText(null) : null;
                relations.deleteRelation(new DeleteRelationCmd(id));
                result = Map.of("deleted", true);
                break;
            }
            case "views.list": {
                result = views.listViews();
                break;
            }
            case "views.create": {
                String type = params != null ? params.path("type").asText(null) : null;
                String name = params != null ? params.path("name").asText(null) : null;
                result = views.createView(new CreateViewCmd(type, name));
                break;
            }
            case "views.get": {
                String id = params != null ? params.path("id").asText(null) : null;
                result = views.getView(new GetViewQuery(id));
                break;
            }
            case "views.delete": {
                String id = params != null ? params.path("id").asText(null) : null;
                views.deleteView(new DeleteViewCmd(id));
                result = Map.of("deleted", true);
                break;
            }
            case "views.content": {
                String id = params != null ? params.path("id").asText(null) : null;
                result = views.getViewContent(new GetViewContentQuery(id));
                break;
            }
            case "views.addElement": {
                String viewId = params != null ? params.path("viewId").asText(null) : null;
                String elementId = params != null ? params.path("elementId").asText(null) : null;
                String parentObjectId = params != null && params.has("parentObjectId") ? params.path("parentObjectId").asText(null) : null;
                Integer x = params != null && params.has("x") ? params.path("x").asInt() : null;
                Integer y = params != null && params.has("y") ? params.path("y").asInt() : null;
                Integer w = params != null && params.has("w") ? params.path("w").asInt() : null;
                Integer h = params != null && params.has("h") ? params.path("h").asInt() : null;
                result = views.addElement(new AddElementToViewCmd(viewId, elementId, parentObjectId, x, y, w, h));
                break;
            }
            case "views.addRelation": {
                String viewId = params != null ? params.path("viewId").asText(null) : null;
                String relationId = params != null ? params.path("relationId").asText(null) : null;
                String sourceObjectId = params != null && params.has("sourceObjectId") ? params.path("sourceObjectId").asText(null) : null;
                String targetObjectId = params != null && params.has("targetObjectId") ? params.path("targetObjectId").asText(null) : null;
                Boolean suppress = params != null && params.has("suppressWhenNested") ? params.path("suppressWhenNested").asBoolean() : null;
                String policy = params != null && params.has("policy") ? params.path("policy").asText(null) : null;
                result = views.addRelation(new AddRelationToViewCmd(viewId, relationId, sourceObjectId, targetObjectId, suppress, policy));
                break;
            }
            case "views.updateBounds": {
                String viewId = params != null ? params.path("viewId").asText(null) : null;
                String objectId = params != null ? params.path("objectId").asText(null) : null;
                Integer x = params != null && params.has("x") ? params.path("x").asInt() : null;
                Integer y = params != null && params.has("y") ? params.path("y").asInt() : null;
                Integer w = params != null && params.has("w") ? params.path("w").asInt() : null;
                Integer h = params != null && params.has("h") ? params.path("h").asInt() : null;
                result = views.updateBounds(new UpdateViewObjectBoundsCmd(viewId, objectId, x, y, w, h));
                break;
            }
            case "views.moveObject": {
                String viewId = params != null ? params.path("viewId").asText(null) : null;
                String objectId = params != null ? params.path("objectId").asText(null) : null;
                String parentObjectId = params != null && params.has("parentObjectId") ? params.path("parentObjectId").asText(null) : null;
                Integer x = params != null && params.has("x") ? params.path("x").asInt() : null;
                Integer y = params != null && params.has("y") ? params.path("y").asInt() : null;
                Integer w = params != null && params.has("w") ? params.path("w").asInt() : null;
                Integer h = params != null && params.has("h") ? params.path("h").asInt() : null;
                Boolean keep = params != null && params.has("keepExistingConnection") ? params.path("keepExistingConnection").asBoolean() : null;
                result = views.moveObject(new MoveViewObjectCmd(viewId, objectId, parentObjectId, x, y, w, h, keep));
                break;
            }
            case "views.deleteObject": {
                String viewId = params != null ? params.path("viewId").asText(null) : null;
                String objectId = params != null ? params.path("objectId").asText(null) : null;
                views.deleteObject(new DeleteViewObjectCmd(viewId, objectId));
                result = Map.of("deleted", true);
                break;
            }
            case "views.image": {
                String viewId = params != null ? params.path("viewId").asText(null) : null;
                String format = params != null && params.has("format") ? params.path("format").asText(null) : null;
                Float scale = params != null && params.has("scale") ? (float) params.path("scale").asDouble() : null;
                Integer dpi = params != null && params.has("dpi") ? params.path("dpi").asInt() : null;
                String bg = params != null && params.has("bg") ? params.path("bg").asText(null) : null;
                Integer margin = params != null && params.has("margin") ? params.path("margin").asInt() : null;
                ViewsCore.ImageData img = views.getViewImage(new GetViewImageQuery(viewId, format, scale, dpi, bg, margin));
                String b64 = Base64.getEncoder().encodeToString(img.data);
                result = Map.of("data", b64, "contentType", img.contentType);
                break;
            }
            case "search": {
                SearchQuery q = new SearchQuery();
                if (params != null) {
                    q.q = params.path("q").asText(null);
                    q.kind = params.path("kind").asText(null);
                    q.elementType = params.path("elementType").asText(null);
                    q.relationType = params.path("relationType").asText(null);
                    q.modelId = params.path("modelId").asText(null);
                    q.includeDocs = params.path("includeDocs").asBoolean(false);
                    q.includeProps = params.path("includeProps").asBoolean(false);
                    q.limit = params.path("limit").asInt(100);
                    q.offset = params.path("offset").asInt(0);
                    q.debug = params.path("debug").asBoolean(false);
                    q.logTarget = params.path("logTarget").asText("stdout");
                    JsonNode props = params.get("propertyFilters");
                    if (props != null && props.isObject()) {
                        props.fields().forEachRemaining(e -> q.propertyFilters.put(e.getKey(), e.getValue().asText()));
                    }
                }
                result = search.search(q);
                break;
            }
            case "folders.list": {
                result = folders.listFolders();
                break;
            }
            case "folders.ensure": {
                String path = params != null ? params.path("path").asText(null) : null;
                result = folders.ensureFolder(new EnsureFolderCmd(path));
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

