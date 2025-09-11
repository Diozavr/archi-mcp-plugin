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

/** Query for retrieving an element. */
public class GetElementQuery {
    public final String id;
    public final boolean includeRelations;
    public final boolean includeElements;

    public GetElementQuery(String id, boolean includeRelations, boolean includeElements) {
        this.id = id;
        this.includeRelations = includeRelations;
        this.includeElements = includeElements;
    }
}
