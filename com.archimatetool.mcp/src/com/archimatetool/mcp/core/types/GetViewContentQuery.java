package com.archimatetool.mcp.core.types;

/** Query to fetch view content. */
public class GetViewContentQuery {
    public final String viewId;

    public GetViewContentQuery(String viewId) {
        this.viewId = viewId;
    }
}
