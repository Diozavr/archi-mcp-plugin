package com.archimatetool.mcp.core.types;

/** Item describing view object move. */
public class MoveViewObjectItem {
    public final String objectId;
    public final String parentObjectId;
    public final Integer x;
    public final Integer y;
    public final Integer w;
    public final Integer h;
    public final Boolean keepExistingConnection;

    public MoveViewObjectItem(String objectId, String parentObjectId,
                              Integer x, Integer y, Integer w, Integer h,
                              Boolean keepExistingConnection) {
        this.objectId = objectId;
        this.parentObjectId = parentObjectId;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.keepExistingConnection = keepExistingConnection;
    }
}
