package com.archimatetool.mcp;

import org.eclipse.swt.widgets.Display;

/**
 * Small helper to run UI-bound operations safely on the SWT UI thread.
 */
public final class UiExec {
    private UiExec() {}

    public static void sync(Runnable runnable) {
        if (runnable == null) return;
        Display.getDefault().syncExec(runnable);
    }
}


