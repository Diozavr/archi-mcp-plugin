package ru.cinimex.archimatetool.mcp.core.types;

import java.util.List;

/** Command to add multiple relations to a view. */
public class AddRelationsToViewCmd {
    public final String viewId;
    public final List<AddRelationToViewItem> items;

    public AddRelationsToViewCmd(String viewId, List<AddRelationToViewItem> items) {
        this.viewId = viewId;
        this.items = items;
    }
}
