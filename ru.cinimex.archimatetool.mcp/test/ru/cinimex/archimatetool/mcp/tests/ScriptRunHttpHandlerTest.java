package ru.cinimex.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import ru.cinimex.archimatetool.mcp.core.errors.TimeoutException;
import ru.cinimex.archimatetool.mcp.core.script.ScriptingCore;
import ru.cinimex.archimatetool.mcp.core.script.ScriptRequest;
import ru.cinimex.archimatetool.mcp.core.script.ScriptResult;
import ru.cinimex.archimatetool.mcp.http.handlers.ScriptRunHttpHandler;
import ru.cinimex.archimatetool.mcp.server.JacksonJson;
import com.fasterxml.jackson.databind.JsonNode;

public class ScriptRunHttpHandlerTest {

    static class StubCore extends ScriptingCore {
        private final boolean installed;
        private final ScriptResult result;
        private final java.util.List<String> engines;
        StubCore(boolean installed, ScriptResult result, java.util.List<String> engines) {
            this.installed = installed;
            this.result = result;
            this.engines = engines;
        }
        @Override public boolean isPluginInstalled() { return installed; }
        @Override public java.util.List<String> listEngines() { return engines; }
        @Override public ScriptResult run(ScriptRequest req) { return result; }
    }

    @Test
    public void testReturns501WhenUninstalled() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/script/run", "{\"code\":\"print('ok')\"}");
        new ScriptRunHttpHandler(new StubCore(false, null, java.util.List.of("ajs"))).handle(ex);
        assertEquals(501, ex.getResponseCode());
    }

    @Test
    public void testRunsWhenInstalled() throws Exception {
        ScriptResult res = new ScriptResult(true, "ok", "out", null, 5);
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/script/run", "{\"code\":\"1+1\"}");
        new ScriptRunHttpHandler(new StubCore(true, res, java.util.List.of("ajs"))).handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertTrue(root.get("ok").asBoolean());
        assertEquals(5, root.get("durationMs").asInt());
        assertEquals("out", root.get("stdout").asText());
    }

    @Test
    public void testValidatesCode() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/script/run", "{}");
        new ScriptRunHttpHandler(new StubCore(true, new ScriptResult(true, null, null, null, 0), java.util.List.of("ajs"))).handle(ex);
        assertEquals(400, ex.getResponseCode());
    }

    @Test
    public void testRejectsNonPost() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/script/run", null);
        new ScriptRunHttpHandler(new StubCore(true, null, java.util.List.of("ajs"))).handle(ex);
        assertEquals(405, ex.getResponseCode());
    }

    @Test
    public void testTimeoutMapsTo504() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/script/run", "{\"code\":\"print(1)\"}");
        ScriptingCore core = new StubCore(true, null, java.util.List.of("ajs")) {
            @Override public ScriptResult run(ScriptRequest req) {
                throw new TimeoutException("timeout");
            }
        };
        new ScriptRunHttpHandler(core).handle(ex);
        assertEquals(504, ex.getResponseCode());
    }

    @Test
    public void testValidatesEngine() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/script/run", "{\"code\":\"1\",\"engine\":\"bad\"}");
        new ScriptRunHttpHandler(new StubCore(true, null, java.util.List.of("ajs"))).handle(ex);
        assertEquals(400, ex.getResponseCode());
    }

    @Test
    public void testValidatesTimeout() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/script/run", "{\"code\":\"1\",\"timeoutMs\":0}");
        new ScriptRunHttpHandler(new StubCore(true, null, java.util.List.of("ajs"))).handle(ex);
        assertEquals(400, ex.getResponseCode());
    }

    @Test
    public void testValidatesLogParam() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/script/run", "{\"code\":\"1\",\"log\":\"bad\"}");
        new ScriptRunHttpHandler(new StubCore(true, null, java.util.List.of("ajs"))).handle(ex);
        assertEquals(400, ex.getResponseCode());
    }

    @Test
    public void testNoActiveModelMapsTo409() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/script/run", "{\"code\":\"print(1)\"}");
        ScriptingCore core = new StubCore(true, null, java.util.List.of("ajs")) {
            @Override public ScriptResult run(ScriptRequest req) {
                throw new ru.cinimex.archimatetool.mcp.core.errors.ConflictException("no active model");
            }
        };
        new ScriptRunHttpHandler(core).handle(ex);
        assertEquals(409, ex.getResponseCode());
    }

    @Test
    public void testScriptExecutionErrorIncludesDetailedMessage() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/script/run", "{\"code\":\"invalid code\",\"engine\":\"ajs\"}");
        ScriptingCore core = new StubCore(true, null, java.util.List.of("ajs")) {
            @Override public ScriptResult run(ScriptRequest req) {
                throw new ru.cinimex.archimatetool.mcp.core.errors.UnprocessableException("script execution failed: ReferenceError: undefined variable");
            }
        };
        new ScriptRunHttpHandler(core).handle(ex);
        assertEquals(422, ex.getResponseCode());
        assertTrue("Should contain detailed error message", ex.getResponseString().contains("ReferenceError"));
    }
}
