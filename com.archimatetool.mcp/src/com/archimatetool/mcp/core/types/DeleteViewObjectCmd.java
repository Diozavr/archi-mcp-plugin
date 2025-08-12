package com.archimatetool.mcp.core.types;

/** Command to delete an object from a view. */
public class DeleteViewObjectCmd {
    public final String viewId;
    public final String objectId;

    public DeleteViewObjectCmd(String viewId, String objectId) {
        this.viewId = viewId;
        this.objectId = objectId;
    }
}
