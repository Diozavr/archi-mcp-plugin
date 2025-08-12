package com.archimatetool.mcp.core.types;

/** Command to update an existing element. */
public class UpdateElementCmd {
    public final String id;
    public final String name;

    public UpdateElementCmd(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
