package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.archimatetool.mcp.server.ModelApi;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class FolderEnsureHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        var model = com.archimatetool.mcp.service.ServiceRegistry.activeModel().getActiveModel();
        if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
        JsonReader jr = JsonReader.fromExchange(exchange);
        String path = jr.optString("path");
        if (path == null || path.trim().isEmpty()) { ResponseUtil.badRequest(exchange, "path required"); return; }
        com.archimatetool.model.IFolder root = model.getFolder(com.archimatetool.model.FolderType.DIAGRAMS);
        if (root == null) { ResponseUtil.badRequest(exchange, "no diagrams folder"); return; }
        String[] parts = path.split("/");
        final com.archimatetool.model.IFolder[] current = new com.archimatetool.model.IFolder[]{ root };
        for (String seg : parts) {
            if (seg == null || seg.isEmpty()) continue;
            com.archimatetool.model.IFolder found = null;
            for (Object f : current[0].getFolders()) {
                if (f instanceof com.archimatetool.model.IFolder) {
                    com.archimatetool.model.IFolder cf = (com.archimatetool.model.IFolder) f;
                    if (seg.equals(cf.getName())) { found = cf; break; }
                }
            }
            if (found == null) {
                final String name = seg;
                final com.archimatetool.model.IFolder[] created = new com.archimatetool.model.IFolder[1];
                org.eclipse.swt.widgets.Display.getDefault().syncExec(() -> {
                    com.archimatetool.model.IFolder nf = com.archimatetool.model.IArchimateFactory.eINSTANCE.createFolder();
                    nf.setName(name);
                    nf.setType(current[0].getType());
                    current[0].getFolders().add(nf);
                    created[0] = nf;
                });
                current[0] = created[0];
            } else {
                current[0] = found;
            }
        }
        Map<String,Object> node = new HashMap<>();
        node.put("id", current[0].getId());
        node.put("name", current[0].getName());
        node.put("path", path);
        node.put("children", new java.util.ArrayList<>());
        ResponseUtil.ok(exchange, node);
    }
}


