package ru.cinimex.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.Map;

import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import ru.cinimex.archimatetool.mcp.core.folders.FoldersCore;
import ru.cinimex.archimatetool.mcp.core.types.EnsureFolderCmd;
import ru.cinimex.archimatetool.mcp.http.ResponseUtil;
import ru.cinimex.archimatetool.mcp.json.JsonReader;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** HTTP handler for ensuring a folder path exists. */
public class FolderEnsureHttpHandler implements HttpHandler {
    private final FoldersCore core = new FoldersCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        JsonReader jr = JsonReader.fromExchange(exchange);
        EnsureFolderCmd cmd = new EnsureFolderCmd(jr.optString("path"));
        try {
            Map<String,Object> node = core.ensureFolder(cmd);
            ResponseUtil.ok(exchange, node);
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }
}


