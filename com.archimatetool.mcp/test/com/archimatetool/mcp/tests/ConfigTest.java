package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import com.archimatetool.mcp.Config;
import com.archimatetool.mcp.preferences.MCPPreferences;

public class ConfigTest {

    @Before
    public void reset() throws Exception {
        System.clearProperty("archi.mcp.port");
        IEclipsePreferences prefs = MCPPreferences.getPreferences();
        prefs.remove(MCPPreferences.PREF_PORT);
        prefs.flush();
        setEnv("ARCHI_MCP_PORT", null);
    }

    @After
    public void cleanup() throws Exception {
        reset();
    }

    @Test
    public void defaultPort() {
        assertEquals(Config.DEFAULT_PORT, Config.resolvePort());
    }

    @Test
    public void preferencePort() throws Exception {
        IEclipsePreferences prefs = MCPPreferences.getPreferences();
        prefs.putInt(MCPPreferences.PREF_PORT, 9001);
        prefs.flush();
        assertEquals(9001, Config.resolvePort());
    }

    @Test
    public void envOverridesPrefs() throws Exception {
        IEclipsePreferences prefs = MCPPreferences.getPreferences();
        prefs.putInt(MCPPreferences.PREF_PORT, 9001);
        prefs.flush();
        setEnv("ARCHI_MCP_PORT", "9002");
        assertEquals(9002, Config.resolvePort());
    }

    @Test
    public void systemPropertyOverridesEnv() throws Exception {
        setEnv("ARCHI_MCP_PORT", "9002");
        System.setProperty("archi.mcp.port", "9003");
        assertEquals(9003, Config.resolvePort());
    }

    private static void setEnv(String key, String value) throws Exception {
        try {
            java.lang.reflect.Field field = java.lang.ProcessEnvironment.class.getDeclaredField("theEnvironment");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> env = (java.util.Map<String, String>) field.get(null);
            if (value == null) {
                env.remove(key);
            } else {
                env.put(key, value);
            }
            field = java.lang.ProcessEnvironment.class.getDeclaredField("theCaseInsensitiveEnvironment");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> cienv = (java.util.Map<String, String>) field.get(null);
            if (value == null) {
                cienv.remove(key);
            } else {
                cienv.put(key, value);
            }
        } catch (NoSuchFieldException e) {
            java.lang.reflect.Field field = Class.forName("java.util.Collections$UnmodifiableMap").getDeclaredField("m");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> env = (java.util.Map<String, String>) field.get(System.getenv());
            if (value == null) {
                env.remove(key);
            } else {
                env.put(key, value);
            }
        }
    }
}
