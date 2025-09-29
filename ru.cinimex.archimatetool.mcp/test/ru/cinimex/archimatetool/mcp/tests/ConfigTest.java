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

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.cinimex.archimatetool.mcp.Config;
import ru.cinimex.archimatetool.mcp.preferences.MCPPreferences;

public class ConfigTest {

    @Before
    public void reset() throws Exception {
        System.clearProperty("archi.mcp.port");
        System.clearProperty("archi.mcp.host");
        MCPPreferences.clear();
        env.clear();
        Config.setGetenv(env::get);
    }

    @After
    public void cleanup() throws Exception {
        System.clearProperty("archi.mcp.port");
        System.clearProperty("archi.mcp.host");
        MCPPreferences.clear();
        env.clear();
        Config.resetGetenv();
    }

    @Test
    public void defaultPort() {
        assertEquals(Config.DEFAULT_PORT, Config.resolvePort());
    }

    @Test
    public void preferencePort() throws Exception {
        MCPPreferences.setPort(9001);
        assertEquals(9001, Config.resolvePort());
    }

    @Test
    public void envOverridesPrefs() throws Exception {
        MCPPreferences.setPort(9001);
        env.put("ARCHI_MCP_PORT", "9002");
        assertEquals(9002, Config.resolvePort());
    }

    @Test
    public void systemPropertyOverridesEnv() throws Exception {
        env.put("ARCHI_MCP_PORT", "9002");
        System.setProperty("archi.mcp.port", "9003");
        assertEquals(9003, Config.resolvePort());
    }

    // Host resolution tests
    @Test
    public void defaultHost() {
        assertEquals(Config.DEFAULT_HOST, Config.resolveHost());
    }

    @Test
    public void preferenceHost() throws Exception {
        MCPPreferences.setHost("192.168.1.100");
        assertEquals("192.168.1.100", Config.resolveHost());
    }

    @Test
    public void envOverridesPrefsHost() throws Exception {
        MCPPreferences.setHost("192.168.1.100");
        env.put("ARCHI_MCP_HOST", "192.168.1.200");
        assertEquals("192.168.1.200", Config.resolveHost());
    }

    @Test
    public void systemPropertyOverridesEnvHost() throws Exception {
        env.put("ARCHI_MCP_HOST", "192.168.1.200");
        System.setProperty("archi.mcp.host", "0.0.0.0");
        assertEquals("0.0.0.0", Config.resolveHost());
    }

    @Test
    public void emptySystemPropertyFallsBackToEnv() throws Exception {
        System.setProperty("archi.mcp.host", "");
        env.put("ARCHI_MCP_HOST", "192.168.1.200");
        assertEquals("192.168.1.200", Config.resolveHost());
    }

    @Test
    public void emptyEnvFallsBackToPrefs() throws Exception {
        env.put("ARCHI_MCP_HOST", "");
        MCPPreferences.setHost("192.168.1.100");
        assertEquals("192.168.1.100", Config.resolveHost());
    }

    @Test
    public void nullEnvFallsBackToPrefs() throws Exception {
        env.put("ARCHI_MCP_HOST", null);
        MCPPreferences.setHost("192.168.1.100");
        assertEquals("192.168.1.100", Config.resolveHost());
    }

    private static final Map<String, String> env = new HashMap<>();
}
