package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.types.GetViewContentQuery;
import com.archimatetool.mcp.core.views.ViewsCore;
import com.archimatetool.mcp.http.QueryParams;
import com.archimatetool.mcp.http.ResponseUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class LegacyViewContentHttpHandler implements HttpHandler {
    private final ViewsCore core = new ViewsCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        QueryParams qp = QueryParams.from(exchange);
        String id = qp.first("id");
        GetViewContentQuery q = new GetViewContentQuery(id);
        try {
            var dto = core.getViewContent(q);
            ResponseUtil.ok(exchange, dto);
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }
}


