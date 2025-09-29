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
package ru.cinimex.archimatetool.mcp.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import ru.cinimex.archimatetool.mcp.Config;
import ru.cinimex.archimatetool.mcp.http.Router;
import ru.cinimex.archimatetool.mcp.util.McpLogger;
import com.sun.net.httpserver.HttpServer;

public class HttpServerRunner {
    private String host = null;

    private HttpServer server;
    private int port = -1;

    public synchronized void start() throws IOException {
        McpLogger.logOperationCall("HTTP Server Start");
        
        if (server != null) {
            McpLogger.logOperationWarning("HTTP Server Start", "Server already running");
            throw new IllegalStateException("Server already running");
        }
        int p = Config.resolvePort();
        host = Config.resolveHost();
        
        McpLogger.logOperationInput("HTTP Server Start", 
            java.util.Map.of("host", host, "port", p));
        
        try {
            server = HttpServer.create(new InetSocketAddress(host, p), 0);
            port = p;
        } catch (IOException ex) {
            McpLogger.logOperationError("HTTP Server Start", ex);
            throw new IOException("Port in use: " + p, ex);
        }

        Router.registerAll(server);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        
        McpLogger.logOperationOutput("HTTP Server Start", 
            java.util.Map.of("status", "started", "host", host, "port", port));
        System.out.println("[Archi MCP] Listening at http://" + host + ":" + port);
    }

    public synchronized void stop() {
        McpLogger.logOperationCall("HTTP Server Stop");
        
        if (server == null) {
            McpLogger.logOperationWarning("HTTP Server Stop", "Server not running");
            throw new IllegalStateException("Server not running");
        }
        
        int currentPort = port;
        McpLogger.logOperationInput("HTTP Server Stop", 
            java.util.Map.of("host", host, "port", currentPort));
        
        try {
            server.stop(0);
            server = null;
            port = -1;
            
            McpLogger.logOperationOutput("HTTP Server Stop", 
                java.util.Map.of("status", "stopped", "host", host, "port", currentPort));
            System.out.println("[Archi MCP] HTTP Server stopped at " + host + ":" + currentPort);
        } catch (Exception ex) {
            McpLogger.logOperationError("HTTP Server Stop", ex);
            throw ex;
        }
    }

    public synchronized void restart() throws IOException {
        McpLogger.logOperationCall("HTTP Server Restart");
        
        try {
            stop();
            start();
            McpLogger.logOperationOutput("HTTP Server Restart", 
                java.util.Map.of("status", "restarted", "host", host, "port", port));
        } catch (Exception ex) {
            McpLogger.logOperationError("HTTP Server Restart", ex);
            throw ex;
        }
    }

    public synchronized boolean isRunning() {
        return server != null;
    }

    public synchronized int getPort() {
        return port;
    }

    public synchronized String getHost() {
        return host;
    }
}

