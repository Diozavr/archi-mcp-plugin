package ru.cinimex.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import ru.cinimex.archimatetool.mcp.core.types.AddElementToViewItem;
import ru.cinimex.archimatetool.mcp.core.types.AddElementsToViewCmd;
import ru.cinimex.archimatetool.mcp.core.types.AddRelationToViewItem;
import ru.cinimex.archimatetool.mcp.core.types.AddRelationsToViewCmd;
import ru.cinimex.archimatetool.mcp.core.types.DeleteViewCmd;
import ru.cinimex.archimatetool.mcp.core.types.DeleteViewObjectItem;
import ru.cinimex.archimatetool.mcp.core.types.DeleteViewObjectsCmd;
import ru.cinimex.archimatetool.mcp.core.types.GetViewContentQuery;
import ru.cinimex.archimatetool.mcp.core.types.GetViewImageQuery;
import ru.cinimex.archimatetool.mcp.core.types.GetViewQuery;
import ru.cinimex.archimatetool.mcp.core.types.MoveViewObjectItem;
import ru.cinimex.archimatetool.mcp.core.types.MoveViewObjectsCmd;
import ru.cinimex.archimatetool.mcp.core.types.UpdateViewObjectBoundsItem;
import ru.cinimex.archimatetool.mcp.core.types.UpdateViewObjectsBoundsCmd;
import ru.cinimex.archimatetool.mcp.core.views.ViewsCore;
import ru.cinimex.archimatetool.mcp.http.QueryParams;
import ru.cinimex.archimatetool.mcp.http.ResponseUtil;
import ru.cinimex.archimatetool.mcp.json.JsonReader;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** HTTP handler for view item operations with batch support. */
public class ViewItemHttpHandler implements HttpHandler {
    private final ViewsCore core = new ViewsCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String remainder = exchange.getRequestURI().getPath().substring("/views/".length());
        String id;
        String subpath = null;
        int slash = remainder.indexOf('/');
        if (slash >= 0) { id = remainder.substring(0, slash); subpath = remainder.substring(slash + 1); }
        else { id = remainder; }

        if (subpath == null || subpath.isEmpty()) {
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                GetViewQuery q = new GetViewQuery(id);
                try {
                    var dto = core.getView(q);
                    ResponseUtil.ok(exchange, dto);
                } catch (CoreException ex) {
                    ResponseUtil.handleCoreException(exchange, ex);
                }
                return;
            } else if ("DELETE".equalsIgnoreCase(method)) {
                DeleteViewCmd cmd = new DeleteViewCmd(id);
                try {
                    core.deleteView(cmd);
                    ResponseUtil.noContent(exchange);
                } catch (CoreException ex) {
                    ResponseUtil.handleCoreException(exchange, ex);
                }
                return;
            }
            ResponseUtil.methodNotAllowed(exchange);
            return;
        }

        if ("content".equals(subpath) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            GetViewContentQuery q = new GetViewContentQuery(id);
            try {
                var dto = core.getViewContent(q);
                ResponseUtil.ok(exchange, dto);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }

        if ("image".equals(subpath) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            QueryParams qp = QueryParams.from(exchange);
            String format = qp.first("format");
            Float scale = qp.getFloat("scale", 1.0f);
            Integer dpi = qp.getInt("dpi", (Integer) null);
            String bg = qp.first("bg");
            Integer margin = qp.getInt("margin", 0);
            GetViewImageQuery q = new GetViewImageQuery(id, format, scale, dpi, bg, margin);
            try {
                var img = core.getViewImage(q);
                exchange.getResponseHeaders().set("Content-Type", img.contentType);
                exchange.sendResponseHeaders(200, img.data.length);
                try (java.io.OutputStream os = exchange.getResponseBody()) { os.write(img.data); }
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }

        if ("add-element".equals(subpath) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            List<AddElementToViewItem> items = new ArrayList<>();
            if (jr.isArrayRoot()) {
                for (int i = 0; i < jr.arraySize(); i++) {
                    JsonReader it = jr.at(i);
                    Integer bx = it.optIntWithin("bounds", "x");
                    Integer by = it.optIntWithin("bounds", "y");
                    Integer bw = it.optIntWithin("bounds", "w");
                    Integer bh = it.optIntWithin("bounds", "h");
                    if (bx == null) bx = it.optInt("x");
                    if (by == null) by = it.optInt("y");
                    if (bw == null) bw = it.optInt("w");
                    if (bh == null) bh = it.optInt("h");
                    items.add(new AddElementToViewItem(it.optString("elementId"), it.optString("parentObjectId"),
                            bx, by, bw, bh, readMap(it.optObject("style"))));
                }
            }
            AddElementsToViewCmd cmd = new AddElementsToViewCmd(id, items);
            try {
                var res = core.addElements(cmd);
                ResponseUtil.ok(exchange, res);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }

        if ("add-relation".equals(subpath) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            List<AddRelationToViewItem> items = new ArrayList<>();
            if (jr.isArrayRoot()) {
                for (int i = 0; i < jr.arraySize(); i++) {
                    JsonReader it = jr.at(i);
                    items.add(new AddRelationToViewItem(it.optString("relationId"), it.optString("sourceObjectId"),
                            it.optString("targetObjectId"), it.optString("policy"), it.optBool("suppressWhenNested")));
                }
            }
            AddRelationsToViewCmd cmd = new AddRelationsToViewCmd(id, items);
            try {
                var res = core.addRelations(cmd);
                ResponseUtil.ok(exchange, res);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }

        if ("objects/bounds".equals(subpath) && "PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            List<UpdateViewObjectBoundsItem> items = new ArrayList<>();
            if (jr.isArrayRoot()) {
                for (int i = 0; i < jr.arraySize(); i++) {
                    JsonReader it = jr.at(i);
                    items.add(new UpdateViewObjectBoundsItem(it.optString("objectId"), it.optInt("x"), it.optInt("y"),
                            it.optInt("w"), it.optInt("h")));
                }
            }
            UpdateViewObjectsBoundsCmd cmd = new UpdateViewObjectsBoundsCmd(id, items);
            try {
                var dto = core.updateBounds(cmd);
                ResponseUtil.ok(exchange, dto);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }

        if ("objects/move".equals(subpath) && "PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            List<MoveViewObjectItem> items = new ArrayList<>();
            if (jr.isArrayRoot()) {
                for (int i = 0; i < jr.arraySize(); i++) {
                    JsonReader it = jr.at(i);
                    Integer bx = it.optIntWithin("bounds", "x");
                    Integer by = it.optIntWithin("bounds", "y");
                    Integer bw = it.optIntWithin("bounds", "w");
                    Integer bh = it.optIntWithin("bounds", "h");
                    if (bx == null) bx = it.optInt("x");
                    if (by == null) by = it.optInt("y");
                    if (bw == null) bw = it.optInt("w");
                    if (bh == null) bh = it.optInt("h");
                    items.add(new MoveViewObjectItem(it.optString("objectId"), it.optString("parentObjectId"),
                            bx, by, bw, bh, it.optBool("keepExistingConnection")));
                }
            }
            MoveViewObjectsCmd cmd = new MoveViewObjectsCmd(id, items);
            try {
                var dto = core.moveObjects(cmd);
                ResponseUtil.ok(exchange, dto);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }

        if ("objects".equals(subpath) && "DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            List<DeleteViewObjectItem> items = new ArrayList<>();
            if (jr.isArrayRoot()) {
                for (int i = 0; i < jr.arraySize(); i++) {
                    JsonReader it = jr.at(i);
                    items.add(new DeleteViewObjectItem(it.optString("objectId")));
                }
            }
            DeleteViewObjectsCmd cmd = new DeleteViewObjectsCmd(id, items);
            try {
                var dto = core.deleteObjects(cmd);
                ResponseUtil.ok(exchange, dto);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }

        ResponseUtil.notFound(exchange, "not found");
    }

    private Map<String, String> readMap(JsonNode node) {
        if (node == null) return null;
        Map<String, String> m = new HashMap<>();
        node.fields().forEachRemaining(e -> m.put(e.getKey(), e.getValue().asText()));
        return m;
    }
}

