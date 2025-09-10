package ru.cinimex.archimatetool.mcp.core.types;

/** Parameters for rendering a view to an image. */
public class GetViewImageQuery {
    public final String viewId;
    public final String format; // png or svg
    public final Float scale;
    public final Integer dpi;
    public final String bg;
    public final Integer margin;

    public GetViewImageQuery(String viewId, String format, Float scale, Integer dpi, String bg, Integer margin) {
        this.viewId = viewId;
        this.format = format;
        this.scale = scale;
        this.dpi = dpi;
        this.bg = bg;
        this.margin = margin;
    }
}
