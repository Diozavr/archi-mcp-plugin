package ru.cinimex.archimatetool.mcp.core.types;

import java.util.List;

/** Command to create multiple relations. */
public class CreateRelationsCmd {
    public final List<CreateRelationItem> items;

    public CreateRelationsCmd(List<CreateRelationItem> items) {
        this.items = items;
    }
}
