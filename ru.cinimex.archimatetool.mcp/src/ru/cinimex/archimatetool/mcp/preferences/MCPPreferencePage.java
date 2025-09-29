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

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import ru.cinimex.archimatetool.mcp.Activator;
import ru.cinimex.archimatetool.mcp.Config;
import ru.cinimex.archimatetool.mcp.util.McpLogger;

public class MCPPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private Label effectivePortLabel;
    private Label hostWarningLabel;
    private Label serverStatusLabel;
    private Button serverToggleButton;

    public MCPPreferencePage() {
        super(GRID);
        setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, MCPPreferences.NODE));
    }

    @Override
    public void init(IWorkbench workbench) {
        // no-op
    }

    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();
        parent.setLayout(new GridLayout(1, false));
        
        // Single main group with consistent layout
        Group mainGroup = new Group(parent, SWT.NONE);
        mainGroup.setText("Archi MCP Configuration");
        mainGroup.setLayout(new GridLayout(2, false));
        mainGroup.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        
        String precedenceTip = "Effective precedence: System Property → Env → Preferences → Default";

        createServerControls(mainGroup, precedenceTip);
        createLoggingControls(mainGroup);
    }

    private void createServerControls(Composite parent, String precedenceTip) {
        // Server Status
        Label statusTitleLabel = new Label(parent, SWT.NONE);
        statusTitleLabel.setText("Server Status:");
        statusTitleLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        
        Composite statusComposite = new Composite(parent, SWT.NONE);
        GridLayout statusLayout = new GridLayout(2, false);
        statusLayout.marginWidth = 0;
        statusLayout.marginHeight = 0;
        statusComposite.setLayout(statusLayout);
        statusComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        
        serverStatusLabel = new Label(statusComposite, SWT.NONE);
        serverStatusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        updateServerStatus();
        
        serverToggleButton = new Button(statusComposite, SWT.PUSH);
        GridData buttonData = new GridData(SWT.END, SWT.CENTER, false, false);
        buttonData.widthHint = 80;
        serverToggleButton.setLayoutData(buttonData);
        updateServerButton();
        
        serverToggleButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                toggleServer();
            }
        });

        // Separator
        Label separator1 = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator1.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));

        // Host field
        StringFieldEditor hostEditor = new StringFieldEditor(MCPPreferences.PREF_HOST, "Host:", parent);
        addField(hostEditor);
        Text hostText = hostEditor.getTextControl(parent);
        hostText.setToolTipText(precedenceTip);
        
        // Host warning (spans both columns)
        hostWarningLabel = new Label(parent, SWT.WRAP);
        hostWarningLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        ModifyListener hostListener = e -> {
            String val = hostText.getText();
            boolean safe = "127.0.0.1".equals(val) || "localhost".equalsIgnoreCase(val);
            if (safe) {
                hostWarningLabel.setText("");
            } else {
                hostWarningLabel.setText("⚠ Warning: non-localhost values may expose the server");
                hostWarningLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
            }
            maybeRestart();
        };
        hostText.addModifyListener(hostListener);
        hostListener.modifyText(null);

        // Port field
        IntegerFieldEditor portEditor = new IntegerFieldEditor(MCPPreferences.PREF_PORT, "Port:", parent);
        portEditor.setValidRange(1024, 65535);
        portEditor.getTextControl(parent).setToolTipText(precedenceTip);
        addField(portEditor);
        portEditor.getTextControl(parent).addModifyListener(e -> maybeRestart());

        // Effective port info (spans both columns)
        effectivePortLabel = new Label(parent, SWT.NONE);
        effectivePortLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        effectivePortLabel.setText("Effective port: " + Config.resolvePort());
        effectivePortLabel.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));

        // Separator
        Label separator2 = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
        separator2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
    }

    private void createLoggingControls(Composite parent) {
        // Info logging checkbox
        BooleanFieldEditor infoLoggingEditor = new BooleanFieldEditor(
            MCPPreferences.PREF_LOG_INFO, 
            "Enable Info Logging:", 
            parent
        );
        addField(infoLoggingEditor);
        
        // Info logging description (spans both columns)
        Label infoDesc = new Label(parent, SWT.WRAP);
        infoDesc.setText("Shows operation names in logs");
        infoDesc.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        infoDesc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));

        // Debug logging checkbox
        BooleanFieldEditor debugLoggingEditor = new BooleanFieldEditor(
            MCPPreferences.PREF_LOG_DEBUG, 
            "Enable Debug Logging:", 
            parent
        );
        addField(debugLoggingEditor);
        
        // Debug logging description (spans both columns)
        Label debugDesc = new Label(parent, SWT.WRAP);
        debugDesc.setText("Shows full request/response JSON content in logs");
        debugDesc.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        debugDesc.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));

        // General note (spans both columns)
        Label generalNote = new Label(parent, SWT.WRAP);
        generalNote.setText("Note: Changes take effect immediately");
        generalNote.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        generalNote.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_DARK_BLUE));
    }

    private void toggleServer() {
        try {
            Activator activator = Activator.getDefault();
            if (activator != null) {
                if (activator.isServerRunning()) {
                    activator.stopServer();
                    McpLogger.logOperationCall("Preferences", "Server stopped by user");
                } else {
                    activator.startServer();
                    McpLogger.logOperationCall("Preferences", "Server started by user");
                }
                updateServerStatus();
                updateServerButton();
                updateEffectivePortLabel();
            }
        } catch (Exception ex) {
            McpLogger.logOperationError("Preferences", ex);
            // Show error in status label
            serverStatusLabel.setText("Error: " + ex.getMessage());
            serverStatusLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_RED));
        }
    }

    private void updateServerStatus() {
        if (serverStatusLabel == null || serverStatusLabel.isDisposed()) {
            return;
        }
        
        Activator activator = Activator.getDefault();
        if (activator != null && activator.isServerRunning()) {
            int port = activator.getBoundPort();
            serverStatusLabel.setText("Running on port " + port);
            serverStatusLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN));
        } else {
            serverStatusLabel.setText("Stopped");
            serverStatusLabel.setForeground(getShell().getDisplay().getSystemColor(SWT.COLOR_DARK_RED));
        }
    }

    private void updateServerButton() {
        if (serverToggleButton == null || serverToggleButton.isDisposed()) {
            return;
        }
        
        Activator activator = Activator.getDefault();
        if (activator != null && activator.isServerRunning()) {
            serverToggleButton.setText("Stop");
        } else {
            serverToggleButton.setText("Start");
        }
    }

    private void updateEffectivePortLabel() {
        if (effectivePortLabel != null && !effectivePortLabel.isDisposed()) {
            Activator activator = Activator.getDefault();
            if (activator != null) {
                int port = activator.getBoundPort();
                if (port != -1) {
                    effectivePortLabel.setText("Effective port: " + port);
                } else {
                    effectivePortLabel.setText("Effective port: " + Config.resolvePort() + " (not running)");
                }
            }
        }
    }

    private void maybeRestart() {
        Activator act = Activator.getDefault();
        if (act != null) {
            int oldPort = act.getBoundPort();
            String oldHost = act.getBoundHost();
            int newPort = getPreferenceStore().getInt(MCPPreferences.PREF_PORT);
            String newHost = getPreferenceStore().getString(MCPPreferences.PREF_HOST);
            boolean needRestart = oldPort != -1 && (oldPort != newPort || (oldHost == null ? newHost != null : !oldHost.equals(newHost)));
            if (needRestart) {
                try {
                    act.restartServer();
                    McpLogger.logOperationCall("Preferences", "Server restarted due to host/port change");
                } catch (Exception ex) {
                    act.getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex));
                }
                updateServerStatus();
                updateServerButton();
                updateEffectivePortLabel();
            }
        }
    }

    private void applyLoggingSettings() {
        boolean infoEnabled = getPreferenceStore().getBoolean(MCPPreferences.PREF_LOG_INFO);
        boolean debugEnabled = getPreferenceStore().getBoolean(MCPPreferences.PREF_LOG_DEBUG);
        
        McpLogger.setInfoEnabled(infoEnabled);
        McpLogger.setDebugEnabled(debugEnabled);
        
        McpLogger.logOperationCall("Preferences", 
            "Logging settings applied: info=" + infoEnabled + ", debug=" + debugEnabled);
    }

    @Override
    public boolean performOk() {
        boolean result = super.performOk();
        maybeRestart();
        
        // Apply logging settings immediately
        applyLoggingSettings();
        
        return result;
    }

    @Override
    protected void performApply() {
        super.performApply();
        maybeRestart();
        
        // Apply logging settings immediately
        applyLoggingSettings();
    }

    @Override
    protected void performDefaults() {
        super.performDefaults();
        
        // Reset to defaults and apply immediately
        McpLogger.setInfoEnabled(true);  // Default is true
        McpLogger.setDebugEnabled(false); // Default is false
        
        McpLogger.logOperationCall("Preferences", "Logging settings reset to defaults");
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        if (visible) {
            // Update server status when page becomes visible
            updateServerStatus();
            updateServerButton();
            updateEffectivePortLabel();
        }
    }
}