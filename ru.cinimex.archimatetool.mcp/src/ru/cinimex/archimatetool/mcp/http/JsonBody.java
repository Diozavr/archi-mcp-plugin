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
package ru.cinimex.archimatetool.mcp.http;

/**
 * Temporary minimal JSON accessors matching the previous regex-based behavior.
 * Will be replaced by JsonReader in the next step.
 */
public final class JsonBody {
    private JsonBody() {}

    public static String extractJsonString(String json, String key) {
        if (json == null || key == null) return null;
        String pattern = "\\\"" + key + "\\\"\\s*:\\s*\\\"";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(pattern);
        java.util.regex.Matcher m = p.matcher(json);
        if (m.find()) {
            int start = m.end();
            int end = json.indexOf('"', start);
            if (end > start) {
                return json.substring(start, end).replace("\\\"", "\"").replace("\\n", "\n");
            }
        }
        return null;
    }

    public static Integer extractJsonInt(String json, String key) {
        String s = extractJsonString(json, key);
        if (s != null) {
            try { return Integer.valueOf(s); } catch (Exception e) { /* fallthrough */ }
        }
        String pattern = "\\\"" + key + "\\\"\\s*:\\s*([0-9]+)";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(pattern).matcher(json);
        if (m.find()) {
            return Integer.valueOf(m.group(1));
        }
        return null;
    }

    public static Integer extractJsonIntWithin(String json, String objectName, String key) {
        if (json == null) return null;
        String objPattern = "\\\"" + objectName + "\\\"\\s*:\\s*\\{([\\n\\r\\t \\S]*?)\\}";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(objPattern).matcher(json);
        if (m.find()) {
            String block = m.group(1);
            String pattern = "\\\"" + key + "\\\"\\s*:\\s*([0-9]+)";
            java.util.regex.Matcher m2 = java.util.regex.Pattern.compile(pattern).matcher(block);
            if (m2.find()) {
                return Integer.valueOf(m2.group(1));
            }
        }
        return null;
    }
}


