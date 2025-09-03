package com.archimatetool.mcp.server.tools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Declarative description of an MCP method (tool).
 */
public class Tool {
    private final String name;
    private final String description;
    private final List<ToolParam> params;
    private final ToolInvoker invoker;

    public Tool(String name, String description, List<ToolParam> params, ToolInvoker invoker) {
        this.name = name;
        this.description = description;
        this.params = params != null ? params : new ArrayList<>();
        this.invoker = invoker;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<ToolParam> getParams() {
        return params;
    }

    public ToolInvoker getInvoker() {
        return invoker;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        if (description != null) {
            m.put("description", description);
        }
        Map<String, Object> schema = new HashMap<>();
        schema.put("$schema", "http://json-schema.org/draft-07/schema#");
        schema.put("type", "object");
        Map<String, Object> props = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();
        for (ToolParam tp : params) {
            Map<String, Object> p = new HashMap<>();
            p.put("type", tp.getType());
            if (tp.getDescription() != null) {
                p.put("description", tp.getDescription());
            }
            if (tp.getDefaultValue() != null) {
                p.put("default", tp.getDefaultValue());
            }
            // Provide minimal sub-schemas for complex types to satisfy strict validators
            if ("array".equals(tp.getType())) {
                // Default to array of strings if not otherwise specified
                Map<String, Object> items = new HashMap<>();
                items.put("type", "string");
                p.put("items", items);
            } else if ("object".equals(tp.getType())) {
                p.put("properties", new LinkedHashMap<String, Object>());
                p.put("additionalProperties", Boolean.TRUE);
            }
            props.put(tp.getName(), p);
            if (tp.isRequired()) {
                required.add(tp.getName());
            }
        }
        schema.put("properties", props);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }
        schema.put("additionalProperties", Boolean.FALSE);
        m.put("inputSchema", schema);
        // Minimal inline usage hint to help MCP agents that don't read external docs
        String exampleArgs = "{";
        for (int i = 0; i < params.size(); i++) {
            ToolParam tp = params.get(i);
            exampleArgs += "\\\"" + tp.getName() + "\\\"" + ":" + exampleValue(tp.getType());
            if (i < params.size() - 1) exampleArgs += ",";
        }
        exampleArgs += "}";
        String usage = "{\\\"jsonrpc\\\":\\\"2.0\\\",\\\"id\\\":1,\\\"method\\\":\\\"tools/call\\\",\\\"params\\\":{\\\"name\\\":\\\"" + name + "\\\",\\\"arguments\\\":" + exampleArgs + "}}";
        m.put("usage", usage);
        return m;
    }

    private String exampleValue(String type) {
        if ("string".equals(type)) return "\"value\"";
        if ("integer".equals(type) || "number".equals(type)) return "0";
        if ("boolean".equals(type)) return "false";
        if ("array".equals(type)) return "[]";
        if ("object".equals(type)) return "{}";
        return "null";
    }
}
