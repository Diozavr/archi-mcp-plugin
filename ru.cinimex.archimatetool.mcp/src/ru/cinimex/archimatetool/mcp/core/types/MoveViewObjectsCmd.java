package ru.cinimex.archimatetool.mcp.core.types;

import java.util.List;

/** Command to move multiple view objects. */
public class MoveViewObjectsCmd {
    public final String viewId;
    public final List<MoveViewObjectItem> items;

    public MoveViewObjectsCmd(String viewId, List<MoveViewObjectItem> items) {
        this.viewId = viewId;
        this.items = items;
    }
}
