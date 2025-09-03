package com.archimatetool.mcp.tests;

import com.archimatetool.mcp.service.IActiveModelService;
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
