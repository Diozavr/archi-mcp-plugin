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

/** Item describing a relation to add to a view. */
public class AddRelationToViewItem {
    public final String relationId;
    public final String sourceObjectId;
    public final String targetObjectId;
    public final String policy;
    public final Boolean suppressWhenNested;

    public AddRelationToViewItem(String relationId, String sourceObjectId,
                                 String targetObjectId, String policy,
                                 Boolean suppressWhenNested) {
        this.relationId = relationId;
        this.sourceObjectId = sourceObjectId;
        this.targetObjectId = targetObjectId;
        this.policy = policy;
        this.suppressWhenNested = suppressWhenNested;
    }
}
