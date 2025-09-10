package ru.cinimex.archimatetool.mcp.core.types;

import java.util.List;

/** Command to delete multiple elements. */
public class DeleteElementsCmd {
    public final List<DeleteElementItem> items;

    public DeleteElementsCmd(List<DeleteElementItem> items) {
        this.items = items;
    }
}
