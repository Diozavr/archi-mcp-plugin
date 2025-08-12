package com.archimatetool.mcp.core.types;

/** Command to create a new relation. */
public class CreateRelationCmd {
    public final String type;
    public final String name;
    public final String sourceId;
    public final String targetId;
    public final String folderId;

    public CreateRelationCmd(String type, String name, String sourceId, String targetId, String folderId) {
        this.type = type;
        this.name = name;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.folderId = folderId;
    }
}
