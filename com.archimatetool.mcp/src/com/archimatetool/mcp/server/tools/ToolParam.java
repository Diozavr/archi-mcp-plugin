package com.archimatetool.mcp.server.tools;

import java.util.HashMap;
import java.util.Map;

/**
 * Parameter metadata for MCP tools.
 */
public class ToolParam {
    private final String name;
    private final String type;
    private final boolean required;
    private final String description;
    private final Object defaultValue;

    public ToolParam(String name, String type, boolean required, String description, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("type", type);
        m.put("required", required);
        if (description != null) {
            m.put("description", description);
        }
        if (defaultValue != null) {
            m.put("default", defaultValue);
        }
        return m;
    }
}
