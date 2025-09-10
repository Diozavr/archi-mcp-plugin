package ru.cinimex.archimatetool.mcp.core.types;

import java.util.List;

/** Command to update multiple elements. */
public class UpdateElementsCmd {
    public final List<UpdateElementItem> items;

    public UpdateElementsCmd(List<UpdateElementItem> items) {
        this.items = items;
    }
}
