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
package ru.cinimex.archimatetool.mcp.core.model;

import java.util.HashMap;
import java.util.Map;

import ru.cinimex.archimatetool.mcp.core.errors.ConflictException;
import ru.cinimex.archimatetool.mcp.service.ServiceRegistry;
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
