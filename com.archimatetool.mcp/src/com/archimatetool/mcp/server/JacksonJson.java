package com.archimatetool.mcp.server;

import java.io.IOException;
import java.io.InputStream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Jackson ObjectMapper singleton with utility helpers.
 */
public final class JacksonJson {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private JacksonJson() {}

    public static ObjectMapper mapper() {
        return MAPPER;
    }

    public static byte[] writeBytes(Object value) throws JsonProcessingException {
        return MAPPER.writeValueAsBytes(value);
    }

    public static <T> T read(InputStream is, Class<T> type) throws IOException {
        return MAPPER.readValue(is, type);
    }

    public static JsonNode readTree(InputStream is) throws IOException {
        return MAPPER.readTree(is);
    }
}
