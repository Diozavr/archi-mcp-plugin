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

import org.junit.Test;

import ru.cinimex.archimatetool.mcp.http.handlers.TypesHttpHandler;
import ru.cinimex.archimatetool.mcp.server.JacksonJson;
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

    @Test
    public void testRejectsNonGet() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("POST", "/types", "{}");
        new TypesHttpHandler().handle(ex);
        assertEquals(405, ex.getResponseCode());
    }
}
