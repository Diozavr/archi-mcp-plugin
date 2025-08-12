package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.net.URI;

import com.archimatetool.mcp.core.elements.ElementsCore;
import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.types.DeleteElementCmd;
import com.archimatetool.mcp.core.types.GetElementQuery;
import com.archimatetool.mcp.core.types.ListElementRelationsQuery;
import com.archimatetool.mcp.core.types.UpdateElementCmd;
import com.archimatetool.mcp.http.QueryParams;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.json.JsonReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** HTTP handler for element item operations. */
public class ElementItemHttpHandler implements HttpHandler {
    private final ElementsCore core = new ElementsCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String remainder = uri.getPath().substring("/elements/".length());
        String id;
        String subpath = null;
        int slash = remainder.indexOf('/');
        if (slash >= 0) {
            id = remainder.substring(0, slash);
            subpath = remainder.substring(slash + 1);
        } else {
            id = remainder;
        }

        String method = exchange.getRequestMethod();

        if (subpath != null && !subpath.isEmpty()) {
            if ("relations".equals(subpath) && "GET".equalsIgnoreCase(method)) {
                QueryParams qp = QueryParams.from(exchange);
                String direction = qp.first("direction");
                boolean includeElements = qp.getBool("includeElements", false);
                ListElementRelationsQuery q = new ListElementRelationsQuery(id, direction, includeElements);
                try {
                    var dto = core.listRelations(q);
                    ResponseUtil.ok(exchange, dto);
                } catch (CoreException ex) {
                    ResponseUtil.handleCoreException(exchange, ex);
                }
                return;
            }
            ResponseUtil.methodNotAllowed(exchange);
            return;
        }

        if ("GET".equalsIgnoreCase(method)) {
            QueryParams qp = QueryParams.from(exchange);
            String include = qp.first("include");
            if (include == null) include = qp.first("with");
            boolean includeRelations = false;
            if (include != null) {
                for (String s : include.split(",")) {
                    if (s.trim().equalsIgnoreCase("relations")) {
                        includeRelations = true;
                        break;
                    }
                }
            }
            boolean includeElements = qp.getBool("includeElements", false);
            GetElementQuery q = new GetElementQuery(id, includeRelations, includeElements);
            try {
                var dto = core.getElement(q);
                ResponseUtil.ok(exchange, dto);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        } else if ("PATCH".equalsIgnoreCase(method)) {
            JsonReader jr = JsonReader.fromExchange(exchange);
            UpdateElementCmd cmd = new UpdateElementCmd(id, jr.optString("name"));
            try {
                var dto = core.updateElement(cmd);
                ResponseUtil.ok(exchange, dto);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        } else if ("DELETE".equalsIgnoreCase(method)) {
            DeleteElementCmd cmd = new DeleteElementCmd(id);
            try {
                core.deleteElement(cmd);
                ResponseUtil.noContent(exchange);
            } catch (CoreException ex) {
                ResponseUtil.handleCoreException(exchange, ex);
            }
            return;
        }

        ResponseUtil.methodNotAllowed(exchange);
    }
}
