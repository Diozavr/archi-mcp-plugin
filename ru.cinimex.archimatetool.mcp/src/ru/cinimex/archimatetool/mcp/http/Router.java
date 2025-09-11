/*
 * Copyright 2025 Cinimex
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.cinimex.archimatetool.mcp.http;

import ru.cinimex.archimatetool.mcp.http.handlers.StatusHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.OpenApiHttpHandler;
import ru.cinimex.archimatetool.mcp.core.script.ScriptingCore;
import ru.cinimex.archimatetool.mcp.http.handlers.ScriptEnginesHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.ScriptRunHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.TypesHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.FoldersHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.FolderEnsureHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.ElementsHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.RelationsHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.ViewsHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.ViewItemHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.ModelSaveHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.LegacyViewContentHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.LegacyViewAddElementHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.SearchHttpHandler;
import ru.cinimex.archimatetool.mcp.http.handlers.JsonRpcHttpHandler;
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


