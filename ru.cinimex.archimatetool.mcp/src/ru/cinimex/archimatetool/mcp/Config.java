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


