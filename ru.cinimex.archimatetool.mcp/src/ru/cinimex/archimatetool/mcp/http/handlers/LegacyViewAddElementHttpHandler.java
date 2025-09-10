package ru.cinimex.archimatetool.mcp.http.handlers;

import java.io.IOException;

import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import java.util.List;

import ru.cinimex.archimatetool.mcp.core.types.AddElementToViewItem;
import ru.cinimex.archimatetool.mcp.core.types.AddElementsToViewCmd;
import ru.cinimex.archimatetool.mcp.core.views.ViewsCore;
import ru.cinimex.archimatetool.mcp.http.ResponseUtil;
import ru.cinimex.archimatetool.mcp.json.JsonReader;
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
        AddElementToViewItem item = new AddElementToViewItem(elementId, null, x, y, w, h, null);
        AddElementsToViewCmd cmd = new AddElementsToViewCmd(viewId, List.of(item));
        try {
            var res = core.addElements(cmd).get(0);
            ResponseUtil.ok(exchange, res);
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }
}


