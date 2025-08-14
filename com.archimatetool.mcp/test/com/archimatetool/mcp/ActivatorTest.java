package com.archimatetool.mcp;

import static org.junit.Assert.*;

import org.junit.Test;

public class ActivatorTest {
    @Test
    public void startStopUpdatesState() throws Exception {
        Activator act = new Activator();
        act.startServer();
        assertTrue(act.isServerRunning());
        act.stopServer();
        assertFalse(act.isServerRunning());
    }
}
