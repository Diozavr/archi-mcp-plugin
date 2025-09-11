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

import ru.cinimex.archimatetool.mcp.service.IActiveModelService;
import com.archimatetool.model.IArchimateModel;

/**
 * Mock implementation of IActiveModelService for unit tests.
 * This avoids Eclipse workbench dependencies in tests.
 * 
 * For unit tests that don't require actual model functionality,
 * this returns null to simulate "no active model" condition,
 * which should be handled gracefully by the code.
 */
public class MockActiveModelService implements IActiveModelService {
    
    @Override
    public IArchimateModel getActiveModel() {
        // Return null to simulate no active model condition
        // This tests the error handling path
        return null;
    }
    
    @Override
    public Object findById(IArchimateModel model, String id) {
        // Return null for mock implementation
        return null;
    }
    
    @Override
    public boolean saveActiveModel() {
        // Return false to simulate no model to save
        return false;
    }
}
