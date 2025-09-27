/*
 * Copyright 2025 Cinimex
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.cinimex.archimatetool.mcp.util;

import java.util.Map;

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.fasterxml.jackson.databind.ObjectMapper;

import ru.cinimex.archimatetool.mcp.Activator;
import ru.cinimex.archimatetool.mcp.preferences.MCPPreferences;

/**
 * Utility class for structured logging of MCP operations.
 * Provides info and debug level logging for operation calls and data.
 */
public class McpLogger {
    
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String MCP_PREFIX = "[MCP] ";
    private static final String API_PREFIX = "[API] ";
    
    // Configurable logging levels - will be initialized from preferences
    private static Boolean infoEnabled = null;
    private static Boolean debugEnabled = null;
    
    /**
     * Enable or disable info level logging.
     */
    public static void setInfoEnabled(boolean enabled) {
        infoEnabled = enabled;
        // Also persist to preferences
        MCPPreferences.setInfoLoggingEnabled(enabled);
    }
    
    /**
     * Enable or disable debug level logging.
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
        // Also persist to preferences
        MCPPreferences.setDebugLoggingEnabled(enabled);
    }
    
    /**
     * Check if info level logging is enabled.
     * Loads from preferences on first access.
     */
    public static boolean isInfoEnabled() {
        if (infoEnabled == null) {
            infoEnabled = MCPPreferences.isInfoLoggingEnabled();
        }
        return infoEnabled;
    }
    
    /**
     * Check if debug level logging is enabled.
     * Loads from preferences on first access.
     */
    public static boolean isDebugEnabled() {
        if (debugEnabled == null) {
            debugEnabled = MCPPreferences.isDebugLoggingEnabled();
        }
        return debugEnabled;
    }
    
    /**
     * Log MCP operation call at info level.
     * Only logs the fact that the operation was called.
     */
    public static void logOperationCall(String operationName) {
        logMcpOperationCall(operationName);
    }
    
    /**
     * Log MCP operation call at info level with additional context.
     */
    public static void logOperationCall(String operationName, String context) {
        logMcpOperationCall(operationName, context);
    }
    
    /**
     * Log MCP operation call at info level.
     */
    public static void logMcpOperationCall(String operationName) {
        if (!isInfoEnabled()) return;
        
        String message = MCP_PREFIX + "Operation called: " + operationName;
        log(IStatus.INFO, message, null);
    }
    
    /**
     * Log MCP operation call at info level with additional context.
     */
    public static void logMcpOperationCall(String operationName, String context) {
        if (!isInfoEnabled()) return;
        
        String message = MCP_PREFIX + "Operation called: " + operationName;
        if (context != null && !context.isEmpty()) {
            message += " (" + context + ")";
        }
        log(IStatus.INFO, message, null);
    }
    
    /**
     * Log API operation call at info level.
     */
    public static void logApiOperationCall(String operationName) {
        if (!isInfoEnabled()) return;
        
        String message = API_PREFIX + "Operation called: " + operationName;
        log(IStatus.INFO, message, null);
    }
    
    /**
     * Log API operation call at info level with additional context.
     */
    public static void logApiOperationCall(String operationName, String context) {
        if (!isInfoEnabled()) return;
        
        String message = API_PREFIX + "Operation called: " + operationName;
        if (context != null && !context.isEmpty()) {
            message += " (" + context + ")";
        }
        log(IStatus.INFO, message, null);
    }
    
    /**
     * Log MCP operation input data at debug level.
     */
    public static void logOperationInput(String operationName, Map<String, Object> inputData) {
        logMcpOperationInput(operationName, inputData);
    }
    
    /**
     * Log MCP operation output data at debug level.
     */
    public static void logOperationOutput(String operationName, Object outputData) {
        logMcpOperationOutput(operationName, outputData);
    }
    
    /**
     * Log MCP operation input and output data at debug level.
     */
    public static void logOperationData(String operationName, Map<String, Object> inputData, Object outputData) {
        logMcpOperationData(operationName, inputData, outputData);
    }
    
    /**
     * Log MCP operation input data at debug level.
     */
    public static void logMcpOperationInput(String operationName, Map<String, Object> inputData) {
        if (!isDebugEnabled()) return;
        
        String message = MCP_PREFIX + "Operation input: " + operationName;
        String dataStr = formatData(inputData);
        if (dataStr != null) {
            message += "\nInput data: " + dataStr;
        }
        log(IStatus.INFO, message, null);
    }
    
    /**
     * Log MCP operation output data at debug level.
     */
    public static void logMcpOperationOutput(String operationName, Object outputData) {
        if (!isDebugEnabled()) return;
        
        String message = MCP_PREFIX + "Operation output: " + operationName;
        String dataStr = formatData(outputData);
        if (dataStr != null) {
            message += "\nOutput data: " + dataStr;
        }
        log(IStatus.INFO, message, null);
    }
    
    /**
     * Log MCP operation input and output data at debug level.
     */
    public static void logMcpOperationData(String operationName, Map<String, Object> inputData, Object outputData) {
        if (!isDebugEnabled()) return;
        
        StringBuilder message = new StringBuilder(MCP_PREFIX + "Operation data: " + operationName);
        
        String inputStr = formatData(inputData);
        if (inputStr != null) {
            message.append("\nInput: ").append(inputStr);
        }
        
        String outputStr = formatData(outputData);
        if (outputStr != null) {
            message.append("\nOutput: ").append(outputStr);
        }
        
        log(IStatus.INFO, message.toString(), null);
    }
    
    /**
     * Log API operation input data at debug level.
     */
    public static void logApiOperationInput(String operationName, Map<String, Object> inputData) {
        if (!isDebugEnabled()) return;
        
        String message = API_PREFIX + "Operation input: " + operationName;
        String dataStr = formatData(inputData);
        if (dataStr != null) {
            message += "\nInput data: " + dataStr;
        }
        log(IStatus.INFO, message, null);
    }
    
    /**
     * Log API operation output data at debug level.
     */
    public static void logApiOperationOutput(String operationName, Object outputData) {
        if (!isDebugEnabled()) return;
        
        String message = API_PREFIX + "Operation output: " + operationName;
        String dataStr = formatData(outputData);
        if (dataStr != null) {
            message += "\nOutput data: " + dataStr;
        }
        log(IStatus.INFO, message, null);
    }
    
    /**
     * Log API operation input and output data at debug level.
     */
    public static void logApiOperationData(String operationName, Map<String, Object> inputData, Object outputData) {
        if (!isDebugEnabled()) return;
        
        StringBuilder message = new StringBuilder(API_PREFIX + "Operation data: " + operationName);
        
        String inputStr = formatData(inputData);
        if (inputStr != null) {
            message.append("\nInput: ").append(inputStr);
        }
        
        String outputStr = formatData(outputData);
        if (outputStr != null) {
            message.append("\nOutput: ").append(outputStr);
        }
        
        log(IStatus.INFO, message.toString(), null);
    }
    
    /**
     * Log MCP operation error.
     */
    public static void logOperationError(String operationName, Throwable error) {
        logMcpOperationError(operationName, error);
    }
    
    /**
     * Log MCP operation warning.
     */
    public static void logOperationWarning(String operationName, String warning) {
        logMcpOperationWarning(operationName, warning);
    }
    
    /**
     * Log MCP operation error.
     */
    public static void logMcpOperationError(String operationName, Throwable error) {
        String message = MCP_PREFIX + "Operation error: " + operationName;
        if (error.getMessage() != null) {
            message += " - " + error.getMessage();
        }
        log(IStatus.ERROR, message, error);
    }
    
    /**
     * Log MCP operation warning.
     */
    public static void logMcpOperationWarning(String operationName, String warning) {
        String message = MCP_PREFIX + "Operation warning: " + operationName + " - " + warning;
        log(IStatus.WARNING, message, null);
    }
    
    /**
     * Log API operation error.
     */
    public static void logApiOperationError(String operationName, Throwable error) {
        String message = API_PREFIX + "Operation error: " + operationName;
        if (error.getMessage() != null) {
            message += " - " + error.getMessage();
        }
        log(IStatus.ERROR, message, error);
    }
    
    /**
     * Log API operation warning.
     */
    public static void logApiOperationWarning(String operationName, String warning) {
        String message = API_PREFIX + "Operation warning: " + operationName + " - " + warning;
        log(IStatus.WARNING, message, null);
    }
    
    /**
     * Format data object as JSON string for logging.
     * Handles potential serialization errors gracefully.
     */
    private static String formatData(Object data) {
        if (data == null) {
            return null;
        }
        
        try {
            // Limit the size of logged data to prevent excessive log entries
            String json = mapper.writeValueAsString(data);
            if (json.length() > 2000) {
                return json.substring(0, 2000) + "... (truncated)";
            }
            return json;
        } catch (Exception e) {
            // If JSON serialization fails, fall back to toString()
            String str = data.toString();
            if (str.length() > 1000) {
                return str.substring(0, 1000) + "... (truncated)";
            }
            return str;
        }
    }
    
    /**
     * Internal method to write to Eclipse log.
     */
    private static void log(int severity, String message, Throwable exception) {
    	Activator activator = null;
    	try {
    		activator = Activator.getDefault();
            if (activator != null) {
                ILog log = activator.getLog();
                if (log != null) {
                    Status status = new Status(severity, Activator.PLUGIN_ID, message, exception);
                    log.log(status);
                }
            }
        } catch (Exception e) {
            // If Eclipse logging fails, fall back to console logging
            System.err.println("Eclipse logging failed: " + e.getMessage());
        }
        
        // Console output only for development/debugging when Eclipse logging is not available
        if (activator == null) {
            String levelStr = severity == IStatus.ERROR ? "ERROR" : 
                             severity == IStatus.WARNING ? "WARN" : "INFO";
            System.out.println("[" + levelStr + "] " + message);
            if (exception != null && isDebugEnabled()) {
                exception.printStackTrace();
            }
        }
    }
}
