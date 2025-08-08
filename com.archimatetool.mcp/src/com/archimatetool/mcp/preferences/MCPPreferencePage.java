package com.archimatetool.mcp.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class MCPPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

    @Override
    public void init(IWorkbench workbench) {
        // no-op for now
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite container = new Composite(parent, SWT.NONE);
        container.setLayout(new GridLayout(1, false));

        Label info = new Label(container, SWT.WRAP);
        info.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        info.setText("Archi MCP: настройки появятся позже. Сервер стартует автоматически при запуске плагина.");

        return container;
    }
}

