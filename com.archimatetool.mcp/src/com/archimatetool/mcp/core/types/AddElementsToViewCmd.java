package com.archimatetool.mcp.core.types;

import java.util.List;

/** Command to add multiple elements to a view. */
public class AddElementsToViewCmd {
    public final String viewId;
    public final List<AddElementToViewItem> items;

    public AddElementsToViewCmd(String viewId, List<AddElementToViewItem> items) {
        this.viewId = viewId;
        this.items = items;
    }
}
