package ru.cinimex.archimatetool.mcp.core.types;

/** Command to delete a view. */
public class DeleteViewCmd {
    public final String id;

    public DeleteViewCmd(String id) {
        this.id = id;
    }
}
