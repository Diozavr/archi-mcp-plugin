package ru.cinimex.archimatetool.mcp.core.types;

import java.util.Map;

/** Item describing a new relation to create. */
public class CreateRelationItem {
    public final String type;
    public final String name;
    public final String sourceId;
    public final String targetId;
    public final String folderId;
    public final Map<String,String> properties;
    public final String documentation;

    public CreateRelationItem(String type, String name, String sourceId,
                              String targetId, String folderId,
                              Map<String,String> properties,
                              String documentation) {
        this.type = type;
        this.name = name;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.folderId = folderId;
        this.properties = properties;
        this.documentation = documentation;
    }
}
