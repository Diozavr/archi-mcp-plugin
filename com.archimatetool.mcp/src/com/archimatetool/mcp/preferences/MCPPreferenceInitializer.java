package com.archimatetool.mcp.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import com.archimatetool.mcp.Config;

/**
 * Sets default values for MCP preferences.
 */
public class MCPPreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        MCPPreferences.setDefaultHostAndPort(Config.DEFAULT_HOST, Config.DEFAULT_PORT);
    }
}
