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
package ru.cinimex.archimatetool.mcp.preferences;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.osgi.service.prefs.BackingStoreException;

import ru.cinimex.archimatetool.mcp.Config;

/**
 * Utility access to MCP plugin preferences with a graceful fallback when the
 * Eclipse preferences service is unavailable (e.g. during headless tests).
 */
public final class MCPPreferences {

    private MCPPreferences() {}

    public static final String NODE = "ru.cinimex.archimatetool.mcp";
    public static final String PREF_HOST = "host";
    public static final String PREF_PORT = "port";
    public static final String PREF_LOG_LEVEL = "logLevel";
    public static final String PREF_LOG_INFO = "logInfo";
    public static final String PREF_LOG_DEBUG = "logDebug";

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

    /** Set the log level preference. */
    public static void setLogLevel(String logLevel) {
        IEclipsePreferences node = instanceNode();
        if (node != null) {
            node.put(PREF_LOG_LEVEL, logLevel);
            try {
                node.flush();
            } catch (BackingStoreException ignore) {}
        } else {
            FALLBACK.put(PREF_LOG_LEVEL, logLevel);
        }
    }

    /** Retrieve the log level preference or default. */
    public static String getLogLevel() {
        IEclipsePreferences node = instanceNode();
        if (node != null) {
            return node.get(PREF_LOG_LEVEL, "info");
        }
        return FALLBACK.getOrDefault(PREF_LOG_LEVEL, "info");
    }

    /** Set the info logging enabled preference. */
    public static void setInfoLoggingEnabled(boolean enabled) {
        IEclipsePreferences node = instanceNode();
        if (node != null) {
            node.putBoolean(PREF_LOG_INFO, enabled);
            try {
                node.flush();
            } catch (BackingStoreException ignore) {}
        } else {
            FALLBACK.put(PREF_LOG_INFO, Boolean.toString(enabled));
        }
    }

    /** Retrieve the info logging enabled preference. */
    public static boolean isInfoLoggingEnabled() {
        IEclipsePreferences node = instanceNode();
        if (node != null) {
            return node.getBoolean(PREF_LOG_INFO, true);
        }
        String val = FALLBACK.get(PREF_LOG_INFO);
        return val != null ? Boolean.parseBoolean(val) : true;
    }

    /** Set the debug logging enabled preference. */
    public static void setDebugLoggingEnabled(boolean enabled) {
        IEclipsePreferences node = instanceNode();
        if (node != null) {
            node.putBoolean(PREF_LOG_DEBUG, enabled);
            try {
                node.flush();
            } catch (BackingStoreException ignore) {}
        } else {
            FALLBACK.put(PREF_LOG_DEBUG, Boolean.toString(enabled));
        }
    }

    /** Retrieve the debug logging enabled preference. */
    public static boolean isDebugLoggingEnabled() {
        IEclipsePreferences node = instanceNode();
        if (node != null) {
            return node.getBoolean(PREF_LOG_DEBUG, false);
        }
        String val = FALLBACK.get(PREF_LOG_DEBUG);
        return val != null ? Boolean.parseBoolean(val) : false;
    }
}
