package com.archimatetool.mcp.http.handlers;

import java.io.IOException;

import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.types.AddElementToViewCmd;
import com.archimatetool.mcp.core.views.ViewsCore;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LegacyViewAddElementHttpHandler implements HttpHandler {
    private final ViewsCore core = new ViewsCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        JsonReader jr = JsonReader.fromExchange(exchange);
        String viewId = jr.optString("id");
        String elementId = jr.optString("elementId");
        Integer x = jr.optInt("x");
        Integer y = jr.optInt("y");
        Integer w = jr.optInt("w");
        Integer h = jr.optInt("h");
        AddElementToViewCmd cmd = new AddElementToViewCmd(viewId, elementId, null, x, y, w, h);
        try {
            var res = core.addElement(cmd);
            ResponseUtil.ok(exchange, res);
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }
}


