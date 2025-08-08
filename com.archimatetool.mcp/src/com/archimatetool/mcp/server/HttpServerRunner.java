package com.archimatetool.mcp.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.archimatetool.mcp.Config;
import com.archimatetool.mcp.http.Router;
import com.sun.net.httpserver.HttpServer;

public class HttpServerRunner {
    private static final String HOST = Config.DEFAULT_HOST;

    private HttpServer server;

    public void start() throws IOException {
        int port = Config.resolvePort();
        IOException last = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            int p = port + attempt;
            try {
                server = HttpServer.create(new InetSocketAddress(HOST, p), 0);
                port = p;
                last = null;
                break;
            } catch (IOException ex) {
                last = ex;
            }
        }
        if (server == null) {
            if (last != null) throw last;
            throw new IOException("Failed to bind Archi MCP HTTP server");
        }

        Router.registerAll(server);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        System.out.println("[Archi MCP] Listening at http://" + HOST + ":" + port);
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }
}


