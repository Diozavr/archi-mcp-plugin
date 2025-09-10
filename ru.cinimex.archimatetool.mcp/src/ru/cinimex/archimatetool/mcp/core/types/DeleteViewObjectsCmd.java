package ru.cinimex.archimatetool.mcp.core.types;

import java.util.List;

/** Command to delete multiple view objects. */
public class DeleteViewObjectsCmd {
    public final String viewId;
    public final List<DeleteViewObjectItem> items;

    public DeleteViewObjectsCmd(String viewId, List<DeleteViewObjectItem> items) {
        this.viewId = viewId;
        this.items = items;
    }
}
