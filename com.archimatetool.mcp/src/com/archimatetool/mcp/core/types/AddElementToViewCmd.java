package com.archimatetool.mcp.core.types;

/** Command to add an element to a view or container. */
public class AddElementToViewCmd {
    public final String viewId;
    public final String elementId;
    public final String parentObjectId;
    public final Integer x;
    public final Integer y;
    public final Integer w;
    public final Integer h;

    public AddElementToViewCmd(String viewId, String elementId, String parentObjectId,
                               Integer x, Integer y, Integer w, Integer h) {
        this.viewId = viewId;
        this.elementId = elementId;
        this.parentObjectId = parentObjectId;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}
