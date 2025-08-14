package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.archimatetool.mcp.Config;
import com.archimatetool.mcp.preferences.MCPPreferences;

public class ConfigTest {

    @Before
    public void reset() throws Exception {
        System.clearProperty("archi.mcp.port");
        MCPPreferences.clear();
        env.clear();
        Config.setGetenv(env::get);
    }

    @After
    public void cleanup() throws Exception {
        System.clearProperty("archi.mcp.port");
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

    private static final Map<String, String> env = new HashMap<>();
}
