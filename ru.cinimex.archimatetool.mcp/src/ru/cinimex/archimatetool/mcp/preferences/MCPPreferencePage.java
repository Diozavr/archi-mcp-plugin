package ru.cinimex.archimatetool.mcp.preferences;

import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import ru.cinimex.archimatetool.mcp.Activator;
import ru.cinimex.archimatetool.mcp.Config;

public class MCPPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    private Label effectivePortLabel;
    private Label hostWarningLabel;

    public MCPPreferencePage() {
        super(GRID);
        setPreferenceStore(new ScopedPreferenceStore(InstanceScope.INSTANCE, MCPPreferences.NODE));
        setDescription("Archi MCP preferences");
    }

    @Override
    public void init(IWorkbench workbench) {
        // no-op
    }

    @Override
    protected void createFieldEditors() {
        Composite parent = getFieldEditorParent();
        String precedenceTip = "Effective precedence: System Property → Env → Preferences → Default";

        StringFieldEditor hostEditor = new StringFieldEditor(MCPPreferences.PREF_HOST, "Host", parent);
        addField(hostEditor);
        Text hostText = hostEditor.getTextControl(parent);
        hostText.setToolTipText(precedenceTip);
        hostWarningLabel = new Label(parent, SWT.WRAP);
        hostWarningLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        ModifyListener hostListener = e -> {
            String val = hostText.getText();
            boolean safe = "127.0.0.1".equals(val) || "localhost".equalsIgnoreCase(val);
            hostWarningLabel.setText(safe ? "" : "Warning: non-localhost values may expose the server.");
        };
        hostText.addModifyListener(hostListener);
        hostListener.modifyText(null);

        IntegerFieldEditor portEditor = new IntegerFieldEditor(MCPPreferences.PREF_PORT, "Port (localhost only)", parent);
        portEditor.setValidRange(1024, 65535);
        portEditor.getTextControl(parent).setToolTipText(precedenceTip);
        addField(portEditor);

        effectivePortLabel = new Label(parent, SWT.NONE);
        effectivePortLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false, 2, 1));
        effectivePortLabel.setText("Effective port: " + Config.resolvePort());
    }

    private void maybeRestart() {
        Activator act = Activator.getDefault();
        if (act != null) {
            int oldPort = act.getBoundPort();
            int newPort = getPreferenceStore().getInt(MCPPreferences.PREF_PORT);
            if (oldPort != -1 && oldPort != newPort) {
                try {
                    act.restartServer();
                } catch (Exception ex) {
                    act.getLog().log(new Status(IStatus.ERROR, Activator.PLUGIN_ID, ex.getMessage(), ex));
                }
                effectivePortLabel.setText("Effective port: " + act.getBoundPort());
            }
        }
    }

    @Override
    public boolean performOk() {
        boolean result = super.performOk();
        maybeRestart();
        return result;
    }

    @Override
    protected void performApply() {
        super.performApply();
        maybeRestart();
    }
}
