package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.archimatetool.mcp.core.script.ScriptingCore;
import com.archimatetool.mcp.http.handlers.ScriptEnginesHttpHandler;
import com.archimatetool.mcp.server.JacksonJson;
import com.fasterxml.jackson.databind.JsonNode;

public class ScriptEnginesHttpHandlerTest {

    static class StubCore extends ScriptingCore {
        private final boolean installed;
        private final List<String> engines;
        StubCore(boolean installed, List<String> engines) {
            this.installed = installed;
            this.engines = engines;
        }
        @Override public boolean isPluginInstalled() { return installed; }
        @Override public List<String> listEngines() { return engines; }
    }

    @Test
    public void testInstalledEngines() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/script/engines", null);
        new ScriptEnginesHttpHandler(new StubCore(true, List.of("ajs"))).handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertTrue(root.get("installed").asBoolean());
        assertEquals("ajs", root.get("engines").get(0).asText());
    }

    @Test
    public void testUninstalledEngines() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/script/engines", null);
        new ScriptEnginesHttpHandler(new StubCore(false, List.of())).handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertFalse(root.get("installed").asBoolean());
        assertEquals(0, root.get("engines").size());
    }

    @Test
    public void testRejectsNonGet() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/script/engines", null);
        new ScriptEnginesHttpHandler(new StubCore(false, List.of())).handle(ex);
        assertEquals(405, ex.getResponseCode());
    }

    @Test
    public void testMultipleEnginesSupported() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/script/engines", null);
        new ScriptEnginesHttpHandler(new StubCore(true, List.of("ajs", "groovy", "jruby"))).handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertTrue(root.get("installed").asBoolean());
        assertEquals(3, root.get("engines").size());
        assertEquals("ajs", root.get("engines").get(0).asText());
        assertEquals("groovy", root.get("engines").get(1).asText());
        assertEquals("jruby", root.get("engines").get(2).asText());
    }
}
