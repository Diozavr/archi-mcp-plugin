package com.archimatetool.mcp.server.tools;

/**
 * Describes a single parameter for an MCP tool.
 */
public class Param {
    public final String name;
    public final String type;
    public final String description;
    public final boolean required;

    public Param(String name, String type, String description, boolean required) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.required = required;
    }
}
