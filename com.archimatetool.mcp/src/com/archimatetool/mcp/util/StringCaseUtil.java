package com.archimatetool.mcp.util;

public final class StringCaseUtil {
    private StringCaseUtil() {}

    public static String toCamelCase(String kebab) {
        if (kebab == null) return null;
        String[] parts = kebab.split("-");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1).toLowerCase());
        }
        return sb.toString();
    }
}


