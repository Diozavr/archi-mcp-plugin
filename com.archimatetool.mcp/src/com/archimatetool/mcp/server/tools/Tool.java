package com.archimatetool.mcp.server.tools;

import java.util.Map;

/**
 * Represents an MCP tool that can be listed and invoked over JSON-RPC.
 */
public class Tool {
    private final String name;
    private final String description;
    private final Map<String, Param> params;
    private final ToolInvoker invoker;

    public Tool(String name, String description, Map<String, Param> params, ToolInvoker invoker) {
        this.name = name;
        this.description = description;
        this.params = params;
        this.invoker = invoker;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Map<String, Param> getParams() {
        return params;
    }

    public ToolInvoker getInvoker() {
        return invoker;
    }
}
