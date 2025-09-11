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

import java.io.IOException;
import java.util.Map;

import ru.cinimex.archimatetool.mcp.core.errors.*;
import ru.cinimex.archimatetool.mcp.server.JsonUtil;
import com.sun.net.httpserver.HttpExchange;

public final class ResponseUtil {
    private ResponseUtil() {}

    public static void json(HttpExchange exchange, int status, Object body) throws IOException {
        JsonUtil.writeJson(exchange, status, body);
    }

    public static void ok(HttpExchange exchange, Object body) throws IOException {
        json(exchange, 200, body);
    }

    public static void created(HttpExchange exchange, Object body) throws IOException {
        json(exchange, 201, body);
    }

    public static void noContent(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(204, -1);
    }

    public static void methodNotAllowed(HttpExchange exchange) throws IOException {
        exchange.sendResponseHeaders(405, -1);
    }

    public static void notFound(HttpExchange exchange, String msg) throws IOException {
        json(exchange, 404, Map.of("error", msg));
    }

    public static void badRequest(HttpExchange exchange, String msg) throws IOException {
        json(exchange, 400, Map.of("error", msg));
    }

    public static void conflictNoActiveModel(HttpExchange exchange) throws IOException {
        json(exchange, 409, Map.of("error", "no active model"));
    }

    public static void notImplemented(HttpExchange exchange, String msg) throws IOException {
        json(exchange, 501, Map.of("ok", false, "error", msg));
    }

    public static void unprocessable(HttpExchange exchange, String msg) throws IOException {
        json(exchange, 422, Map.of("error", msg));
    }

    public static void internalError(HttpExchange exchange, String msg) throws IOException {
        json(exchange, 500, Map.of("error", msg));
    }

    /** Map core exceptions to HTTP error responses. */
    public static void handleCoreException(HttpExchange exchange, CoreException ex) throws IOException {
        if (ex instanceof BadRequestException) {
            badRequest(exchange, ex.getMessage());
        } else if (ex instanceof NotFoundException) {
            notFound(exchange, ex.getMessage());
        } else if (ex instanceof ConflictException) {
            json(exchange, 409, Map.of("error", ex.getMessage()));
        } else if (ex instanceof UnprocessableException) {
            unprocessable(exchange, ex.getMessage());
        } else if (ex instanceof TimeoutException) {
            json(exchange, 504, Map.of("error", ex.getMessage()));
        } else {
            internalError(exchange, "internal error");
        }
    }
}


