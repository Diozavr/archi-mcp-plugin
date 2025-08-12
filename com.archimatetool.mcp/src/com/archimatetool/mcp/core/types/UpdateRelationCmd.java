package com.archimatetool.mcp.core.types;

/** Command to update a relation. */
public class UpdateRelationCmd {
    public final String id;
    public final String name;

    public UpdateRelationCmd(String id, String name) {
        this.id = id;
        this.name = name;
    }
}
