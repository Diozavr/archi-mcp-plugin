package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

import com.archimatetool.mcp.http.handlers.JsonRpcHttpHandler;
import com.archimatetool.mcp.server.JacksonJson;
import com.archimatetool.mcp.server.tools.Param;
import com.archimatetool.mcp.server.tools.Tool;
import com.archimatetool.mcp.server.tools.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;

public class JsonRpcHttpHandlerTest {

    private JsonRpcHttpHandler handlerWithStub() {
        ToolRegistry reg = new ToolRegistry();
        reg.register(new Tool("status", "", Map.of(), args -> Map.of("ok", true)));
        reg.register(new Tool("search", "", Map.of(
                "q", new Param("q", "string", "", false)
        ), args -> Map.of("items", java.util.List.of())));
        reg.register(new Tool("get_view_image", "", Map.of(
                "id", new Param("id", "string", "", true)
        ), args -> Map.of("data", "", "contentType", "image/png")));
        return new JsonRpcHttpHandler(reg);
    }

    @Test
    public void testRejectsNonPost() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/mcp", null);
        handlerWithStub().handle(ex);
        assertEquals(405, ex.getResponseCode());
    }

    @Test
    public void testParseError() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", "{bad json}");
        handlerWithStub().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals(-32700, root.get("error").get("code").asInt());
    }

    @Test
    public void testMethodNotFound() throws Exception {
        String req = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"bogus.method\"}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        handlerWithStub().handle(ex);
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals(-32601, root.get("error").get("code").asInt());
    }

    @Test
    public void testBatchRequests() throws Exception {
        String req = "[{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"},{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"bogus\"}]";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        handlerWithStub().handle(ex);
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertTrue(root.isArray());
        assertEquals(2, root.size());
    }

    @Test
    public void testToolsListAndCall() throws Exception {
        JsonRpcHttpHandler handler = handlerWithStub();
        String listReq = "{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/list\"}";
        FakeHttpExchange ex1 = new FakeHttpExchange("POST", "/mcp", listReq);
        handler.handle(ex1);
        JsonNode listRoot = JacksonJson.mapper().readTree(ex1.getResponseString());
        assertTrue(listRoot.get("result").isArray());
        String callReq = "{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"tools/call\",\"params\":{\"name\":\"status\"}}";
        FakeHttpExchange ex2 = new FakeHttpExchange("POST", "/mcp", callReq);
        handler.handle(ex2);
        JsonNode callRoot = JacksonJson.mapper().readTree(ex2.getResponseString());
        assertTrue(callRoot.get("result").get("ok").asBoolean());
    }

    @Test
    public void testToolsCallMissingName() throws Exception {
        JsonRpcHttpHandler handler = handlerWithStub();
        String req = "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\",\"params\":{}}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        handler.handle(ex);
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals(-32602, root.get("error").get("code").asInt());
    }
}
