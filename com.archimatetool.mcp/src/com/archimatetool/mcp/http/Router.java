package com.archimatetool.mcp.http;

import com.archimatetool.mcp.http.handlers.StatusHttpHandler;
import com.archimatetool.mcp.http.handlers.OpenApiHttpHandler;
import com.archimatetool.mcp.core.script.ScriptingCore;
import com.archimatetool.mcp.http.handlers.ScriptEnginesHttpHandler;
import com.archimatetool.mcp.http.handlers.ScriptRunHttpHandler;
import com.archimatetool.mcp.http.handlers.TypesHttpHandler;
import com.archimatetool.mcp.http.handlers.FoldersHttpHandler;
import com.archimatetool.mcp.http.handlers.FolderEnsureHttpHandler;
import com.archimatetool.mcp.http.handlers.ElementsHttpHandler;
import com.archimatetool.mcp.http.handlers.RelationsHttpHandler;
import com.archimatetool.mcp.http.handlers.ViewsHttpHandler;
import com.archimatetool.mcp.http.handlers.ViewItemHttpHandler;
import com.archimatetool.mcp.http.handlers.ModelSaveHttpHandler;
import com.archimatetool.mcp.http.handlers.LegacyViewContentHttpHandler;
import com.archimatetool.mcp.http.handlers.LegacyViewAddElementHttpHandler;
import com.archimatetool.mcp.http.handlers.SearchHttpHandler;
import com.archimatetool.mcp.http.handlers.JsonRpcHttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Central place to register contexts and legacy aliases.
 */
public final class Router {
    private Router() {}

    public static void registerAll(HttpServer server) {
        server.createContext("/status", new StatusHttpHandler());
        server.createContext("/openapi.json", new OpenApiHttpHandler());
        ScriptingCore scriptingCore = new ScriptingCore();
        server.createContext("/script/engines", new ScriptEnginesHttpHandler(scriptingCore));
        server.createContext("/script/run", new ScriptRunHttpHandler(scriptingCore));
        server.createContext("/types", new TypesHttpHandler());
        server.createContext("/folders", new FoldersHttpHandler());
        server.createContext("/folder/ensure", new FolderEnsureHttpHandler());
        server.createContext("/elements", new ElementsHttpHandler());
        server.createContext("/relations", new RelationsHttpHandler());
        server.createContext("/views", new ViewsHttpHandler());
        server.createContext("/views/", new ViewItemHttpHandler());
        server.createContext("/search", new SearchHttpHandler());
        server.createContext("/views/content", new LegacyViewContentHttpHandler());
        server.createContext("/views/add-element", new LegacyViewAddElementHttpHandler());
        server.createContext("/model/save", new ModelSaveHttpHandler());
        server.createContext("/mcp", new JsonRpcHttpHandler());
        // Остальные хендлеры будут добавляться по мере рефакторинга
    }
}


