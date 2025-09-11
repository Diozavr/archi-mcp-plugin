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
package ru.cinimex.archimatetool.mcp.core.types;

import java.util.Map;

/** Item describing a new relation to create. */
public class CreateRelationItem {
    public final String type;
    public final String name;
    public final String sourceId;
    public final String targetId;
    public final String folderId;
    public final Map<String,String> properties;
    public final String documentation;

    public CreateRelationItem(String type, String name, String sourceId,
                              String targetId, String folderId,
                              Map<String,String> properties,
                              String documentation) {
        this.type = type;
        this.name = name;
        this.sourceId = sourceId;
        this.targetId = targetId;
        this.folderId = folderId;
        this.properties = properties;
        this.documentation = documentation;
    }
}
