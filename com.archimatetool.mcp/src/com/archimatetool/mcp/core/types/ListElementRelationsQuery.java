package com.archimatetool.mcp.core.types;

/** Query to list relations of an element. */
public class ListElementRelationsQuery {
    public final String id;
    public final String direction; // both|in|out
    public final boolean includeElements;

    public ListElementRelationsQuery(String id, String direction, boolean includeElements) {
        this.id = id;
        this.direction = direction;
        this.includeElements = includeElements;
    }
}
