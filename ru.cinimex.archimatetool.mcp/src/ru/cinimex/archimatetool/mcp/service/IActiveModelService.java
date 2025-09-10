package ru.cinimex.archimatetool.mcp.service;

import com.archimatetool.model.IArchimateModel;

/**
 * Interface for active model service to enable testing.
 */
public interface IActiveModelService {
    
    /**
     * Get the currently active ArchiMate model.
     * @return active model or null if none available
     */
    IArchimateModel getActiveModel();
    
    /**
     * Find an object by ID in the given model.
     * @param model the model to search in
     * @param id the object ID
     * @return the found object or null
     */
    Object findById(IArchimateModel model, String id);
    
    /**
     * Save the currently active model.
     * @return true if saved successfully, false otherwise
     */
    boolean saveActiveModel();
}
