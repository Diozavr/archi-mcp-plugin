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

import java.util.List;
import java.util.Map;

import org.junit.Test;

import java.nio.charset.StandardCharsets;

import ru.cinimex.archimatetool.mcp.server.JacksonJson;

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


