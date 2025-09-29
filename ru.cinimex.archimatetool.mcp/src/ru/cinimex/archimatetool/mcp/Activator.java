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
package ru.cinimex.archimatetool.mcp;

import java.io.IOException;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import ru.cinimex.archimatetool.mcp.preferences.MCPPreferences;
import ru.cinimex.archimatetool.mcp.server.HttpServerRunner;
import ru.cinimex.archimatetool.mcp.util.McpLogger;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {

    // The plug-in ID
    public static final String PLUGIN_ID = "ru.cinimex.archimatetool.mcp"; //$NON-NLS-1$

    // The shared instance
    private static Activator instance;

    private HttpServerRunner serverRunner;

    /**
     * The constructor
     */
    public Activator() {
    }

    public synchronized void startServer() throws IOException {
        McpLogger.logOperationCall("Plugin Start Server");
        
        if (serverRunner == null) {
            serverRunner = new HttpServerRunner();
        }
        if (!serverRunner.isRunning()) {
            serverRunner.start();
            refreshToggleState();
        }
    }

    public synchronized void stopServer() {
        McpLogger.logOperationCall("Plugin Stop Server");
        
        if (serverRunner != null && serverRunner.isRunning()) {
            serverRunner.stop();
            refreshToggleState();
            McpLogger.logOperationOutput("Plugin Stop Server", 
                java.util.Map.of("status", "server stopped via plugin"));
        } else {
            McpLogger.logOperationWarning("Plugin Stop Server", "Server not running or not initialized");
        }
    }

    public synchronized void restartServer() throws IOException {
        if (serverRunner != null && serverRunner.isRunning()) {
            serverRunner.restart();
        } else {
            startServer();
            return;
        }
        refreshToggleState();
    }

    public synchronized boolean isServerRunning() {
        return serverRunner != null && serverRunner.isRunning();
    }

    public synchronized int getBoundPort() {
        if (serverRunner != null) {
            return serverRunner.getPort();
        }
        return -1;
    }

    public synchronized String getBoundHost() {
        if (serverRunner != null) {
            return serverRunner.getHost();
        }
        return null;
    }

    private void refreshToggleState() {
        Display display = Display.getDefault();
        if (display == null) {
            return;
        }
        display.asyncExec(() -> {
            try {
                ICommandService cs = PlatformUI.getWorkbench().getService(ICommandService.class);
                if (cs != null) {
                    // Update the toggle state
                    Command cmd = cs.getCommand("ru.cinimex.archimatetool.mcp.commands.toggleServer");
                    if (cmd != null) {
                        State state = cmd.getState("org.eclipse.ui.commands.toggleState");
                        if (state != null) {
                            state.setValue(Boolean.valueOf(isServerRunning()));
                        }
                    }
                    // Refresh UI elements
                    cs.refreshElements("ru.cinimex.archimatetool.mcp.commands.toggleServer", null);
                }
            } catch (Exception e) {
                // Ignore errors during UI refresh
            }
        });
    }

    /**
     * Returns the shared instance
     *
     * @return the shared instance
     */
    public static Activator getDefault() {
        return instance;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        instance = this;
        
        // Initialize logging settings from preferences
        initializeLoggingSettings();
        
        startServer();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        McpLogger.logOperationCall("Plugin Shutdown");
        
        if (serverRunner != null && serverRunner.isRunning()) {
            serverRunner.stop();
            McpLogger.logOperationOutput("Plugin Shutdown", 
                java.util.Map.of("status", "server stopped during plugin shutdown"));
        }
        
        instance = null;
        super.stop(context);
    }

    private void initializeLoggingSettings() {
        // Load logging settings from preferences and apply them
        boolean infoEnabled = MCPPreferences.isInfoLoggingEnabled();
        boolean debugEnabled = MCPPreferences.isDebugLoggingEnabled();
        
        McpLogger.setInfoEnabled(infoEnabled);
        McpLogger.setDebugEnabled(debugEnabled);
        
        McpLogger.logOperationCall("Plugin Startup", 
            "Logging initialized: info=" + infoEnabled + ", debug=" + debugEnabled);
    }
}