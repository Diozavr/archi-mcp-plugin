package com.archimatetool.mcp.core.types;

/** Item describing bounds updates for a view object. */
public class UpdateViewObjectBoundsItem {
    public final String objectId;
    public final Integer x;
    public final Integer y;
    public final Integer w;
    public final Integer h;

    public UpdateViewObjectBoundsItem(String objectId, Integer x, Integer y,
                                      Integer w, Integer h) {
        this.objectId = objectId;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}
