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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import ru.cinimex.archimatetool.mcp.Config;
import ru.cinimex.archimatetool.mcp.preferences.MCPPreferences;

public class MCPPreferencesTest {

    @Before
    public void reset() throws Exception {
        MCPPreferences.clear();
    }

    @After
    public void cleanup() throws Exception {
        MCPPreferences.clear();
    }

    @Test
    public void defaultHost() {
        assertEquals(Config.DEFAULT_HOST, MCPPreferences.getHost());
    }

    @Test
    public void setAndGetHost() throws Exception {
        MCPPreferences.setHost("192.168.1.100");
        assertEquals("192.168.1.100", MCPPreferences.getHost());
    }

    @Test
    public void setHostToLocalhost() throws Exception {
        MCPPreferences.setHost("localhost");
        assertEquals("localhost", MCPPreferences.getHost());
    }

    @Test
    public void setHostToZeroZeroZeroZero() throws Exception {
        MCPPreferences.setHost("0.0.0.0");
        assertEquals("0.0.0.0", MCPPreferences.getHost());
    }

    @Test
    public void clearResetsHostToDefault() throws Exception {
        MCPPreferences.setHost("192.168.1.100");
        MCPPreferences.clear();
        assertEquals(Config.DEFAULT_HOST, MCPPreferences.getHost());
    }

    @Test
    public void setHostWithEmptyString() throws Exception {
        MCPPreferences.setHost("");
        assertEquals("", MCPPreferences.getHost());
    }

    @Test
    public void setHostWithNull() throws Exception {
        MCPPreferences.setHost(null);
        assertEquals(null, MCPPreferences.getHost());
    }

    // Test that host preferences work independently of port preferences
    @Test
    public void hostAndPortIndependent() throws Exception {
        MCPPreferences.setHost("192.168.1.100");
        MCPPreferences.setPort(9001);
        
        assertEquals("192.168.1.100", MCPPreferences.getHost());
        assertEquals(9001, MCPPreferences.getPort());
        
        MCPPreferences.setHost("0.0.0.0");
        assertEquals("0.0.0.0", MCPPreferences.getHost());
        assertEquals(9001, MCPPreferences.getPort()); // Port unchanged
    }
}

