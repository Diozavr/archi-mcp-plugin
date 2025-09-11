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
import java.util.Map;

import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import ru.cinimex.archimatetool.mcp.core.search.SearchCore;
import ru.cinimex.archimatetool.mcp.core.types.SearchQuery;
import ru.cinimex.archimatetool.mcp.http.QueryParams;
import ru.cinimex.archimatetool.mcp.http.ResponseUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/** HTTP handler delegating search to the core layer. */
public class SearchHttpHandler implements HttpHandler {
    private final SearchCore core = new SearchCore();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        QueryParams qp = QueryParams.from(exchange);
        SearchQuery q = new SearchQuery();
        q.q = qp.first("q");
        q.kind = qp.first("kind");
        q.elementType = qp.first("elementType");
        q.relationType = qp.first("relationType");
        q.modelId = qp.first("modelId");
        q.includeDocs = qp.getBool("includeDocs", false);
        q.includeProps = qp.getBool("includeProps", false);
        q.limit = qp.getInt("limit", 100);
        q.offset = qp.getInt("offset", 0);
        q.debug = qp.getBool("debug", false);
        String logTarget = qp.first("log") != null ? qp.first("log") : qp.first("logTarget");
        q.logTarget = logTarget != null ? logTarget : "stdout";
        for (String v : qp.all("property")) {
            int eq = v.indexOf('=');
            if (eq > 0) {
                String pk = v.substring(0, eq);
                String pv = v.substring(eq + 1);
                q.propertyFilters.put(pk, pv);
            }
        }
        try {
            Map<String,Object> resp = core.search(q);
            ResponseUtil.ok(exchange, resp);
        } catch (CoreException ex) {
            ResponseUtil.handleCoreException(exchange, ex);
        }
    }
}
