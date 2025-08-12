package com.archimatetool.mcp.core.types;

/** Command to update bounds of a view object. */
public class UpdateViewObjectBoundsCmd {
    public final String viewId;
    public final String objectId;
    public final Integer x;
    public final Integer y;
    public final Integer w;
    public final Integer h;

    public UpdateViewObjectBoundsCmd(String viewId, String objectId,
                                     Integer x, Integer y, Integer w, Integer h) {
        this.viewId = viewId;
        this.objectId = objectId;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}
