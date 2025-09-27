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
package ru.cinimex.archimatetool.mcp.util;

public final class StringCaseUtil {
    private StringCaseUtil() {}

    /**
     * Convert input to PascalCase for EMF compatibility.
     * Supports both kebab-case input ("business-actor" -> "BusinessActor") 
     * and already correct PascalCase ("BusinessActor" -> "BusinessActor").
     * This ensures compatibility with both jArchi API style and EMF naming.
     */
    public static String toCamelCase(String input) {
        if (input == null) return null;
        if (input.isEmpty()) return input;
        
        // If the input doesn't contain dashes, ensure it starts with uppercase (EMF format)
        if (!input.contains("-")) {
            return Character.toUpperCase(input.charAt(0)) + 
                   (input.length() > 1 ? input.substring(1) : "");
        }
        
        // Process kebab-case to PascalCase (jArchi -> EMF conversion)
        String[] parts = input.split("-");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0)));
            if (p.length() > 1) sb.append(p.substring(1).toLowerCase());
        }
        return sb.toString();
    }

    /**
     * Convert PascalCase to kebab-case for API consistency.
     * Examples: "BusinessActor" -> "business-actor", "AssignmentRelationship" -> "assignment-relationship"
     * Handles abbreviations like "HTMLParser" -> "html-parser", "XMLHttpRequest" -> "xml-http-request"
     */
    public static String toKebabCase(String input) {
        if (input == null || input.isEmpty()) return input;
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            
            if (Character.isUpperCase(c) && i > 0) {
                char prev = input.charAt(i - 1);
                
                // Add dash if:
                // 1. Previous character is lowercase (normal case: "wordWord" -> "word-word")
                // 2. This is the start of a new word after abbreviation (like "HTMLParser" -> "HTML-Parser")
                if (Character.isLowerCase(prev)) {
                    sb.append('-');
                } else if (i < input.length() - 1 && Character.isLowerCase(input.charAt(i + 1))) {
                    // This uppercase letter starts a new word (abbreviation ends here)
                    sb.append('-');
                }
            }
            sb.append(Character.toLowerCase(c));
        }
        return sb.toString();
    }
}


