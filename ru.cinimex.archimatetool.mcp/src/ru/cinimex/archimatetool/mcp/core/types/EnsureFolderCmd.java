package ru.cinimex.archimatetool.mcp.core.types;

/** Command to ensure that a folder path exists. */
public class EnsureFolderCmd {
    public final String path;
    public EnsureFolderCmd(String path) {
        this.path = path;
    }
}
