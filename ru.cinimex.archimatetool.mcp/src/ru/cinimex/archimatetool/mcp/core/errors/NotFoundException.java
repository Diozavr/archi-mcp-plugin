package ru.cinimex.archimatetool.mcp.core.errors;

/** Resource not found (HTTP 404) */
public class NotFoundException extends CoreException {
    public NotFoundException(String message) {
        super(message);
    }
}
