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
package ru.cinimex.archimatetool.mcp.server;

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
