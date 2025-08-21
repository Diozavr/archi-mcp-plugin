package com.archimatetool.mcp.core.types;

import java.util.List;

/** Command to create multiple elements. */
public class CreateElementsCmd {
    public final List<CreateElementItem> items;

    public CreateElementsCmd(List<CreateElementItem> items) {
        this.items = items;
    }
}
