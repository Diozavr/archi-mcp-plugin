package com.archimatetool.mcp.core.model;

import java.util.HashMap;
import java.util.Map;

import com.archimatetool.mcp.core.errors.ConflictException;
import com.archimatetool.mcp.service.ServiceRegistry;
import com.archimatetool.model.IArchimateModel;

/** Core operations for the model. */
public class ModelCore {

    private IArchimateModel requireModel() {
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) {
            throw new ConflictException("no active model");
        }
        return model;
    }

    /** Save the active model to disk. */
    public Map<String,Object> saveModel() {
        var model = requireModel();
        boolean ok = ServiceRegistry.activeModel().saveActiveModel();
        Map<String,Object> resp = new HashMap<>();
        resp.put("saved", ok);
        resp.put("modelId", model.getId());
        if (model.getFile() != null) {
            resp.put("path", model.getFile().getAbsolutePath());
        }
        return resp;
    }
}
