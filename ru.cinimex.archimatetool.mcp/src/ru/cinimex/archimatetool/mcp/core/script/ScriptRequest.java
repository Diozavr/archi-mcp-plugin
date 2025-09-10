package ru.cinimex.archimatetool.mcp.core.script;

import java.util.Map;

/** Parameters for a script execution request. */
public record ScriptRequest(
    String engine,
    String code,
    Integer timeoutMs,
    Map<String, Object> bindings,
    String modelId,
    String log
) {}
