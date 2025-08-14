package com.archimatetool.mcp.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;

import com.archimatetool.mcp.Config;

/**
 * Sets default values for MCP preferences.
 */
public class MCPPreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        IEclipsePreferences prefs = DefaultScope.INSTANCE.getNode(MCPPreferences.NODE);
        prefs.put(MCPPreferences.PREF_HOST, Config.DEFAULT_HOST);
        prefs.putInt(MCPPreferences.PREF_PORT, Config.DEFAULT_PORT);
    }
}
