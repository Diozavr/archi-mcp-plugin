package ru.cinimex.archimatetool.mcp.core.types;

/** Command to create a new view. */
public class CreateViewCmd {
    public final String type;
    public final String name;

    public CreateViewCmd(String type, String name) {
        this.type = type;
        this.name = name;
    }
}
