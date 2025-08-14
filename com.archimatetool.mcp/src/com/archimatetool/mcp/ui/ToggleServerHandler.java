package com.archimatetool.mcp.ui;

import java.util.Map;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.archimatetool.mcp.Activator;
import com.archimatetool.mcp.Config;

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
                cs.refreshElements("com.archimatetool.mcp.commands.toggleServer", null);
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
        element.setText(running ? "Stop MCP Server" : "Start MCP Server");
        int port = running ? act.getBoundPort() : Config.resolvePort();
        element.setTooltip("Start/Stop MCP Server (127.0.0.1:" + port + ")");
        ImageDescriptor icon = AbstractUIPlugin.imageDescriptorFromPlugin(
                Activator.PLUGIN_ID,
                running ? "img/mcp_on.png" : "img/mcp_off.png");
        element.setIcon(icon);
    }
}

