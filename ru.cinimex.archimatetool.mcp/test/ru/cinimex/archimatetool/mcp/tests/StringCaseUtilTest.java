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
        // kebab-case to PascalCase (jArchi API compatibility)
        assertEquals("BusinessActor", StringCaseUtil.toCamelCase("business-actor"));
        assertEquals("DataObject", StringCaseUtil.toCamelCase("data-object"));
        assertEquals("ApplicationComponent", StringCaseUtil.toCamelCase("application-component"));
        assertEquals("AssignmentRelationship", StringCaseUtil.toCamelCase("assignment-relationship"));
    }

    @Test
    public void testToCamelCaseEdgeCases() {
        assertEquals("", StringCaseUtil.toCamelCase("--"));
        assertNull(StringCaseUtil.toCamelCase(null));
        assertEquals("AB", StringCaseUtil.toCamelCase("a-b"));
        assertEquals("A", StringCaseUtil.toCamelCase("a-"));
    }

    @Test
    public void testToCamelCaseEMFCompatibility() {
        // Already correct PascalCase should remain unchanged (EMF format)
        assertEquals("BusinessActor", StringCaseUtil.toCamelCase("BusinessActor"));
        assertEquals("AssignmentRelationship", StringCaseUtil.toCamelCase("AssignmentRelationship"));
        assertEquals("Unknown", StringCaseUtil.toCamelCase("unknown"));
        assertEquals("MyCustomType", StringCaseUtil.toCamelCase("my-custom-type"));
    }

    @Test
    public void testToKebabCase() {
        assertEquals("business-actor", StringCaseUtil.toKebabCase("BusinessActor"));
        assertEquals("assignment-relationship", StringCaseUtil.toKebabCase("AssignmentRelationship"));
        assertEquals("application-component", StringCaseUtil.toKebabCase("ApplicationComponent"));
        assertEquals("data-object", StringCaseUtil.toKebabCase("DataObject"));
    }

    @Test
    public void testToKebabCaseEdgeCases() {
        assertEquals("", StringCaseUtil.toKebabCase(""));
        assertNull(StringCaseUtil.toKebabCase(null));
        assertEquals("a", StringCaseUtil.toKebabCase("A"));
        assertEquals("ab", StringCaseUtil.toKebabCase("AB")); // Abbreviation without separator
        assertEquals("abc", StringCaseUtil.toKebabCase("ABC")); // All caps abbreviation
        assertEquals("html-parser", StringCaseUtil.toKebabCase("HTMLParser")); // Abbreviation + word
    }
}


