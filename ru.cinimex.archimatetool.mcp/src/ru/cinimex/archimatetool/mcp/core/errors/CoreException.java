package ru.cinimex.archimatetool.mcp.core.errors;

/**
 * Base class for all exceptions thrown from the core layer.
 */
public class CoreException extends RuntimeException {
    public CoreException(String message) {
        super(message);
    }
    public CoreException(String message, Throwable cause) {
        super(message, cause);
    }
}
