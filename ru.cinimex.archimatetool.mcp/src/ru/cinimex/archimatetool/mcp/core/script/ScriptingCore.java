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
package ru.cinimex.archimatetool.mcp.core.script;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import ru.cinimex.archimatetool.mcp.core.errors.UnprocessableException;
import ru.cinimex.archimatetool.mcp.core.errors.TimeoutException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.widgets.Display;

/** Core helper for interacting with the Archi scripting plugin. */
public class ScriptingCore {

    /** Maximum number of characters captured for stdout/stderr. */
    private static final int MAX_LOG_CHARS = 10_000;

    /** Determine whether a compatible scripting plugin is installed. */
    public boolean isPluginInstalled() {
        return Platform.getBundle("com.archimatetool.script") != null;
    }

    /** List available script engines (e.g. ajs, groovy). */
    public List<String> listEngines() {
        if (!isPluginInstalled()) {
            return List.of();
        }
        List<String> engines = new ArrayList<>();
        
        // The main script plugin supports JavaScript/AJS by default
        if (Platform.getBundle("com.archimatetool.script") != null) {
            engines.add("ajs");
        }
        
        // Check for additional script engines based on actual bundle names
        if (Platform.getBundle("com.archimatetool.script.groovy") != null) {
            engines.add("groovy");
        }
        if (Platform.getBundle("com.archimatetool.script.jruby") != null) {
            engines.add("jruby");
        }
        
        return engines;
    }

    /** Get comprehensive documentation for autonomous agents. */
    public Map<String, Object> getAgentDocumentation() {
        Map<String, Object> documentation = new HashMap<>();
        
        // jArchi scripting guide
        documentation.put("jArchiGuide", AgentDocumentation.JARCHI_GUIDE);
        
        // Common patterns with code examples
        documentation.put("commonPatterns", AgentDocumentation.COMMON_PATTERNS);
        
        // ArchiMate modeling context
        documentation.put("archiMateContext", AgentDocumentation.ARCHIMATE_CONTEXT);
        
        return documentation;
    }

    /**
     * Run a script using the installed jArchi plugin. This uses reflection so that
     * the plugin remains an optional dependency at compile time.
     */
    public ScriptResult run(ScriptRequest req) throws CoreException {
        if (!isPluginInstalled()) {
            throw new UnsupportedOperationException("scripting plugin is not installed");
        }

        Callable<ScriptResult> task = () -> {
            long start = System.currentTimeMillis();
            File tmp = null;
            ByteArrayOutputStream outBuf = new ByteArrayOutputStream();
            ByteArrayOutputStream errBuf = new ByteArrayOutputStream();
            PrintStream origOut = System.out;
            PrintStream origErr = System.err;
            try {
                String ext = switch (req.engine()) {
                    case "groovy" -> ".groovy";
                    case "jruby" -> ".rb";
                    default -> ".ajs";
                };
                tmp = File.createTempFile("mcp-script", ext);
                Files.writeString(tmp.toPath(), req.code(), StandardCharsets.UTF_8);

                System.setOut(new PrintStream(outBuf, true, StandardCharsets.UTF_8));
                System.setErr(new PrintStream(errBuf, true, StandardCharsets.UTF_8));

                Class<?> runnerClass = Class.forName("com.archimatetool.script.RunArchiScript");
                Object runner = runnerClass.getConstructor(File.class).newInstance(tmp);

                Display.getDefault().syncExec(() -> {
                    try {
                        runnerClass.getMethod("run").invoke(runner);
                    } catch (Exception e) {
                        System.err.println("Script execution error: " + e.getMessage());
                        if (e.getCause() != null) {
                            System.err.println("Caused by: " + e.getCause().getMessage());
                        }
                        e.printStackTrace();
                        throw new RuntimeException("Script execution failed: " + e.getMessage(), e);
                    }
                });

                long duration = System.currentTimeMillis() - start;
                String stdout = truncate(outBuf.toString(StandardCharsets.UTF_8));
                String stderr = truncate(errBuf.toString(StandardCharsets.UTF_8));
                return new ScriptResult(true, null,
                    stdout.isEmpty() ? null : stdout,
                    stderr.isEmpty() ? null : stderr,
                    duration);
            } finally {
                System.setOut(origOut);
                System.setErr(origErr);
                if (tmp != null) {
                    try {
                        Files.deleteIfExists(tmp.toPath());
                    } catch (IOException ignore) {}
                }
            }
        };

        ExecutorService exec = Executors.newSingleThreadExecutor();
        try {
            Future<ScriptResult> fut = exec.submit(task);
            if (req.timeoutMs() != null && req.timeoutMs() > 0) {
                try {
                    return fut.get(req.timeoutMs(), TimeUnit.MILLISECONDS);
                } catch (java.util.concurrent.TimeoutException e) {
                    fut.cancel(true);
                    throw new TimeoutException("script timed out");
                }
            }
            return fut.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new UnprocessableException("script interrupted", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof CoreException ce) {
                throw ce;
            }
            String errorMessage = "script execution failed";
            if (cause != null && cause.getMessage() != null) {
                errorMessage += ": " + cause.getMessage();
            }
            throw new UnprocessableException(errorMessage, cause);
        } finally {
            exec.shutdownNow();
        }
    }

    /** Truncate a log string if it exceeds {@link #MAX_LOG_CHARS}. */
    private String truncate(String log) {
        if (log.length() > MAX_LOG_CHARS) {
            return log.substring(0, MAX_LOG_CHARS) + "...";
        }
        return log;
    }
}
