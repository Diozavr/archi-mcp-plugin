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

import ru.cinimex.archimatetool.mcp.util.StringCaseUtil;

public class StringCaseUtilTest {

    @Test
    public void testToCamelCase() {
        assertEquals("BusinessActor", StringCaseUtil.toCamelCase("business-actor"));
        assertEquals("DataObject", StringCaseUtil.toCamelCase("data-object"));
        assertEquals("ApplicationComponent", StringCaseUtil.toCamelCase("application-component"));
    }

    @Test
    public void testToCamelCaseEdgeCases() {
        assertEquals("", StringCaseUtil.toCamelCase("--"));
        assertNull(StringCaseUtil.toCamelCase(null));
        assertEquals("AB", StringCaseUtil.toCamelCase("a-b"));
        assertEquals("A", StringCaseUtil.toCamelCase("a-"));
    }

    @Test
    public void testCustomTypes() {
        assertEquals("MyCustomType", StringCaseUtil.toCamelCase("my-custom-type"));
        assertEquals("Unknown", StringCaseUtil.toCamelCase("unknown"));
    }
}


