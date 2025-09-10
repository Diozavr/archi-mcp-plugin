package ru.cinimex.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import ru.cinimex.archimatetool.mcp.json.JsonReader;

public class JsonReaderTest {

    @Test
    public void testSimpleExtraction() {
        JsonReader jr = JsonReader.fromString("{\"type\":\"BusinessActor\",\"name\":\"A\",\"n\":123,\"b\":true}");
        assertEquals("BusinessActor", jr.optString("type"));
        assertEquals("A", jr.optString("name"));
        assertEquals(Integer.valueOf(123), jr.optInt("n"));
        assertEquals(Boolean.TRUE, jr.optBool("b"));
    }

    @Test
    public void testBoundsNestedAndFlat() {
        JsonReader jr1 = JsonReader.fromString("{\"bounds\":{\"x\":10,\"y\":20,\"w\":120,\"h\":80}}");
        assertEquals(Integer.valueOf(10), jr1.optIntWithin("bounds", "x"));
        assertEquals(Integer.valueOf(20), jr1.optIntWithin("bounds", "y"));
        assertEquals(Integer.valueOf(120), jr1.optIntWithin("bounds", "w"));
        assertEquals(Integer.valueOf(80), jr1.optIntWithin("bounds", "h"));

        JsonReader jr2 = JsonReader.fromString("{\"x\":10,\"y\":20,\"w\":120,\"h\":80}");
        assertEquals(Integer.valueOf(10), jr2.optInt("x"));
        assertEquals(Integer.valueOf(20), jr2.optInt("y"));
        assertEquals(Integer.valueOf(120), jr2.optInt("w"));
        assertEquals(Integer.valueOf(80), jr2.optInt("h"));
    }

    @Test
    public void testNumbersAsStrings() {
        JsonReader jr = JsonReader.fromString("{\"x\":\"42\"}");
        assertEquals(Integer.valueOf(42), jr.optInt("x"));
    }

    @Test
    public void testMalformedToEmpty() {
        JsonReader jr = JsonReader.fromString("{\"x\": 123"); // malformed
        assertNull(jr.optInt("x"));
    }

    @Test
    public void testEmptyBodyAndInvalidTypes() {
        JsonReader jr = JsonReader.fromString("");
        assertNull(jr.optInt("x"));
        jr = JsonReader.fromString("{\"x\":{}} ");
        assertNull(jr.optInt("x"));
    }

    @Test
    public void testBooleanParsingAndDefaults() {
        JsonReader jr = JsonReader.fromString("{\"a\":\"true\",\"b\":\"1\",\"c\":\"yes\",\"d\":\"no\"}");
        assertTrue(jr.optBool("a"));
        assertTrue(jr.optBool("b"));
        assertTrue(jr.optBool("c"));
        assertNull(jr.optBool("d"));

        assertEquals(5, jr.optInt("missing", 5));
        assertTrue(jr.optBool("missingBool", true));
        assertEquals("def", jr.optString("missingStr", "def"));
        assertEquals(7, jr.optIntWithin("missingObj", "x", 7));
    }

    @Test
    public void testArrayRootAndAt() {
        JsonReader jr = JsonReader.fromString("[{\"x\":1},{\"x\":2}]");
        assertTrue(jr.isArrayRoot());
        assertEquals(2, jr.arraySize());
        assertEquals(Integer.valueOf(1), jr.at(0).optInt("x"));
    }

    @Test
    public void testOptArray() {
        JsonReader jr = JsonReader.fromString("{\"items\":[{\"x\":1},{\"x\":2}]}");
        assertFalse(jr.isArrayRoot());
        assertEquals(0, jr.arraySize());
        java.util.List<JsonReader> arr = jr.optArray("items");
        assertEquals(2, arr.size());
        assertEquals(Integer.valueOf(2), arr.get(1).optInt("x"));
    }
}


