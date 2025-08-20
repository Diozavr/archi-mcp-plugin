package com.archimatetool.mcp.core.types;

import java.util.Map;

/** Item describing an element to add to a view. */
public class AddElementToViewItem {
    public final String elementId;
    public final String parentObjectId;
    public final Integer x;
    public final Integer y;
    public final Integer w;
    public final Integer h;
    public final Map<String,String> style;

    public AddElementToViewItem(String elementId, String parentObjectId,
                                Integer x, Integer y, Integer w, Integer h,
                                Map<String,String> style) {
        this.elementId = elementId;
        this.parentObjectId = parentObjectId;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.style = style;
    }
}
