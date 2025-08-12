package com.archimatetool.mcp.core.types;

/** Command to delete an element by id. */
public class DeleteElementCmd {
    public final String id;

    public DeleteElementCmd(String id) {
        this.id = id;
    }
}
