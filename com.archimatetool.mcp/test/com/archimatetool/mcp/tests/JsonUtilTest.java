package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import com.archimatetool.mcp.server.JacksonJson;

public class JsonUtilTest {

    @Test
    public void testSerializeSimple() throws Exception {
        String json = toJson(Map.of("ok", true, "n", 1, "s", "x"));
        assertTrue(json.contains("\"ok\":true"));
        assertTrue(json.contains("\"n\":1"));
        assertTrue(json.contains("\"s\":\"x\""));
    }

    @Test
    public void testSerializeNested() throws Exception {
        String json = toJson(Map.of(
            "arr", List.of(1, 2, 3),
            "obj", Map.of("a", "b")
        ));
        assertTrue(json.contains("\"arr\":[1,2,3]"));
        assertTrue(json.contains("\"obj\":{\"a\":\"b\"}"));
    }

    @Test
    public void testEscape() throws Exception {
        String json = toJson(Map.of("s", "\"\\\n\r\t"));
        assertTrue(json.contains("\\\""));
        assertTrue(json.contains("\\\\"));
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\r"));
        assertTrue(json.contains("\\t"));
    }

    @Test
    public void testUnicode() throws Exception {
        String json = toJson(Map.of("s", "Привет"));
        assertTrue(json.contains("Привет"));
    }

    private static String toJson(Object body) throws Exception {
        return new String(JacksonJson.writeBytes(body), StandardCharsets.UTF_8);
    }
}


