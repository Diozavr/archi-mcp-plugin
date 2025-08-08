package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.archimatetool.mcp.server.JsonUtil;

public class JsonUtilTest {

    @Test
    public void testSerializeSimple() throws Exception {
        String json = invokeToJson(Map.of("ok", true, "n", 1, "s", "x"));
        assertTrue(json.contains("\"ok\":true"));
        assertTrue(json.contains("\"n\":1"));
        assertTrue(json.contains("\"s\":\"x\""));
    }

    @Test
    public void testSerializeNested() throws Exception {
        String json = invokeToJson(Map.of(
            "arr", List.of(1, 2, 3),
            "obj", Map.of("a", "b")
        ));
        assertTrue(json.contains("\"arr\":[1,2,3]"));
        assertTrue(json.contains("\"obj\":{\"a\":\"b\"}"));
    }

    @Test
    public void testEscape() throws Exception {
        String json = invokeToJson(Map.of("s", "\"\\\n\r\t"));
        assertTrue(json.contains("\\\""));
        assertTrue(json.contains("\\\\"));
        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\r"));
        assertTrue(json.contains("\\t"));
    }

    private static String invokeToJson(Object body) throws Exception {
        var m = JsonUtil.class.getDeclaredMethod("toJson", Object.class);
        m.setAccessible(true);
        return (String) m.invoke(null, body);
    }
}


