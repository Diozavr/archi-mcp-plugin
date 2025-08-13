package com.archimatetool.mcp.server.tools;

import com.archimatetool.mcp.core.errors.CoreException;
import com.fasterxml.jackson.databind.JsonNode;

@FunctionalInterface
public interface ToolInvoker {
    Object invoke(JsonNode args) throws CoreException;
}
