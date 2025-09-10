package ru.cinimex.archimatetool.mcp.core.validation;

/** Simple validation helpers for the core layer. */
public final class Validators {
    private Validators() {
    }

    /** Generic boolean check. */
    public static void require(boolean condition, String message) {
        if (!condition) {
            throw new ru.cinimex.archimatetool.mcp.core.errors.BadRequestException(message);
        }
    }

    /** Ensure the value is not {@code null}. */
    public static void requireNonNull(Object value, String field) {
        if (value == null) {
            throw new ru.cinimex.archimatetool.mcp.core.errors.BadRequestException(field + " required");
        }
    }

    /** Ensure the string is not {@code null} or blank. */
    public static void requireNonEmpty(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ru.cinimex.archimatetool.mcp.core.errors.BadRequestException(field + " required");
        }
    }

    /** Ensure the integer is not negative. */
    public static void requireNonNegative(Integer value, String field) {
        if (value == null || value.intValue() < 0) {
            throw new ru.cinimex.archimatetool.mcp.core.errors.BadRequestException(field + " must be non-negative");
        }
    }
}
