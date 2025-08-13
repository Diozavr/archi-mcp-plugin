package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import com.archimatetool.mcp.http.handlers.JsonRpcHttpHandler;
import com.archimatetool.mcp.server.JacksonJson;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * Unit tests for the minimal JSON-RPC handler.
 *
 * The handler is exercised using {@link FakeHttpExchange} to avoid a running
 * HTTP server. Only protocol-level behaviour is verified; the underlying core
 * implementations are not exercised.
 */
public class JsonRpcHttpHandlerTest {

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
        String req = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"bogus.method\"}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals(-32601, root.get("error").get("code").asInt());
        assertEquals(1, root.get("id").asInt());
    }

    @Test
    public void testNotificationNoContent() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"method\":\"status\"}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(204, ex.getResponseCode());
        assertEquals("", ex.getResponseString());
    }

    @Test
    public void testBatchWithNotification() throws Exception {
        String req = "[" +
            "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"status\"}," +
            "{\"jsonrpc\":\"2.0\",\"method\":\"status\"}]";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertTrue(root.isArray());
        assertEquals(1, root.size());
        assertEquals(1, root.get(0).get("id").asInt());
    }

    @Test
    public void testBatchAllNotifications() throws Exception {
        String req = "[" +
            "{\"jsonrpc\":\"2.0\",\"method\":\"status\"}," +
            "{\"jsonrpc\":\"2.0\",\"method\":\"types\"}]";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(204, ex.getResponseCode());
        assertEquals("", ex.getResponseString());
    }

    @Test
    public void testToolsList() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/list\"}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertTrue(root.get("result").isArray());
        assertTrue(root.get("result").size() > 0);
    }

    @Test
    public void testInvalidParams() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"elements/get\",\"params\":{}}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals(-32602, root.get("error").get("code").asInt());
    }

    @Test
    public void testStatusHappyPath() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"status\"}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertTrue(root.get("result").get("ok").asBoolean());
        assertEquals(7, root.get("id").asInt());
    }
}

