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
package ru.cinimex.archimatetool.mcp.http.handlers;

import java.io.IOException;
import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import ru.cinimex.archimatetool.mcp.core.types.GetViewContentQuery;
import ru.cinimex.archimatetool.mcp.core.views.ViewsCore;
import ru.cinimex.archimatetool.mcp.http.QueryParams;
import ru.cinimex.archimatetool.mcp.http.ResponseUtil;
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


