package com.archimatetool.mcp.core.errors;

/** Unprocessable entity (HTTP 422) */
public class UnprocessableException extends CoreException {
    public UnprocessableException(String message) {
        super(message);
    }

    public UnprocessableException(String message, Throwable cause) {
        super(message, cause);
    }
}
