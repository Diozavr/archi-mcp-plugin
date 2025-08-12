package com.archimatetool.mcp.core.errors;

/** Conflict (HTTP 409) */
public class ConflictException extends CoreException {
    public ConflictException(String message) {
        super(message);
    }
}
