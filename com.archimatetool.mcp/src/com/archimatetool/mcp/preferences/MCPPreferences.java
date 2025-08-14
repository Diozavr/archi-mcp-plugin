package com.archimatetool.mcp.preferences;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * Utility access to MCP plugin preferences.
 */
public final class MCPPreferences {

    private MCPPreferences() {}

    public static final String NODE = "com.archimatetool.mcp";
    public static final String PREF_HOST = "host";
    public static final String PREF_PORT = "port";

    public static IEclipsePreferences getPreferences() {
        return InstanceScope.INSTANCE.getNode(NODE);
    }
}
