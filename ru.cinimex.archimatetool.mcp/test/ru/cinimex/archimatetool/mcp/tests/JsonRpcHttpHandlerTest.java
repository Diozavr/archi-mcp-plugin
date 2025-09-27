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
package ru.cinimex.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import ru.cinimex.archimatetool.mcp.http.handlers.JsonRpcHttpHandler;
import ru.cinimex.archimatetool.mcp.server.JacksonJson;
import ru.cinimex.archimatetool.mcp.server.tools.Tool;
import ru.cinimex.archimatetool.mcp.server.tools.ToolParam;
import ru.cinimex.archimatetool.mcp.server.tools.ToolRegistry;
import ru.cinimex.archimatetool.mcp.core.script.ScriptingCore;
import ru.cinimex.archimatetool.mcp.core.script.ScriptRequest;
import ru.cinimex.archimatetool.mcp.core.script.ScriptResult;
import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Unit tests for the minimal JSON-RPC handler.
 *
 * The handler is exercised using {@link FakeHttpExchange} to avoid a running
 * HTTP server. Only protocol-level behaviour is verified; the underlying core
 * implementations are not exercised.
 */
public class JsonRpcHttpHandlerTest {

    static class StubScriptingCore extends ScriptingCore {
        boolean installed;
        List<String> engines;
        Map<String, Object> docs;
        ScriptResult nextResult;
        ScriptRequest lastRequest;

        StubScriptingCore(boolean installed, List<String> engines, Map<String, Object> docs, ScriptResult nextResult) {
            this.installed = installed;
            this.engines = engines != null ? engines : List.of();
            this.docs = docs;
            this.nextResult = nextResult;
        }

        @Override
        public boolean isPluginInstalled() {
            return installed;
        }

        @Override
        public List<String> listEngines() {
            return engines;
        }

        @Override
        public Map<String, Object> getAgentDocumentation() {
            return docs != null ? docs : super.getAgentDocumentation();
        }

        @Override
        protected ScriptResult execute(ScriptRequest req) throws CoreException {
            this.lastRequest = req;
            return nextResult != null ? nextResult : new ScriptResult(true, null, null, null, 0);
        }
    }

    private Object replaceScriptingCore(ScriptingCore replacement) throws Exception {
        Field field = ToolRegistry.class.getDeclaredField("scriptingCore");
        field.setAccessible(true);
        Object original = field.get(null);
        field.set(null, replacement);
        return original;
    }

    private void restoreScriptingCore(Object original) throws Exception {
        Field field = ToolRegistry.class.getDeclaredField("scriptingCore");
        field.setAccessible(true);
        field.set(null, original);
    }

    @Test
    public void testRejectsNonPost() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/mcp", null);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(405, ex.getResponseCode());
    }

    @Test
    public void testParseError() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", "{bad json}");
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals(-32700, root.get("error").get("code").asInt());
    }

    @Test
    public void testMethodNotFound() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"bogus\"}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals(-32601, root.get("error").get("code").asInt());
        assertEquals(1, root.get("id").asInt());
    }

    @Test
    public void testNotificationAccepted() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\",\"params\":{\"name\":\"status\",\"args\":{}}}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(202, ex.getResponseCode());
        assertEquals("", ex.getResponseString());
    }

    @Test
    public void testBatchWithNotification() throws Exception {
        String req = "[" +
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\",\"params\":{}}," +
            "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}]";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertTrue(root.isArray());
        assertEquals(1, root.size());
        assertEquals(1, root.get(0).get("id").asInt());
    }

    @Test
    public void testBatchAllNotificationsAccepted() throws Exception {
        String req = "[" +
            "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}," +
            "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\",\"params\":{\"name\":\"status\",\"args\":{}}}]";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(202, ex.getResponseCode());
        assertEquals("", ex.getResponseString());
    }

    @Test
    public void testInitialize() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"initialize\"}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals("2025-06-18", root.get("result").get("protocolVersion").asText());
    }

    @Test
    public void testNotificationsInitializedAccepted() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(202, ex.getResponseCode());
    }

    @Test
    public void testToolsList() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/list\",\"params\":{}}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertTrue(root.get("result").isObject());
        assertTrue(root.get("result").get("tools").isArray());
        assertTrue(root.get("result").get("tools").size() > 0);
    }

    @Test
    public void testInvalidParams() throws Exception {
        Field f = ToolRegistry.class.getDeclaredField("TOOLS");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Tool> tools = (Map<String, Tool>) f.get(null);
        tools.put("echo", new Tool(
            "echo",
            "Echo back a message",
            Arrays.asList(new ToolParam("msg", "string", true, "Message to echo", null)),
            params -> params
        ));
        String req = "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\",\"params\":{\"name\":\"echo\",\"args\":{}}}"; 
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals(-32602, root.get("error").get("code").asInt());
    }

    @Test
    public void testToolsCallUnknown() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\",\"params\":{\"name\":\"bogus\",\"args\":{}}}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals(-32601, root.get("error").get("code").asInt());
    }

    @Test
    public void testStatusHappyPath() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"id\":8,\"method\":\"tools/call\",\"params\":{\"name\":\"status\",\"args\":{}}}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        // Check MCP tool response format
        JsonNode content = root.get("result").get("content");
        assertNotNull(content);
        assertTrue(content.isArray());
        assertTrue(content.size() > 0);
        JsonNode firstContent = content.get(0);
        assertEquals("text", firstContent.get("type").asText());
        
        // Parse the actual tool result from the text field
        JsonNode toolResult = JacksonJson.mapper().readTree(firstContent.get("text").asText());
        assertTrue(toolResult.get("ok").asBoolean());
        assertEquals(8, root.get("id").asInt());
    }

    @Test
    public void testListScriptEnginesWhenNotInstalled() throws Exception {
        StubScriptingCore stub = new StubScriptingCore(false, List.of(), null, null);
        Object original = replaceScriptingCore(stub);
        try {
            String req = "{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"tools/call\",\"params\":{\"name\":\"list_script_engines\",\"arguments\":{}}}";
            FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
            new JsonRpcHttpHandler().handle(ex);
            assertEquals(200, ex.getResponseCode());
            JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
            // Check MCP tool response format
            JsonNode content = root.get("result").get("content");
            assertNotNull(content);
            assertTrue(content.isArray());
            assertTrue(content.size() > 0);
            JsonNode firstContent = content.get(0);
            assertEquals("text", firstContent.get("type").asText());
            
            // Parse the actual tool result from the text field
            JsonNode toolResult = JacksonJson.mapper().readTree(firstContent.get("text").asText());
            assertFalse(toolResult.get("installed").asBoolean());
            assertEquals(0, toolResult.get("engines").size());
            assertNull(toolResult.get("documentation"));
        } finally {
            restoreScriptingCore(original);
        }
    }

    @Test
    public void testListScriptEnginesWhenInstalled() throws Exception {
        Map<String, Object> docs = Map.of("guide", "doc");
        StubScriptingCore stub = new StubScriptingCore(true, List.of("ajs", "groovy"), docs, null);
        Object original = replaceScriptingCore(stub);
        try {
            String req = "{\"jsonrpc\":\"2.0\",\"id\":10,\"method\":\"tools/call\",\"params\":{\"name\":\"list_script_engines\",\"arguments\":{}}}";
            FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
            new JsonRpcHttpHandler().handle(ex);
            JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
            // Check MCP tool response format
            JsonNode content = root.get("result").get("content");
            assertNotNull(content);
            assertTrue(content.isArray());
            assertTrue(content.size() > 0);
            JsonNode firstContent = content.get(0);
            assertEquals("text", firstContent.get("type").asText());
            
            // Parse the actual tool result from the text field
            JsonNode toolResult = JacksonJson.mapper().readTree(firstContent.get("text").asText());
            assertTrue(toolResult.get("installed").asBoolean());
            assertEquals(2, toolResult.get("engines").size());
            assertTrue(toolResult.get("documentation").isObject());
        } finally {
            restoreScriptingCore(original);
        }
    }

    @Test
    public void testRunScriptSuccess() throws Exception {
        ScriptResult res = new ScriptResult(true, "answer", "out", "err", 42);
        StubScriptingCore stub = new StubScriptingCore(true, List.of("ajs"), Map.of(), res);
        Object original = replaceScriptingCore(stub);
        try {
            String req = "{\"jsonrpc\":\"2.0\",\"id\":11,\"method\":\"tools/call\",\"params\":{\"name\":\"run_script\",\"arguments\":{\"code\":\"return 1\",\"engine\":\"ajs\",\"timeout_ms\":1000,\"bindings\":{\"foo\":\"bar\"},\"log\":\"stdout\"}}}";
            FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
            new JsonRpcHttpHandler().handle(ex);
            JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
            // Check MCP tool response format
            JsonNode content = root.get("result").get("content");
            assertNotNull(content);
            assertTrue(content.isArray());
            assertTrue(content.size() > 0);
            JsonNode firstContent = content.get(0);
            assertEquals("text", firstContent.get("type").asText());
            
            // Parse the actual tool result from the text field
            JsonNode toolResult = JacksonJson.mapper().readTree(firstContent.get("text").asText());
            assertTrue(toolResult.get("ok").asBoolean());
            assertEquals("answer", toolResult.get("result").asText());
            assertEquals("out", toolResult.get("stdout").asText());
            assertEquals("err", toolResult.get("stderr").asText());
            assertEquals(42, toolResult.get("durationMs").asInt());
            assertNotNull(stub.lastRequest);
            assertEquals(Integer.valueOf(1000), stub.lastRequest.timeoutMs());
        } finally {
            restoreScriptingCore(original);
        }
    }

    @Test
    public void testRunScriptValidatesMissingCode() throws Exception {
        StubScriptingCore stub = new StubScriptingCore(true, List.of("ajs"), Map.of(), new ScriptResult(true, null, null, null, 0));
        Object original = replaceScriptingCore(stub);
        try {
            String req = "{\"jsonrpc\":\"2.0\",\"id\":12,\"method\":\"tools/call\",\"params\":{\"name\":\"run_script\",\"arguments\":{}}}";
            FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
            new JsonRpcHttpHandler().handle(ex);
            JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
            assertEquals(-32602, root.get("error").get("code").asInt());
            assertEquals("invalid params", root.get("error").get("message").asText());
            assertEquals("missing required param 'code'", root.get("error").get("data").get("error").asText());
        } finally {
            restoreScriptingCore(original);
        }
    }

    @Test
    public void testRunScriptWhenPluginMissing() throws Exception {
        StubScriptingCore stub = new StubScriptingCore(false, List.of(), null, null);
        Object original = replaceScriptingCore(stub);
        try {
            String req = "{\"jsonrpc\":\"2.0\",\"id\":13,\"method\":\"tools/call\",\"params\":{\"name\":\"run_script\",\"arguments\":{\"code\":\"print(1)\"}}}";
            FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
            new JsonRpcHttpHandler().handle(ex);
            JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
            assertEquals(-32051, root.get("error").get("code").asInt());
            assertEquals("Not Implemented: install a compatible jArchi to enable /script APIs", root.get("error").get("message").asText());
        } finally {
            restoreScriptingCore(original);
        }
    }
}

