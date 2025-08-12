package com.archimatetool.mcp.core.types;

/** Command to create a new element. */
public class CreateElementCmd {
    public final String type;
    public final String name;
    public final String folderId;

    public CreateElementCmd(String type, String name, String folderId) {
        this.type = type;
        this.name = name;
        this.folderId = folderId;
    }
}
