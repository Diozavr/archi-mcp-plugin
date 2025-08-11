package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import com.archimatetool.mcp.http.handlers.TypesHttpHandler;
import com.archimatetool.mcp.server.JacksonJson;
import com.fasterxml.jackson.databind.JsonNode;

public class TypesHttpHandlerTest {

    @Test
    public void testListsNotEmpty() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/types", null);
        new TypesHttpHandler().handle(ex);
        assertEquals(200, ex.getResponseCode());
        JsonNode root = JacksonJson.mapper().readTree(ex.getResponseString());
        assertTrue(root.get("elementTypes").size() > 0);
        assertTrue(root.get("relationTypes").size() > 0);
        assertTrue(root.get("viewTypes").size() > 0);
    }
}
