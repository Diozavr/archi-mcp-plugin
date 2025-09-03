package com.archimatetool.mcp.core.types;

import java.util.Map;

/** Item describing relation fields to update. */
public class UpdateRelationItem {
    public final String id;
    public final String name;
    public final String type;
    public final Map<String,String> properties;
    public final String documentation;

    public UpdateRelationItem(String id, String name, String type,
                              Map<String,String> properties,
                              String documentation) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.properties = properties;
        this.documentation = documentation;
    }
}
