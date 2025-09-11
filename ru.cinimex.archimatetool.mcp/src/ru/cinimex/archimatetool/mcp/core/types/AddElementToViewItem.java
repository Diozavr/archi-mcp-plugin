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

/** Item describing an element to add to a view. */
public class AddElementToViewItem {
    public final String elementId;
    public final String parentObjectId;
    public final Integer x;
    public final Integer y;
    public final Integer w;
    public final Integer h;
    public final Map<String,String> style;

    public AddElementToViewItem(String elementId, String parentObjectId,
                                Integer x, Integer y, Integer w, Integer h,
                                Map<String,String> style) {
        this.elementId = elementId;
        this.parentObjectId = parentObjectId;
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.style = style;
    }
}
