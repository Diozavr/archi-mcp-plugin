package com.archimatetool.mcp.core.types;

/** Command to delete a relation. */
public class DeleteRelationCmd {
    public final String id;

    public DeleteRelationCmd(String id) {
        this.id = id;
    }
}
