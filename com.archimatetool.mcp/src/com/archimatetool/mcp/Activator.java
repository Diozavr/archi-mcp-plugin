package com.archimatetool.mcp;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.archimatetool.mcp.server.HttpServerRunner;

public class Activator implements BundleActivator {

    private static Activator instance;
    private HttpServerRunner serverRunner;

    public static Activator getDefault() {
        return instance;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        instance = this;
        serverRunner = new HttpServerRunner();
        serverRunner.start();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        if (serverRunner != null) {
            serverRunner.stop();
            serverRunner = null;
        }
        instance = null;
    }
}

