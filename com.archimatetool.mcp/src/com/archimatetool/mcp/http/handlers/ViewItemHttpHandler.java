package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.Map;

import com.archimatetool.mcp.http.QueryParams;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.archimatetool.mcp.server.ModelApi;
import com.archimatetool.mcp.service.ServiceRegistry;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IDiagramModel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ViewItemHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String remainder = exchange.getRequestURI().getPath().substring("/views/".length());
        String id;
        String subpath = null;
        int slash = remainder.indexOf('/');
        if (slash >= 0) { id = remainder.substring(0, slash); subpath = remainder.substring(slash + 1); }
        else { id = remainder; }

        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
        Object o = com.archimatetool.mcp.service.ServiceRegistry.activeModel().findById(model, id);
        if (subpath == null || subpath.isEmpty()) {
            String method = exchange.getRequestMethod();
            if ("GET".equalsIgnoreCase(method)) {
                if (!(o instanceof IDiagramModel)) { ResponseUtil.notFound(exchange, "not found"); return; }
                ResponseUtil.ok(exchange, ModelApi.viewToDto((IDiagramModel)o));
                return;
            } else if ("DELETE".equalsIgnoreCase(method)) {
                if (!(o instanceof IDiagramModel)) { ResponseUtil.notFound(exchange, "not found"); return; }
                boolean ok = com.archimatetool.mcp.service.ServiceRegistry.views().deleteView((IDiagramModel)o);
                if (ok) { ResponseUtil.noContent(exchange); } else { ResponseUtil.badRequest(exchange, "cannot delete"); }
                return;
            }
            ResponseUtil.methodNotAllowed(exchange);
            return;
        }

        if (!(o instanceof IDiagramModel)) { ResponseUtil.notFound(exchange, "view not found"); return; }
        IDiagramModel view = (IDiagramModel) o;
        if ("content".equals(subpath) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtil.ok(exchange, ModelApi.viewContentToDto(view));
            return;
        }
        if ("image".equals(subpath) && "GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            model = ServiceRegistry.activeModel().getActiveModel();
            if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
            QueryParams qp = QueryParams.from(exchange);
            String format = qp.first("format");
            if (format == null) format = "png";
            float scale = qp.getFloat("scale", 1.0f);
            Integer dpi = qp.getInt("dpi", (Integer) null);
            String bg = qp.first("bg");
            if (bg == null) bg = "transparent";
            int margin = qp.getInt("margin", 0);
            if ("svg".equalsIgnoreCase(format)) {
                byte[] svg = ModelApi.renderViewToSVG(view, scale, bg, margin);
                if (svg == null || svg.length == 0) { ResponseUtil.badRequest(exchange, "render failed"); return; }
                exchange.getResponseHeaders().set("Content-Type", "image/svg+xml; charset=utf-8");
                exchange.sendResponseHeaders(200, svg.length);
                try (java.io.OutputStream os = exchange.getResponseBody()) { os.write(svg); }
                return;
            }
            java.awt.Color bgc = null;
            if (bg != null && !"transparent".equalsIgnoreCase(bg)) {
                try { if (bg.startsWith("%23")) bg = bg.replace("%23", "#"); } catch (Exception ignore) {}
                if (bg.startsWith("#") && bg.length()==7) {
                    int r = Integer.parseInt(bg.substring(1,3),16);
                    int g = Integer.parseInt(bg.substring(3,5),16);
                    int b = Integer.parseInt(bg.substring(5,7),16);
                    bgc = new java.awt.Color(r,g,b);
                }
            }
            byte[] png = ModelApi.renderViewToPNG(view, scale, dpi, bgc, margin);
            if (png == null || png.length == 0) { ResponseUtil.badRequest(exchange, "render failed"); return; }
            exchange.getResponseHeaders().set("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, png.length);
            try (java.io.OutputStream os = exchange.getResponseBody()) { os.write(png); }
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
            int x = bx != null ? bx.intValue() : 100;
            int y = by != null ? by.intValue() : 100;
            int w = bw != null ? bw.intValue() : 120;
            int h = bh != null ? bh.intValue() : 80;
            Object eo = ServiceRegistry.activeModel().findById(model, elementId);
            if (!(eo instanceof IArchimateElement)) { ResponseUtil.notFound(exchange, "element not found"); return; }
            com.archimatetool.model.IDiagramModelObject parentObj = null;
            if (parentObjectId != null && !parentObjectId.isEmpty()) {
                parentObj = ModelApi.findDiagramObjectById(view, parentObjectId);
                if (parentObj == null) { ResponseUtil.notFound(exchange, "parentObjectId not found in view"); return; }
                if (!(parentObj instanceof com.archimatetool.model.IDiagramModelContainer)) { ResponseUtil.badRequest(exchange, "parent object is not a container"); return; }
            }
            com.archimatetool.model.IDiagramModelArchimateObject dmo;
            if (parentObj instanceof com.archimatetool.model.IDiagramModelContainer) {
                dmo = ServiceRegistry.views().addElementToContainer((com.archimatetool.model.IDiagramModelContainer) parentObj, (IArchimateElement)eo, x, y, w, h);
            } else {
                dmo = ServiceRegistry.views().addElementToView(view, (IArchimateElement)eo, x, y, w, h);
            }
            ResponseUtil.ok(exchange, Map.of("objectId", dmo.getId()));
            return;
        }
        if ("add-relation".equals(subpath) && "POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            String relationId = jr.optString("relationId");
            String sourceObjectId = jr.optString("sourceObjectId");
            String targetObjectId = jr.optString("targetObjectId");
            Boolean suppressWhenNested = jr.optBool("suppressWhenNested");
            String policy = jr.optString("policy");
            if (policy == null || policy.isEmpty()) policy = "auto";
            if (relationId == null || relationId.isEmpty()) { ResponseUtil.badRequest(exchange, "relationId is required"); return; }
            model = ServiceRegistry.activeModel().getActiveModel();
            Object ro = ServiceRegistry.activeModel().findById(model, relationId);
            if (!(ro instanceof com.archimatetool.model.IArchimateRelationship)) { ResponseUtil.notFound(exchange, "relation not found"); return; }
            com.archimatetool.model.IArchimateRelationship rel = (com.archimatetool.model.IArchimateRelationship) ro;

            com.archimatetool.model.IDiagramModelObject so = null;
            com.archimatetool.model.IDiagramModelObject to = null;

            if (sourceObjectId != null && !sourceObjectId.isEmpty()) {
                so = ModelApi.findDiagramObjectById(view, sourceObjectId);
                if (so == null) { ResponseUtil.notFound(exchange, "sourceObjectId not found in view"); return; }
            }
            if (targetObjectId != null && !targetObjectId.isEmpty()) {
                to = ModelApi.findDiagramObjectById(view, targetObjectId);
                if (to == null) { ResponseUtil.notFound(exchange, "targetObjectId not found in view"); return; }
            }

            // Validate correspondence to relation endpoints if provided
            if (so != null) {
                if (!(so instanceof com.archimatetool.model.IDiagramModelArchimateObject)) { ResponseUtil.json(exchange, 422, java.util.Map.of("error", "sourceObjectId is not archimate object")); return; }
                com.archimatetool.model.IArchimateConcept c = ((com.archimatetool.model.IDiagramModelArchimateObject) so).getArchimateConcept();
                if (!(c instanceof com.archimatetool.model.IArchimateElement) || c != rel.getSource()) { ResponseUtil.json(exchange, 422, java.util.Map.of("error", "sourceObjectId does not match relation source")); return; }
            }
            if (to != null) {
                if (!(to instanceof com.archimatetool.model.IDiagramModelArchimateObject)) { ResponseUtil.json(exchange, 422, java.util.Map.of("error", "targetObjectId is not archimate object")); return; }
                com.archimatetool.model.IArchimateConcept c = ((com.archimatetool.model.IDiagramModelArchimateObject) to).getArchimateConcept();
                if (!(c instanceof com.archimatetool.model.IArchimateElement) || c != rel.getTarget()) { ResponseUtil.json(exchange, 422, java.util.Map.of("error", "targetObjectId does not match relation target")); return; }
            }

            // Auto policy
            if ((so == null || to == null)) {
                if (!"auto".equals(policy)) { ResponseUtil.badRequest(exchange, "sourceObjectId/targetObjectId required or use policy=auto"); return; }
                // find occurrences of relation endpoints on the view
                String srcElId = rel.getSource() != null ? rel.getSource().getId() : null;
                String tgtElId = rel.getTarget() != null ? rel.getTarget().getId() : null;
                java.util.List<com.archimatetool.model.IDiagramModelObject> srcObjs = ModelApi.findDiagramObjectsByElementId(view, srcElId);
                java.util.List<com.archimatetool.model.IDiagramModelObject> tgtObjs = ModelApi.findDiagramObjectsByElementId(view, tgtElId);
                if (so == null) {
                    if (srcObjs.size() == 1) so = srcObjs.get(0); else { ResponseUtil.json(exchange, 409, java.util.Map.of("error", "ambiguous or missing source object on view")); return; }
                }
                if (to == null) {
                    if (tgtObjs.size() == 1) to = tgtObjs.get(0); else { ResponseUtil.json(exchange, 409, java.util.Map.of("error", "ambiguous or missing target object on view")); return; }
                }
            }

            // Optionally suppress when nested
            if (Boolean.TRUE.equals(suppressWhenNested)) {
                if (ModelApi.isAncestorOf(so, to) || ModelApi.isAncestorOf(to, so)) {
                    ResponseUtil.ok(exchange, java.util.Map.of("suppressed", true));
                    return;
                }
            }
            var conn = ModelApi.addRelationToView(view, rel, so, to);
            ResponseUtil.created(exchange, ModelApi.connectionToDto(conn));
            return;
        }
        if (subpath.startsWith("objects/") ) {
            String rest = subpath.substring("objects/".length());
            int s2 = rest.indexOf('/');
            String objectId = s2>=0 ? rest.substring(0, s2) : rest;
            String tail = s2>=0 ? rest.substring(s2+1) : null;
            // Use recursive lookup so nested objects are supported
            com.archimatetool.model.IDiagramModelObject dmo = ModelApi.findDiagramObjectById(view, objectId);
            if (dmo == null) { ResponseUtil.notFound(exchange, "object not found"); return; }
            if (tail == null && "DELETE".equalsIgnoreCase(exchange.getRequestMethod())) {
                boolean ok = ServiceRegistry.views().deleteViewObject(dmo);
                if (ok) { ResponseUtil.noContent(exchange); } else { ResponseUtil.badRequest(exchange, "cannot remove object"); }
                return;
            }
            if ("bounds".equals(tail) && "PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
                JsonReader jr = JsonReader.fromExchange(exchange);
                String sx = jr.optString("x");
                String sy = jr.optString("y");
                String sw = jr.optString("w");
                String sh = jr.optString("h");
                int x = sx != null ? Integer.parseInt(sx) : dmo.getBounds().getX();
                int y = sy != null ? Integer.parseInt(sy) : dmo.getBounds().getY();
                int w = sw != null ? Integer.parseInt(sw) : dmo.getBounds().getWidth();
                int h = sh != null ? Integer.parseInt(sh) : dmo.getBounds().getHeight();
                com.archimatetool.model.IBounds b = com.archimatetool.model.IArchimateFactory.eINSTANCE.createBounds(x, y, w, h);
                final com.archimatetool.model.IBounds fb = b;
                org.eclipse.swt.widgets.Display.getDefault().syncExec(() -> dmo.setBounds(fb));
                ResponseUtil.ok(exchange, ModelApi.viewObjectToDto(dmo));
                return;
            }
            if ("move".equals(tail) && "PATCH".equalsIgnoreCase(exchange.getRequestMethod())) {
                JsonReader jr = JsonReader.fromExchange(exchange);
                String parentObjectId = jr.optString("parentObjectId");
                if (parentObjectId == null || parentObjectId.isEmpty()) { ResponseUtil.badRequest(exchange, "parentObjectId is required"); return; }
                com.archimatetool.model.IDiagramModelObject parentObj = null;
                com.archimatetool.model.IDiagramModelContainer targetContainer = null;
                if ("0".equals(parentObjectId)) {
                    targetContainer = view; // move to root
                } else {
                    parentObj = ModelApi.findDiagramObjectById(view, parentObjectId);
                    if (parentObj == null) { ResponseUtil.notFound(exchange, "parentObjectId not found in view"); return; }
                    if (!(parentObj instanceof com.archimatetool.model.IDiagramModelContainer)) { ResponseUtil.badRequest(exchange, "parent object is not a container"); return; }
                    targetContainer = (com.archimatetool.model.IDiagramModelContainer) parentObj;
                }
                // Prevent cycles: parent cannot be the object itself or inside its subtree
                if (parentObj != null && ModelApi.isAncestorOf(dmo, parentObj)) { ResponseUtil.badRequest(exchange, "cannot move into own descendant"); return; }
                Integer bx = jr.optIntWithin("bounds", "x");
                Integer by = jr.optIntWithin("bounds", "y");
                Integer bw = jr.optIntWithin("bounds", "w");
                Integer bh = jr.optIntWithin("bounds", "h");
                var moved = ServiceRegistry.views().moveObjectToContainer(dmo, targetContainer, bx, by, bw, bh);
                Boolean keepExistingConnection = jr.optBool("keepExistingConnection");
                if (!Boolean.TRUE.equals(keepExistingConnection) && parentObj != null) {
                    // If there is an Archimate relationship between elements of moved object and parent, and a connection exists on this view between these objects, remove that connection
                    try {
                        if (dmo instanceof com.archimatetool.model.IDiagramModelArchimateObject && parentObj instanceof com.archimatetool.model.IDiagramModelArchimateObject) {
                            var childEl = ((com.archimatetool.model.IDiagramModelArchimateObject) dmo).getArchimateConcept();
                            var parentEl = ((com.archimatetool.model.IDiagramModelArchimateObject) parentObj).getArchimateConcept();
                            if (childEl instanceof com.archimatetool.model.IArchimateElement && parentEl instanceof com.archimatetool.model.IArchimateElement) {
                                // Find direct connection object between these two diagram objects (either direction)
                                java.util.List<Object> toDel = new java.util.ArrayList<>();
                                for (Object co : dmo.getSourceConnections()) {
                                    if (co instanceof com.archimatetool.model.IDiagramModelArchimateConnection) {
                                        var ac = (com.archimatetool.model.IDiagramModelArchimateConnection) co;
                                        if (ac.getSource() == dmo && ac.getTarget() == parentObj) toDel.add(ac);
                                    }
                                }
                                for (Object co : dmo.getTargetConnections()) {
                                    if (co instanceof com.archimatetool.model.IDiagramModelArchimateConnection) {
                                        var ac = (com.archimatetool.model.IDiagramModelArchimateConnection) co;
                                        if (ac.getTarget() == dmo && ac.getSource() == parentObj) toDel.add(ac);
                                    }
                                }
                                if (!toDel.isEmpty()) {
                                    final java.util.List<Object> dels = toDel;
                                    org.eclipse.swt.widgets.Display.getDefault().syncExec(() -> {
                                        for (Object dc : dels) {
                                            org.eclipse.emf.ecore.util.EcoreUtil.delete((org.eclipse.emf.ecore.EObject) dc);
                                        }
                                    });
                                }
                            }
                        }
                    } catch (Throwable ignore) { /* best-effort removal */ }
                }
                if (moved == null) { ResponseUtil.badRequest(exchange, "cannot move object"); return; }
                ResponseUtil.ok(exchange, ModelApi.viewObjectToDto(moved));
                return;
            }
        }
        ResponseUtil.notFound(exchange, "not found");
    }
}


