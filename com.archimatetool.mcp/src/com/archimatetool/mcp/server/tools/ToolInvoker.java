package com.archimatetool.mcp.server.tools;

import java.util.Map;

/**
 * Functional interface to invoke a tool with parameters.
 */
@FunctionalInterface
public interface ToolInvoker {
    Object invoke(Map<String, Object> params) throws Exception;
}
