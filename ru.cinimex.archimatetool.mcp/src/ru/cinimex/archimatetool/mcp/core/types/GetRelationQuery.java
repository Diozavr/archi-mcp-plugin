package ru.cinimex.archimatetool.mcp.core.types;

/** Query to get a relation by id. */
public class GetRelationQuery {
    public final String id;

    public GetRelationQuery(String id) {
        this.id = id;
    }
}
