package com.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import org.junit.Test;

import com.archimatetool.mcp.core.errors.*;
import com.archimatetool.mcp.http.ResponseUtil;

public class ResponseUtilTest {

    @Test
    public void mapsBadRequest() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/", null);
        ResponseUtil.handleCoreException(ex, new BadRequestException("bad"));
        assertEquals(400, ex.getResponseCode());
        assertEquals("{\"error\":\"bad\"}", ex.getResponseString());
    }

    @Test
    public void mapsNotFound() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/", null);
        ResponseUtil.handleCoreException(ex, new NotFoundException("missing"));
        assertEquals(404, ex.getResponseCode());
        assertEquals("{\"error\":\"missing\"}", ex.getResponseString());
    }

    @Test
    public void mapsConflict() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/", null);
        ResponseUtil.handleCoreException(ex, new ConflictException("model"));
        assertEquals(409, ex.getResponseCode());
        assertEquals("{\"error\":\"model\"}", ex.getResponseString());
    }

    @Test
    public void mapsUnprocessable() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/", null);
        ResponseUtil.handleCoreException(ex, new UnprocessableException("bad"));
        assertEquals(422, ex.getResponseCode());
        assertEquals("{\"error\":\"bad\"}", ex.getResponseString());
    }

    @Test
    public void mapsInternal() throws Exception {
        FakeHttpExchange ex = new FakeHttpExchange("GET", "/", null);
        ResponseUtil.handleCoreException(ex, new CoreException("x"));
        assertEquals(500, ex.getResponseCode());
        assertEquals("{\"error\":\"internal error\"}", ex.getResponseString());
    }
}
