package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import com.archimatetool.mcp.Config;
import com.archimatetool.mcp.preferences.MCPPreferenceInitializer;
import com.archimatetool.mcp.preferences.MCPPreferences;

public class MCPPreferenceInitializerTest {

    @Before
    public void resetDefaults() throws Exception {
        IEclipsePreferences prefs = DefaultScope.INSTANCE.getNode(MCPPreferences.NODE);
        prefs.clear();
        prefs.flush();
    }

    @Test
    public void setsDefaultHostAndPort() throws Exception {
        new MCPPreferenceInitializer().initializeDefaultPreferences();
        IEclipsePreferences defaults = DefaultScope.INSTANCE.getNode(MCPPreferences.NODE);
        assertEquals(Config.DEFAULT_HOST, defaults.get(MCPPreferences.PREF_HOST, null));
        assertEquals(Config.DEFAULT_PORT, defaults.getInt(MCPPreferences.PREF_PORT, -1));
    }
}
