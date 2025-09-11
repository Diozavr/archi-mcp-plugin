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
package ru.cinimex.archimatetool.mcp.ui;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.State;

import ru.cinimex.archimatetool.mcp.Activator;
import ru.cinimex.archimatetool.mcp.Config;

public class ToggleServerHandler extends AbstractHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        Activator act = Activator.getDefault();
        if (act == null) {
            return null;
        }
        try {
            if (act.isServerRunning()) {
                act.stopServer();
            } else {
                act.startServer();
            }
            ICommandService cs = PlatformUI.getWorkbench().getService(ICommandService.class);
            if (cs != null) {
                try {
                    Command cmd = cs.getCommand("ru.cinimex.archimatetool.mcp.commands.toggleServer");
                    if (cmd != null) {
                        State state = cmd.getState("org.eclipse.ui.commands.toggleState");
                        if (state != null) {
                            state.setValue(Boolean.valueOf(act.isServerRunning()));
                        }
                    }
                } catch (Exception ignore) {
                }
                cs.refreshElements("ru.cinimex.archimatetool.mcp.commands.toggleServer", null);
            }
        } catch (Exception ex) {
            ILog log = act.getLog();
            if (log != null) {
                log.log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex));
            }
            MessageDialog.openError(Display.getDefault().getActiveShell(), "MCP Server", ex.getMessage());
        }
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        Activator act = Activator.getDefault();
        boolean running = act != null && act.isServerRunning();
        
        element.setChecked(running);
        element.setText("MCP");
        int port = running ? act.getBoundPort() : Config.resolvePort();
        element.setTooltip("Start/Stop MCP Server (127.0.0.1:" + port + ")");
    }
}

