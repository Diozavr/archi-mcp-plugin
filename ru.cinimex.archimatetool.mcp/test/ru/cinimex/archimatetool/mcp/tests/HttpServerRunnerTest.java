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

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.cinimex.archimatetool.mcp.Config;
import ru.cinimex.archimatetool.mcp.preferences.MCPPreferences;
import ru.cinimex.archimatetool.mcp.server.HttpServerRunner;

public class HttpServerRunnerTest {

    private Map<String, String> env;

    @Before
    public void setUp() throws Exception {
        // Clear any existing properties first
        System.clearProperty("archi.mcp.host");
        System.clearProperty("archi.mcp.port");
        MCPPreferences.clear();
        
        // Stop any running server from the main plugin
        try {
            ru.cinimex.archimatetool.mcp.Activator activator = ru.cinimex.archimatetool.mcp.Activator.getDefault();
            if (activator != null && activator.isServerRunning()) {
                activator.stopServer();
                Thread.sleep(200); // Wait for port to be released
            }
        } catch (Exception e) {
            // Ignore if activator is not available
        }
        
        env = new HashMap<>();
        Config.setGetenv(env::get);
    }

    @After
    public void tearDown() throws Exception {
        System.clearProperty("archi.mcp.host");
        System.clearProperty("archi.mcp.port");
        MCPPreferences.clear();
        Config.resetGetenv();
    }
    
    private HttpServerRunner createServerRunner() throws Exception {
        // Find an available port and use it immediately
        int testPort = findAvailablePort();
        System.setProperty("archi.mcp.port", String.valueOf(testPort));
        
        HttpServerRunner server = new HttpServerRunner();
        
        // Start the server immediately to claim the port
        server.start();
        
        return server;
    }
    
    private int findAvailablePort() throws IOException {
        // Try ports starting from 9000
        for (int port = 9000; port < 9999; port++) {
            try (ServerSocket socket = new ServerSocket(port)) {
                return port;
            } catch (IOException e) {
                // Port is in use, try next one
            }
        }
        throw new IOException("Could not find available port in range 9000-9999");
    }
    
    private void stopServer(HttpServerRunner server) throws Exception {
        if (server != null && server.isRunning()) {
            server.stop();
            // Wait for port to be released
            Thread.sleep(100);
        }
    }

    @Test
    public void hostResolutionAndServerBinding() throws Exception {
        HttpServerRunner server;
        
        // Test 1: Default host binding
        System.clearProperty("archi.mcp.host");
        MCPPreferences.clear();
        server = createServerRunner();
        assertEquals(Config.DEFAULT_HOST, server.getHost());
        assertTrue(server.isRunning());
        stopServer(server);
        
        // Test 2: System property host binding
        System.setProperty("archi.mcp.host", "0.0.0.0");
        server = createServerRunner();
        assertEquals("0.0.0.0", server.getHost());
        assertTrue(server.isRunning());
        stopServer(server);
        
        // Test 3: Environment variable host binding
        System.clearProperty("archi.mcp.host");
        env.put("ARCHI_MCP_HOST", "0.0.0.0");
        server = createServerRunner();
        assertEquals("0.0.0.0", server.getHost());
        assertTrue(server.isRunning());
        stopServer(server);
        
        // Test 4: Preference host binding
        env.clear();
        MCPPreferences.setHost("localhost");
        server = createServerRunner();
        assertEquals("localhost", server.getHost());
        assertTrue(server.isRunning());
        stopServer(server);
        
        // Test 5: System property overrides env
        env.put("ARCHI_MCP_HOST", "0.0.0.0");
        System.setProperty("archi.mcp.host", "127.0.0.1");
        server = createServerRunner();
        assertEquals("127.0.0.1", server.getHost());
        assertTrue(server.isRunning());
        stopServer(server);
        
        // Test 6: Env overrides preferences
        System.clearProperty("archi.mcp.host");
        MCPPreferences.setHost("localhost");
        env.put("ARCHI_MCP_HOST", "0.0.0.0");
        server = createServerRunner();
        assertEquals("0.0.0.0", server.getHost());
        assertTrue(server.isRunning());
        stopServer(server);
        
        // Test 7: Server state management
        server = createServerRunner();
        assertNotNull(server.getHost());
        assertTrue(server.isRunning());
        
        server.stop();
        assertNotNull(server.getHost()); // Should still be available (cached)
        assertFalse(server.isRunning());
        
        // Test 8: Exception handling
        server = createServerRunner();
        try {
            server.start(); // Should throw exception
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        } finally {
            stopServer(server);
        }
        
        try {
            server.stop(); // Should throw exception
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
        
        // Test 9: Different host types
        System.setProperty("archi.mcp.host", "127.0.0.1");
        server = createServerRunner();
        assertEquals("127.0.0.1", server.getHost());
        assertTrue(server.getPort() > 0);
        stopServer(server);
        
        System.setProperty("archi.mcp.host", "0.0.0.0");
        server = createServerRunner();
        assertEquals("0.0.0.0", server.getHost());
        assertTrue(server.getPort() > 0);
        stopServer(server);
    }
}
