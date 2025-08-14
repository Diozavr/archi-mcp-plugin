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
            props.put(tp.getName(), p);
            if (tp.isRequired()) {
                required.add(tp.getName());
            }
        }
        schema.put("properties", props);
        schema.put("required", required);
        schema.put("additionalProperties", Boolean.FALSE);
        m.put("inputSchema", schema);
        return m;
    }
}
