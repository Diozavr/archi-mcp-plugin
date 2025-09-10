package ru.cinimex.archimatetool.mcp.core.types;

import java.util.List;

/** Command to delete multiple relations. */
public class DeleteRelationsCmd {
    public final List<DeleteRelationItem> items;

    public DeleteRelationsCmd(List<DeleteRelationItem> items) {
        this.items = items;
    }
}
