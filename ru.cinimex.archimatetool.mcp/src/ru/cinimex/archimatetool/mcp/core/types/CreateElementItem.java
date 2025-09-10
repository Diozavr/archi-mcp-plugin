package ru.cinimex.archimatetool.mcp.core.types;

import java.util.Map;

/** Item describing a new element to create. */
public class CreateElementItem {
    public final String modelId;
    public final String type;
    public final String name;
    public final String folderId;
    public final Map<String,String> properties;
    public final String documentation;

    public CreateElementItem(String modelId, String type, String name,
                             String folderId, Map<String,String> properties,
                             String documentation) {
        this.modelId = modelId;
        this.type = type;
        this.name = name;
        this.folderId = folderId;
        this.properties = properties;
        this.documentation = documentation;
    }
}
