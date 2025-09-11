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
package ru.cinimex.archimatetool.mcp.tests;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import ru.cinimex.archimatetool.mcp.Config;
import ru.cinimex.archimatetool.mcp.preferences.MCPPreferenceInitializer;
import ru.cinimex.archimatetool.mcp.preferences.MCPPreferences;

public class MCPPreferenceInitializerTest {

    @Before
    public void resetDefaults() throws Exception {
        MCPPreferences.clearDefaults();
    }

    @Test
    public void setsDefaultHostAndPort() throws Exception {
        new MCPPreferenceInitializer().initializeDefaultPreferences();
        assertEquals(Config.DEFAULT_HOST, MCPPreferences.getDefaultHost());
        assertEquals(Config.DEFAULT_PORT, MCPPreferences.getDefaultPort());
    }
}
