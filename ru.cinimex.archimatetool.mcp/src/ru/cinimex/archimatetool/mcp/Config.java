package ru.cinimex.archimatetool.mcp;

import ru.cinimex.archimatetool.mcp.preferences.MCPPreferences;

/**
 * Centralized configuration with precedence: System Properties → Environment → Preferences → Defaults.
 */
public final class Config {

    private Config() {}

    public static final String DEFAULT_HOST = "127.0.0.1";
    public static final int DEFAULT_PORT = 8765;

    public static boolean isDebugEnabled() {
        String sp = System.getProperty("archi.mcp.debug");
        if (isTrue(sp)) return true;
        String ev = getenv.apply("ARCHI_MCP_DEBUG");
        return isTrue(ev);
    }

    public static int resolvePort() {
        try {
            String sp = System.getProperty("archi.mcp.port");
            if (sp != null && !sp.isEmpty()) return Integer.parseInt(sp);
        } catch (Exception ignore) {}
        try {
            String ev = getenv.apply("ARCHI_MCP_PORT");
            if (ev != null && !ev.isEmpty()) return Integer.parseInt(ev);
        } catch (Exception ignore) {}
        return MCPPreferences.getPort();
    }

    private static java.util.function.Function<String, String> getenv = System::getenv;

    public static void setGetenv(java.util.function.Function<String, String> f) {
        getenv = f;
    }

    public static void resetGetenv() {
        getenv = System::getenv;
    }

    private static boolean isTrue(String s) {
        return s != null && ("true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s));
    }
}


