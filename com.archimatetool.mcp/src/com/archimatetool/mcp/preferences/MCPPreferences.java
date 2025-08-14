package com.archimatetool.mcp.preferences;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import com.archimatetool.mcp.Config;

/**
 * Utility access to MCP plugin preferences with a graceful fallback when the
 * Eclipse preferences service is unavailable (e.g. during headless tests).
 */
public final class MCPPreferences {

    private MCPPreferences() {}

    public static final String NODE = "com.archimatetool.mcp";
    public static final String PREF_HOST = "host";
    public static final String PREF_PORT = "port";

    private static final Map<String, String> FALLBACK = new HashMap<>();
    private static final Map<String, String> FALLBACK_DEFAULTS = new HashMap<>();

    private static IEclipsePreferences instanceNode() {
        try {
            return InstanceScope.INSTANCE.getNode(NODE);
        } catch (Throwable t) {
            return null;
        }
    }

    private static IEclipsePreferences defaultNode() {
        try {
            return DefaultScope.INSTANCE.getNode(NODE);
        } catch (Throwable t) {
            return null;
        }
    }

    /** Clear stored host/port preferences. */
    public static void clear() {
        IEclipsePreferences node = instanceNode();
        if (node != null) {
            node.remove(PREF_HOST);
            node.remove(PREF_PORT);
            try {
                node.flush();
            } catch (BackingStoreException ignore) {}
        } else {
            FALLBACK.clear();
        }
    }

    /** Set the persisted port preference. */
    public static void setPort(int port) {
        IEclipsePreferences node = instanceNode();
        if (node != null) {
            node.putInt(PREF_PORT, port);
            try {
                node.flush();
            } catch (BackingStoreException ignore) {}
        } else {
            FALLBACK.put(PREF_PORT, Integer.toString(port));
        }
    }

    /** Retrieve the persisted port preference or the default if unset. */
    public static int getPort() {
        IEclipsePreferences node = instanceNode();
        if (node != null) {
            return node.getInt(PREF_PORT, Config.DEFAULT_PORT);
        }
        String val = FALLBACK.get(PREF_PORT);
        return val != null ? Integer.parseInt(val) : Config.DEFAULT_PORT;
    }

    /** Set default host and port values. */
    public static void setDefaultHostAndPort(String host, int port) {
        IEclipsePreferences node = defaultNode();
        if (node != null) {
            node.put(PREF_HOST, host);
            node.putInt(PREF_PORT, port);
            try {
                node.flush();
            } catch (BackingStoreException ignore) {}
        } else {
            FALLBACK_DEFAULTS.put(PREF_HOST, host);
            FALLBACK_DEFAULTS.put(PREF_PORT, Integer.toString(port));
        }
    }

    /** Clear default preference values. */
    public static void clearDefaults() {
        IEclipsePreferences node = defaultNode();
        if (node != null) {
            try {
                node.clear();
                node.flush();
            } catch (BackingStoreException ignore) {}
        } else {
            FALLBACK_DEFAULTS.clear();
        }
    }

    /** Retrieve the default host value. */
    public static String getDefaultHost() {
        IEclipsePreferences node = defaultNode();
        if (node != null) {
            return node.get(PREF_HOST, Config.DEFAULT_HOST);
        }
        return FALLBACK_DEFAULTS.getOrDefault(PREF_HOST, Config.DEFAULT_HOST);
    }

    /** Retrieve the default port value. */
    public static int getDefaultPort() {
        IEclipsePreferences node = defaultNode();
        if (node != null) {
            return node.getInt(PREF_PORT, Config.DEFAULT_PORT);
        }
        String val = FALLBACK_DEFAULTS.get(PREF_PORT);
        return val != null ? Integer.parseInt(val) : Config.DEFAULT_PORT;
    }
}
