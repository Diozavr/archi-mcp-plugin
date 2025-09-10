package ru.cinimex.archimatetool.mcp.server;

import java.io.IOException;
import java.net.InetSocketAddress;

import ru.cinimex.archimatetool.mcp.Config;
import ru.cinimex.archimatetool.mcp.http.Router;
import com.sun.net.httpserver.HttpServer;

public class HttpServerRunner {
    private static final String HOST = Config.DEFAULT_HOST;

    private HttpServer server;
    private int port = -1;

    public synchronized void start() throws IOException {
        if (server != null) {
            throw new IllegalStateException("Server already running");
        }
        int p = Config.resolvePort();
        try {
            server = HttpServer.create(new InetSocketAddress(HOST, p), 0);
            port = p;
        } catch (IOException ex) {
            throw new IOException("Port in use: " + p, ex);
        }

        Router.registerAll(server);
        server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
        server.start();
        System.out.println("[Archi MCP] Listening at http://" + HOST + ":" + port);
    }

    public synchronized void stop() {
        if (server == null) {
            throw new IllegalStateException("Server not running");
        }
        server.stop(0);
        server = null;
        port = -1;
    }

    public synchronized void restart() throws IOException {
        stop();
        start();
    }

    public synchronized boolean isRunning() {
        return server != null;
    }

    public synchronized int getPort() {
        return port;
    }
}

