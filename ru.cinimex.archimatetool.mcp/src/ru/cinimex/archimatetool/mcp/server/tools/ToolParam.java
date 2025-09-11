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
package ru.cinimex.archimatetool.mcp.server.tools;

import java.util.HashMap;
import java.util.Map;

/**
 * Parameter metadata for MCP tools.
 */
public class ToolParam {
    private final String name;
    private final String type;
    private final boolean required;
    private final String description;
    private final Object defaultValue;

    public ToolParam(String name, String type, boolean required, String description, Object defaultValue) {
        this.name = name;
        this.type = type;
        this.required = required;
        this.description = description;
        this.defaultValue = defaultValue;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public boolean isRequired() {
        return required;
    }

    public String getDescription() {
        return description;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("type", type);
        m.put("required", required);
        if (description != null) {
            m.put("description", description);
        }
        if (defaultValue != null) {
            m.put("default", defaultValue);
        }
        return m;
    }
}
