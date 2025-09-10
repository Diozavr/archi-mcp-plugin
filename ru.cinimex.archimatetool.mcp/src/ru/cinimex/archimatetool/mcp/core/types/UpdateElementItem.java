package ru.cinimex.archimatetool.mcp.core.types;

import java.util.Map;

/** Item describing element fields to update. */
public class UpdateElementItem {
    public final String id;
    public final String name;
    public final String type;
    public final String folderId;
    public final Map<String,String> properties;
    public final String documentation;

    public UpdateElementItem(String id, String name, String type,
                             String folderId, Map<String,String> properties,
                             String documentation) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.folderId = folderId;
        this.properties = properties;
        this.documentation = documentation;
    }
}
