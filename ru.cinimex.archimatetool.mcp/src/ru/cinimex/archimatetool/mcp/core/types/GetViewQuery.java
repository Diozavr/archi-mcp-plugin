package ru.cinimex.archimatetool.mcp.core.types;

/** Query to fetch a view by id. */
public class GetViewQuery {
    public final String id;

    public GetViewQuery(String id) {
        this.id = id;
    }
}
