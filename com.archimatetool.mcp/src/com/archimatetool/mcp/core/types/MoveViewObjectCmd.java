package com.archimatetool.mcp.core.types;

/** Command to move a view object to another container. */
public class MoveViewObjectCmd {
    public final String viewId;
    public final String objectId;
    public final String parentObjectId;
    public final Integer x;
    public final Integer y;
    public final Integer w;
    public final Integer h;
    public final Boolean keepExistingConnection;

    public MoveViewObjectCmd(String viewId, String objectId, String parentObjectId,
                             Integer x, Integer y, Integer w, Integer h,
                             Boolean keepExistingConnection) {
        this.viewId = viewId;
        this.objectId = objectId;
        this.parentObjectId = parentObjectId;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.keepExistingConnection = keepExistingConnection;
    }
}
