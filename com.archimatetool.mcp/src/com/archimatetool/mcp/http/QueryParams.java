package com.archimatetool.mcp.http;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.sun.net.httpserver.HttpExchange;

/**
 * Helper for parsing query parameters with UTF-8 decoding and repeated keys.
 */
public final class QueryParams {
    private final Map<String, List<String>> params = new LinkedHashMap<>();

    private QueryParams() {}

    public static QueryParams from(HttpExchange exchange) {
        String q = exchange != null ? exchange.getRequestURI().getQuery() : null;
        return parse(q);
    }

    public static QueryParams parse(String query) {
        QueryParams qp = new QueryParams();
        if (query == null || query.isEmpty()) return qp;
        for (String p : query.split("&")) {
            if (p.isEmpty()) continue;
            int i = p.indexOf('=');
            String k = i >= 0 ? p.substring(0, i) : p;
            String v = i >= 0 ? p.substring(i + 1) : "";
            try {
                k = URLDecoder.decode(k, StandardCharsets.UTF_8);
                v = URLDecoder.decode(v, StandardCharsets.UTF_8);
            } catch (Exception ignore) {}
            qp.params.computeIfAbsent(k, kk -> new ArrayList<>()).add(v);
        }
        return qp;
    }

    public String first(String key) {
        List<String> l = params.get(key);
        return (l == null || l.isEmpty()) ? null : l.get(0);
    }

    public List<String> all(String key) {
        List<String> l = params.get(key);
        return l == null ? List.of() : List.copyOf(l);
    }

    public Integer getInt(String key, Integer def) {
        try {
            String v = first(key);
            return v != null ? Integer.valueOf(v) : def;
        } catch (Exception e) {
            return def;
        }
    }

    public float getFloat(String key, float def) {
        try {
            String v = first(key);
            return v != null ? Float.parseFloat(v) : def;
        } catch (Exception e) {
            return def;
        }
    }

    public boolean getBool(String key, boolean def) {
        String v = first(key);
        if (v == null) return def;
        return "true".equalsIgnoreCase(v) || "1".equals(v) || "yes".equalsIgnoreCase(v);
    }
}
