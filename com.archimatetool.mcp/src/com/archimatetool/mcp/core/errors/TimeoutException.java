package com.archimatetool.mcp.core.errors;

/** Exception thrown when a script exceeds its allowed execution time. */
public class TimeoutException extends CoreException {
    public TimeoutException(String message) {
        super(message);
    }
}
