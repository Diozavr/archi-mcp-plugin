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

import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import ru.cinimex.archimatetool.mcp.server.HttpServerRunner;

public class Activator implements BundleActivator {

    public static final String PLUGIN_ID = "ru.cinimex.archimatetool.mcp";

    private static Activator instance;
    private HttpServerRunner serverRunner;
    private ILog log;

    public static Activator getDefault() {
        return instance;
    }

    public ILog getLog() {
        return log;
    }

    public synchronized void startServer() throws IOException {
        if (serverRunner == null) {
            serverRunner = new HttpServerRunner();
        }
        if (!serverRunner.isRunning()) {
            serverRunner.start();
            refreshToggleState();
        }
    }

    public synchronized void stopServer() {
        if (serverRunner != null && serverRunner.isRunning()) {
            serverRunner.stop();
            refreshToggleState();
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

    private void refreshToggleState() {
        Display display = Display.getDefault();
        if (display == null) {
            return;
        }
        display.asyncExec(() -> {
            ICommandService cs = PlatformUI.getWorkbench().getService(ICommandService.class);
            if (cs != null) {
                try {
                    Command cmd = cs.getCommand("ru.cinimex.archimatetool.mcp.commands.toggleServer");
                    if (cmd != null) {
                        State state = cmd.getState("org.eclipse.ui.commands.toggleState");
                        if (state != null) {
                            state.setValue(Boolean.valueOf(isServerRunning()));
                        }
                    }
                } catch (Exception ignore) {
                    // Best-effort state sync; ignore if command not available yet
                }
                cs.refreshElements("ru.cinimex.archimatetool.mcp.commands.toggleServer", null);
            }
        });
    }

    @Override
    public void start(BundleContext context) throws Exception {
        instance = this;
        log = Platform.getLog(context.getBundle());
        startServer();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (serverRunner != null && serverRunner.isRunning()) {
            serverRunner.stop();
            serverRunner = null;
        }
        instance = null;
    }
}

