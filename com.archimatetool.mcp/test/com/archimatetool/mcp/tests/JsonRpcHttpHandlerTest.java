package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import com.archimatetool.mcp.http.handlers.JsonRpcHttpHandler;
import com.archimatetool.mcp.server.JacksonJson;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import com.archimatetool.mcp.core.elements.ElementsCore;
import com.archimatetool.mcp.core.types.GetElementQuery;
import com.archimatetool.mcp.core.relations.RelationsCore;
import com.archimatetool.mcp.core.views.ViewsCore;
import com.archimatetool.mcp.core.search.SearchCore;
import com.archimatetool.mcp.core.folders.FoldersCore;
import com.archimatetool.mcp.core.model.ModelCore;

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
    public void testElementsGetHappyPath() throws Exception {
        class StubElementsCore extends ElementsCore {
            @Override
            public Map<String, Object> getElement(GetElementQuery q) {
                return Map.of("id", q.id, "name", "Dummy");
            }
        }
        JsonRpcHttpHandler handler = new JsonRpcHttpHandler(
                new StubElementsCore(), new RelationsCore(), new ViewsCore(),
                new SearchCore(), new FoldersCore(), new ModelCore());
        String req = "{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"elements.get\",\"params\":{\"id\":\"e1\"}}";
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/mcp", req);
        handler.handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertEquals("e1", root.get("result").get("id").asText());
        assertEquals(7, root.get("id").asInt());
    }
}
