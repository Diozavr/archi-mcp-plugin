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

/** Parameters for rendering a view to an image. */
public class GetViewImageQuery {
    public final String viewId;
    public final String format; // png or svg
    public final Float scale;
    public final Integer dpi;
    public final String bg;
    public final Integer margin;

    public GetViewImageQuery(String viewId, String format, Float scale, Integer dpi, String bg, Integer margin) {
        this.viewId = viewId;
        this.format = format;
        this.scale = scale;
        this.dpi = dpi;
        this.bg = bg;
        this.margin = margin;
    }
}
