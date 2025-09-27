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
package ru.cinimex.archimatetool.mcp.preferences;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;

import ru.cinimex.archimatetool.mcp.Config;

/**
 * Sets default values for MCP preferences.
 */
public class MCPPreferenceInitializer extends AbstractPreferenceInitializer {

    @Override
    public void initializeDefaultPreferences() {
        // Set default host and port
        MCPPreferences.setDefaultHostAndPort(Config.DEFAULT_HOST, Config.DEFAULT_PORT);
        
        // Set default logging preferences using the same graceful approach as MCPPreferences
        setDefaultLoggingPreferences();
    }

    private void setDefaultLoggingPreferences() {
        // Use the same pattern as MCPPreferences.setDefaultHostAndPort()
        // This will gracefully handle cases where Eclipse preferences service is not available
        try {
            // Try to set defaults using the MCPPreferences methods which have fallback handling
            // This is safer than directly accessing DefaultScope.INSTANCE
            
            // The MCPPreferences class already has proper fallback mechanisms
            // and default values are handled in the getter methods themselves:
            // - isInfoLoggingEnabled() defaults to true
            // - isDebugLoggingEnabled() defaults to false
            
            // So we don't need to explicitly set defaults here - they're handled
            // by the MCPPreferences class itself with proper fallbacks
            
        } catch (Exception e) {
            // Ignore - defaults will be handled by MCPPreferences getters
        }
    }
}