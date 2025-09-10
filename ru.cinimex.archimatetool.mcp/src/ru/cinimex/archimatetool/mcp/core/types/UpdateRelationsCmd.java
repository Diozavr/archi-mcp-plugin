package ru.cinimex.archimatetool.mcp.core.types;

import java.util.List;

/** Command to update multiple relations. */
public class UpdateRelationsCmd {
    public final List<UpdateRelationItem> items;

    public UpdateRelationsCmd(List<UpdateRelationItem> items) {
        this.items = items;
    }
}
