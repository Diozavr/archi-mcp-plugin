package ru.cinimex.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import ru.cinimex.archimatetool.mcp.core.script.ScriptingCore;
import ru.cinimex.archimatetool.mcp.core.script.ScriptRequest;
import ru.cinimex.archimatetool.mcp.core.script.ScriptResult;
import ru.cinimex.archimatetool.mcp.http.ResponseUtil;
import ru.cinimex.archimatetool.mcp.json.JsonReader;
import ru.cinimex.archimatetool.mcp.server.JacksonJson;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** HTTP handler for POST /script/run. */
public class ScriptRunHttpHandler implements HttpHandler {
    private final ScriptingCore core;

    public ScriptRunHttpHandler(ScriptingCore core) {
        this.core = core;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            ResponseUtil.methodNotAllowed(exchange);
            return;
        }
        if (!core.isPluginInstalled()) {
            ResponseUtil.notImplemented(exchange, "Not Implemented: install a compatible jArchi to enable /script APIs");
            return;
        }
        JsonReader jr = JsonReader.fromExchange(exchange);
        String code = jr.optString("code");
        if (code == null || code.isBlank()) {
            ResponseUtil.badRequest(exchange, "code is required");
            return;
        }
        String engine = jr.optString("engine", "ajs");
        if (!core.listEngines().contains(engine)) {
            ResponseUtil.badRequest(exchange, "unknown engine");
            return;
        }
        Integer timeoutMs = jr.optInt("timeoutMs");
        if (timeoutMs != null && (timeoutMs <= 0 || timeoutMs > 60_000)) {
            ResponseUtil.badRequest(exchange, "invalid timeoutMs");
            return;
        }
        JsonNode bindingsNode = jr.optObject("bindings");
        Map<String, Object> bindings = null;
        if (bindingsNode != null) {
            bindings = JacksonJson.mapper().convertValue(bindingsNode, Map.class);
        }
        String modelId = jr.optString("modelId");
        String log = jr.optString("log");
        if (log != null && !log.isEmpty() && !"stdout".equals(log) && !"script".equals(log)) {
            ResponseUtil.badRequest(exchange, "invalid log parameter");
            return;
        }
        ScriptRequest req = new ScriptRequest(engine, code, timeoutMs, bindings, modelId, log);
        try {
            ScriptResult result = core.run(req);
            Map<String, Object> resp = new HashMap<>();
            resp.put("ok", result.ok());
            if (result.result() != null) resp.put("result", result.result());
            if (result.stdout() != null) resp.put("stdout", result.stdout());
            if (result.stderr() != null) resp.put("stderr", result.stderr());
            resp.put("durationMs", result.durationMs());
            ResponseUtil.ok(exchange, resp);
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        } catch (UnsupportedOperationException ex) {
            ResponseUtil.notImplemented(exchange, ex.getMessage() != null ? ex.getMessage() : "not implemented");
        }
    }
}
