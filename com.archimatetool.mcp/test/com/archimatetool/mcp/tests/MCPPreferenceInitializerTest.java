package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import com.archimatetool.mcp.Config;
import com.archimatetool.mcp.preferences.MCPPreferenceInitializer;
import com.archimatetool.mcp.preferences.MCPPreferences;

public class MCPPreferenceInitializerTest {

    @Before
    public void resetDefaults() throws Exception {
        MCPPreferences.clearDefaults();
    }

    @Test
    public void setsDefaultHostAndPort() throws Exception {
        new MCPPreferenceInitializer().initializeDefaultPreferences();
        assertEquals(Config.DEFAULT_HOST, MCPPreferences.getDefaultHost());
        assertEquals(Config.DEFAULT_PORT, MCPPreferences.getDefaultPort());
    }
}
