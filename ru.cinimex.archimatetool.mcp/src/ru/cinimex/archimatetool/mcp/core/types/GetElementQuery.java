package ru.cinimex.archimatetool.mcp.core.types;

/** Query for retrieving an element. */
public class GetElementQuery {
    public final String id;
    public final boolean includeRelations;
    public final boolean includeElements;

    public GetElementQuery(String id, boolean includeRelations, boolean includeElements) {
        this.id = id;
        this.includeRelations = includeRelations;
        this.includeElements = includeElements;
    }
}
