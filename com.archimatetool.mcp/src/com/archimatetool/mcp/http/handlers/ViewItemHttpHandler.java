package com.archimatetool.mcp.http.handlers;

import java.io.IOException;

import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.types.AddElementToViewCmd;
import com.archimatetool.mcp.core.types.AddRelationToViewCmd;
import com.archimatetool.mcp.core.types.DeleteViewCmd;
import com.archimatetool.mcp.core.types.DeleteViewObjectCmd;
import com.archimatetool.mcp.core.types.GetViewContentQuery;
import com.archimatetool.mcp.core.types.GetViewImageQuery;
import com.archimatetool.mcp.core.types.GetViewQuery;
import com.archimatetool.mcp.core.types.MoveViewObjectCmd;
import com.archimatetool.mcp.core.types.UpdateViewObjectBoundsCmd;
import com.archimatetool.mcp.core.views.ViewsCore;
import com.archimatetool.mcp.http.QueryParams;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

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
            String elementId = jr.optString("elementId");
            String parentObjectId = jr.optString("parentObjectId");
            Integer bx = jr.optIntWithin("bounds", "x");
            Integer by = jr.optIntWithin("bounds", "y");
            Integer bw = jr.optIntWithin("bounds", "w");
            Integer bh = jr.optIntWithin("bounds", "h");
            if (bx == null) bx = jr.optInt("x");
            if (by == null) by = jr.optInt("y");
            if (bw == null) bw = jr.optInt("w");
            if (bh == null) bh = jr.optInt("h");
            AddElementToViewCmd cmd = new AddElementToViewCmd(id, elementId, parentObjectId, bx, by, bw, bh);
            try {
                var res = core.addElement(cmd);
                ResponseUtil.ok(exchange, res);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }
        if ("add-relation".equals(subpath) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            String relationId = jr.optString("relationId");
            String sourceObjectId = jr.optString("sourceObjectId");
            String targetObjectId = jr.optString("targetObjectId");
            Boolean suppressWhenNested = jr.optBool("suppressWhenNested");
            String policy = jr.optString("policy");
            AddRelationToViewCmd cmd = new AddRelationToViewCmd(id, relationId, sourceObjectId, targetObjectId, suppressWhenNested, policy);
            try {
                var res = core.addRelation(cmd);
                if (Boolean.TRUE.equals(res.get("suppressed"))) {
                    ResponseUtil.ok(exchange, res);
                } else {
                    ResponseUtil.created(exchange, res);
                }
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }
        if (subpath.startsWith("objects/")) {
            String rest = subpath.substring("objects/".length());
            int s2 = rest.indexOf('/');
            String objectId = s2 >= 0 ? rest.substring(0, s2) : rest;
            String tail = s2 >= 0 ? rest.substring(s2 + 1) : null;
            if (tail == null && "DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                DeleteViewObjectCmd cmd = new DeleteViewObjectCmd(id, objectId);
                try {
                    core.deleteObject(cmd);
                    ResponseUtil.noContent(exchange);
                } catch (CoreException ex) {
                    ResponseUtil.handleCoreException(exchange, ex);
                }
                return;
            }
            if ("bounds".equals(tail) && "PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
                JsonReader jr = JsonReader.fromExchange(exchange);
                UpdateViewObjectBoundsCmd cmd = new UpdateViewObjectBoundsCmd(id, objectId,
                        jr.optInt("x"), jr.optInt("y"), jr.optInt("w"), jr.optInt("h"));
                try {
                    var dto = core.updateBounds(cmd);
                    ResponseUtil.ok(exchange, dto);
                } catch (CoreException ex) {
                    ResponseUtil.handleCoreException(exchange, ex);
                }
                return;
            }
            if ("move".equals(tail) && "PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
                JsonReader jr = JsonReader.fromExchange(exchange);
                String parentObjectId = jr.optString("parentObjectId");
                Boolean keepExistingConnection = jr.optBool("keepExistingConnection");
                Integer bx = jr.optIntWithin("bounds", "x");
                Integer by = jr.optIntWithin("bounds", "y");
                Integer bw = jr.optIntWithin("bounds", "w");
                Integer bh = jr.optIntWithin("bounds", "h");
                if (bx == null) bx = jr.optInt("x");
                if (by == null) by = jr.optInt("y");
                if (bw == null) bw = jr.optInt("w");
                if (bh == null) bh = jr.optInt("h");
                MoveViewObjectCmd cmd = new MoveViewObjectCmd(id, objectId, parentObjectId, bx, by, bw, bh, keepExistingConnection);
                try {
                    var dto = core.moveObject(cmd);
                    ResponseUtil.ok(exchange, dto);
                } catch (CoreException ex) {
                    ResponseUtil.handleCoreException(exchange, ex);
                }
                return;
            }
        }
        ResponseUtil.notFound(exchange, "not found");
    }
}
