package ru.cinimex.archimatetool.mcp;

import org.eclipse.ui.IStartup;

public class Startup implements IStartup {
    @Override
    public void earlyStartup() {
        // Touch activator so bundle is started early
        Activator.getDefault();
    }
}

