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
package ru.cinimex.archimatetool.mcp.tests;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpPrincipal;

public class FakeHttpExchange extends HttpExchange {
    private final Headers requestHeaders = new Headers();
    private final Headers responseHeaders = new Headers();
    private final ByteArrayInputStream requestBody;
    private final ByteArrayOutputStream responseBody = new ByteArrayOutputStream();
    private final String method;
    private final URI uri;
    private int responseCode;

    public FakeHttpExchange(String method, String path, String body) {
        this.method = method;
        this.uri = URI.create(path);
        this.requestBody = new ByteArrayInputStream(body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8));
    }

    @Override public Headers getRequestHeaders() { return requestHeaders; }
    @Override public Headers getResponseHeaders() { return responseHeaders; }
    @Override public URI getRequestURI() { return uri; }
    @Override public String getRequestMethod() { return method; }
    @Override public HttpContext getHttpContext() { return null; }
    @Override public void close() {}
    @Override public InputStream getRequestBody() { return requestBody; }
    @Override public OutputStream getResponseBody() { return responseBody; }
    @Override public void sendResponseHeaders(int rCode, long responseLength) throws IOException { responseCode = rCode; }
    @Override public InetSocketAddress getRemoteAddress() { return null; }
    @Override public InetSocketAddress getLocalAddress() { return null; }
    @Override public String getProtocol() { return "HTTP/1.1"; }
    @Override public Object getAttribute(String name) { return null; }
    @Override public void setAttribute(String name, Object value) {}
    @Override public void setStreams(InputStream i, OutputStream o) {}
    @Override public HttpPrincipal getPrincipal() { return null; }

    public int getResponseCode() { return responseCode; }
    public String getResponseString() { return responseBody.toString(StandardCharsets.UTF_8); }
}
