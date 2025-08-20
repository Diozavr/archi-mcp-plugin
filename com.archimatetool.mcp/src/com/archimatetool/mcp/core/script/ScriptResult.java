package com.archimatetool.mcp.core.script;

/** Result of executing a script. */
public record ScriptResult(
    boolean ok,
    Object result,
    String stdout,
    String stderr,
    long durationMs
) {}
