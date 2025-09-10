package ru.cinimex.archimatetool.mcp.core.errors;

/** Bad request (HTTP 400) */
public class BadRequestException extends CoreException {
    public BadRequestException(String message) {
        super(message);
    }
}
