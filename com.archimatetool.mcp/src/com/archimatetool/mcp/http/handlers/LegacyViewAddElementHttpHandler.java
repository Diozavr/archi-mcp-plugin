package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.archimatetool.mcp.json.JsonReader;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.server.ModelApi;
import com.archimatetool.mcp.service.ServiceRegistry;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IDiagramModel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LegacyViewAddElementHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        JsonReader jr = JsonReader.fromExchange(exchange);
        String viewId = jr.optString("id");
        String elementId = jr.optString("elementId");
        Integer bx = jr.optInt("x");
        Integer by = jr.optInt("y");
        Integer bw = jr.optInt("w");
        Integer bh = jr.optInt("h");
        int x = bx != null ? bx : 100;
        int y = by != null ? by : 100;
        int w = bw != null ? bw : 120;
        int h = bh != null ? bh : 80;
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
        Object vo = ServiceRegistry.activeModel().findById(model, viewId);
        Object eo = ServiceRegistry.activeModel().findById(model, elementId);
        if (!(vo instanceof IDiagramModel) || !(eo instanceof IArchimateElement)) { ResponseUtil.notFound(exchange, "view or element not found"); return; }
        IDiagramModel v = (IDiagramModel) vo;
        IArchimateElement el = (IArchimateElement) eo;
        var dmo = com.archimatetool.mcp.service.ServiceRegistry.views().addElementToView(v, el, x, y, w, h);
        ResponseUtil.ok(exchange, Map.of("objectId", dmo.getId()));
    }
}


