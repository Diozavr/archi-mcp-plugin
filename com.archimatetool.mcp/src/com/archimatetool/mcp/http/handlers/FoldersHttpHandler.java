package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.service.ServiceRegistry;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class FoldersHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
        List<Map<String,Object>> roots = new ArrayList<>();
        for (Object f : model.getFolders()) {
            if (f instanceof com.archimatetool.model.IFolder) {
                roots.add(folderToNode((com.archimatetool.model.IFolder) f, ""));
            }
        }
        ResponseUtil.ok(exchange, roots);
    }

    private Map<String,Object> folderToNode(com.archimatetool.model.IFolder folder, String parentPath) {
        Map<String,Object> node = new HashMap<>();
        String path = parentPath == null || parentPath.isEmpty() ? folder.getName() : parentPath + "/" + folder.getName();
        node.put("id", folder.getId());
        node.put("name", folder.getName());
        node.put("path", path);
        List<Map<String,Object>> children = new ArrayList<>();
        for (Object ch : folder.getFolders()) {
            if (ch instanceof com.archimatetool.model.IFolder) {
                children.add(folderToNode((com.archimatetool.model.IFolder) ch, path));
            }
        }
        node.put("children", children);
        return node;
    }
}


