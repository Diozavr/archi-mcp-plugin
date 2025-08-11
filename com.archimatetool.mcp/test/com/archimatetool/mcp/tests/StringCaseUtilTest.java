package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import com.archimatetool.mcp.util.StringCaseUtil;

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


