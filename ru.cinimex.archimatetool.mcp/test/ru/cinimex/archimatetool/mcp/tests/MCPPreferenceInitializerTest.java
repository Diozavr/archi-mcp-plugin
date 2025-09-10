package ru.cinimex.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import ru.cinimex.archimatetool.mcp.Config;
import ru.cinimex.archimatetool.mcp.preferences.MCPPreferenceInitializer;
import ru.cinimex.archimatetool.mcp.preferences.MCPPreferences;

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
