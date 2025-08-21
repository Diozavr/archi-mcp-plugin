package com.archimatetool.mcp.core.types;

import java.util.List;

/** Command to update bounds for multiple view objects. */
public class UpdateViewObjectsBoundsCmd {
    public final String viewId;
    public final List<UpdateViewObjectBoundsItem> items;

    public UpdateViewObjectsBoundsCmd(String viewId, List<UpdateViewObjectBoundsItem> items) {
        this.viewId = viewId;
        this.items = items;
    }
}
