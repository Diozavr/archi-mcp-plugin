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
        String req = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"bogus\"}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals(-32601, root.get("error").get("code").asInt());
        assertEquals(1, root.get("id").asInt());
    }

    @Test
    public void testNotificationNoContent() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\",\"params\":{\"name\":\"status\",\"args\":{}}}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(204, ex.getResponseCode());
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
    public void testBatchAllNotifications() throws Exception {
        String req = "[" +
            "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}," +
            "{\"jsonrpc\":\"2.0\",\"method\":\"tools/call\",\"params\":{\"name\":\"status\",\"args\":{}}}]";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(204, ex.getResponseCode());
        assertEquals("", ex.getResponseString());
    }

    @Test
    public void testInitialize() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"initialize\"}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals("2024-11-05", root.get("result").get("protocolVersion").asText());
    }

    @Test
    public void testNotificationsInitialized() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(204, ex.getResponseCode());
    }

    @Test
    public void testToolsList() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/list\",\"params\":{}}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        new JsonRpcHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertTrue(root.get("result").isArray());
        assertTrue(root.get("result").size() > 0);
    }

    @Test
    public void testInvalidParams() throws Exception {
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
        assertTrue(root.get("result").get("ok").asBoolean());
        assertEquals(8, root.get("id").asInt());
    }
}

