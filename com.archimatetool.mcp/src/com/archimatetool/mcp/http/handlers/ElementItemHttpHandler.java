package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.archimatetool.mcp.server.ModelApi;
import com.archimatetool.mcp.service.ServiceRegistry;
import com.archimatetool.model.IArchimateElement;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class ElementItemHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String remainder = uri.getPath().substring("/elements/".length());
        String id;
        String subpath = null;
        int slash = remainder.indexOf('/');
        if (slash >= 0) { id = remainder.substring(0, slash); subpath = remainder.substring(slash + 1); }
        else { id = remainder; }

        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
        Object o = com.archimatetool.mcp.service.ServiceRegistry.activeModel().findById(model, id);
        String method = exchange.getRequestMethod();

        if (subpath != null && !subpath.isEmpty()) {
            if (!(o instanceof IArchimateElement)) { ResponseUtil.notFound(exchange, "element not found"); return; }
            IArchimateElement el = (IArchimateElement) o;
            if ("relations".equals(subpath) && "GET".equalsIgnoreCase(method)) {
                String direction = "both";
                boolean includeElements = false;
                String query = uri.getQuery();
                if (query != null) {
                    for (String p : query.split("&")) {
                        int i = p.indexOf('=');
                        String k = i > 0 ? p.substring(0, i) : p;
                        String v = i > 0 ? java.net.URLDecoder.decode(p.substring(i + 1), java.nio.charset.StandardCharsets.UTF_8) : "";
                        if ("direction".equalsIgnoreCase(k)) direction = v != null && !v.isEmpty() ? v.toLowerCase() : direction;
                        if ("includeElements".equalsIgnoreCase(k)) includeElements = "true".equalsIgnoreCase(v) || "1".equals(v);
                    }
                }
                List<Object> items = new ArrayList<>();
                for (Object f : model.getFolders()) {
                    if (f instanceof com.archimatetool.model.IFolder) {
                        com.archimatetool.model.IFolder folder = (com.archimatetool.model.IFolder) f;
                        for (Object e : folder.getElements()) {
                            if (e instanceof com.archimatetool.model.IArchimateRelationship) {
                                com.archimatetool.model.IArchimateRelationship r = (com.archimatetool.model.IArchimateRelationship) e;
                                boolean isOut = r.getSource() == el;
                                boolean isIn = r.getTarget() == el;
                                boolean match = "both".equals(direction) || ("out".equals(direction) && isOut) || ("in".equals(direction) && isIn);
                                if (match && (isOut || isIn)) {
                                    if (includeElements) {
                                        Map<String, Object> m = new HashMap<>();
                                        m.put("relation", ModelApi.relationToDto(r));
                                        if (r.getSource() instanceof IArchimateElement) m.put("source", ModelApi.elementToDto((IArchimateElement) r.getSource()));
                                        if (r.getTarget() instanceof IArchimateElement) m.put("target", ModelApi.elementToDto((IArchimateElement) r.getTarget()));
                                        items.add(m);
                                    } else {
                                        items.add(ModelApi.relationToDto(r));
                                    }
                                }
                            }
                        }
                    }
                }
                Map<String, Object> resp = new HashMap<>();
                resp.put("total", items.size());
                resp.put("items", items);
                ResponseUtil.ok(exchange, resp);
                return;
            }
            ResponseUtil.methodNotAllowed(exchange);
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            if (!(o instanceof IArchimateElement)) { ResponseUtil.notFound(exchange, "not found"); return; }
            IArchimateElement el = (IArchimateElement) o;
            String include = null;
            boolean includeElements = false;
            String query = uri.getQuery();
            if (query != null) {
                for (String p : query.split("&")) {
                    int i = p.indexOf('=');
                    String k = i > 0 ? p.substring(0, i) : p;
                        String v = i > 0 ? java.net.URLDecoder.decode(p.substring(i + 1), java.nio.charset.StandardCharsets.UTF_8) : "";
                    if ("include".equalsIgnoreCase(k) || "with".equalsIgnoreCase(k)) include = v;
                    if ("includeElements".equalsIgnoreCase(k)) includeElements = "true".equalsIgnoreCase(v) || "1".equals(v);
                }
            }
            if (include != null && !include.isEmpty() && java.util.Arrays.stream(include.split(",")).anyMatch(s -> s.trim().equalsIgnoreCase("relations"))) {
                List<Object> items = new ArrayList<>();
                for (Object f : model.getFolders()) {
                    if (f instanceof com.archimatetool.model.IFolder) {
                        com.archimatetool.model.IFolder folder = (com.archimatetool.model.IFolder) f;
                        for (Object e : folder.getElements()) {
                            if (e instanceof com.archimatetool.model.IArchimateRelationship) {
                                com.archimatetool.model.IArchimateRelationship r = (com.archimatetool.model.IArchimateRelationship) e;
                                boolean isOut = r.getSource() == el;
                                boolean isIn = r.getTarget() == el;
                                if (isOut || isIn) {
                                    if (includeElements) {
                                        Map<String, Object> m = new HashMap<>();
                                        m.put("relation", ModelApi.relationToDto(r));
                                        if (r.getSource() instanceof IArchimateElement) m.put("source", ModelApi.elementToDto((IArchimateElement) r.getSource()));
                                        if (r.getTarget() instanceof IArchimateElement) m.put("target", ModelApi.elementToDto((IArchimateElement) r.getTarget()));
                                        items.add(m);
                                    } else {
                                        items.add(ModelApi.relationToDto(r));
                                    }
                                }
                            }
                        }
                    }
                }
                Map<String,Object> dto = new HashMap<>(ModelApi.elementToDto(el));
                dto.put("relations", items);
                ResponseUtil.ok(exchange, dto);
            } else {
                ResponseUtil.ok(exchange, ModelApi.elementToDto(el));
            }
            return;
        } else if ("PATCH".equalsIgnoreCase(method)) {
            if (!(o instanceof IArchimateElement)) { ResponseUtil.notFound(exchange, "not found"); return; }
            JsonReader jr = JsonReader.fromExchange(exchange);
            String name = jr.optString("name");
            IArchimateElement el = (IArchimateElement)o;
            if (name != null) {
                final String n = name;
                org.eclipse.swt.widgets.Display.getDefault().syncExec(() -> el.setName(n));
            }
            ResponseUtil.ok(exchange, ModelApi.elementToDto(el));
            return;
        } else if ("DELETE".equalsIgnoreCase(method)) {
            if (!(o instanceof IArchimateElement)) { ResponseUtil.notFound(exchange, "not found"); return; }
            boolean ok = com.archimatetool.mcp.service.ServiceRegistry.elements().deleteElement((IArchimateElement)o);
            if (ok) { ResponseUtil.noContent(exchange); } else { ResponseUtil.badRequest(exchange, "cannot delete"); }
            return;
        }
        ResponseUtil.methodNotAllowed(exchange);
    }
}


