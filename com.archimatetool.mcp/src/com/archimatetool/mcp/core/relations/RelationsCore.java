package com.archimatetool.mcp.core.relations;

import com.archimatetool.mcp.core.errors.BadRequestException;
import com.archimatetool.mcp.core.errors.ConflictException;
import com.archimatetool.mcp.core.errors.NotFoundException;
import com.archimatetool.mcp.core.types.CreateRelationCmd;
import com.archimatetool.mcp.core.types.CreateRelationItem;
import com.archimatetool.mcp.core.types.CreateRelationsCmd;
import com.archimatetool.mcp.core.types.DeleteRelationCmd;
import com.archimatetool.mcp.core.types.DeleteRelationItem;
import com.archimatetool.mcp.core.types.DeleteRelationsCmd;
import com.archimatetool.mcp.core.types.GetRelationQuery;
import com.archimatetool.mcp.core.types.UpdateRelationCmd;
import com.archimatetool.mcp.core.types.UpdateRelationItem;
import com.archimatetool.mcp.core.types.UpdateRelationsCmd;
import com.archimatetool.mcp.core.validation.Validators;
import com.archimatetool.mcp.server.ModelApi;
import com.archimatetool.mcp.service.ServiceRegistry;
import com.archimatetool.mcp.util.StringCaseUtil;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateRelationship;

/** Core operations for relations. */
public class RelationsCore {

    private IArchimateModel requireModel() {
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) {
            throw new ConflictException("no active model");
        }
        return model;
    }

    /** Create a relation (legacy single-item). */
    public java.util.Map<String, Object> createRelation(CreateRelationCmd cmd) {
        var item = new CreateRelationItem(cmd.type, cmd.name, cmd.sourceId, cmd.targetId, cmd.folderId, null, null);
        return createRelation(item);
    }

    /** Create multiple relations. */
    public java.util.List<java.util.Map<String, Object>> createRelations(CreateRelationsCmd cmd) {
        Validators.requireNonNull(cmd.items, "items");
        Validators.require(!cmd.items.isEmpty(), "items required");
        java.util.List<java.util.Map<String, Object>> res = new java.util.ArrayList<>();
        for (CreateRelationItem item : cmd.items) {
            res.add(createRelation(item));
        }
        return res;
    }

    private java.util.Map<String, Object> createRelation(CreateRelationItem item) {
        Validators.requireNonEmpty(item.type, "type");
        Validators.requireNonEmpty(item.sourceId, "sourceId");
        Validators.requireNonEmpty(item.targetId, "targetId");
        var model = requireModel();
        Object so = ServiceRegistry.activeModel().findById(model, item.sourceId);
        Object to = ServiceRegistry.activeModel().findById(model, item.targetId);
        if (!(so instanceof IArchimateElement) || !(to instanceof IArchimateElement)) {
            throw new NotFoundException("source or target not found");
        }
        String camelType = StringCaseUtil.toCamelCase(item.type);
        try {
            var rel = ServiceRegistry.relations().createRelation(model, camelType, item.name != null ? item.name : "",
                    (IArchimateElement) so, (IArchimateElement) to, item.folderId);
            return ModelApi.relationToDto(rel);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    /** Get relation by id. */
    public java.util.Map<String, Object> getRelation(GetRelationQuery q) {
        Validators.requireNonEmpty(q.id, "id");
        var model = requireModel();
        Object o = ServiceRegistry.activeModel().findById(model, q.id);
        if (!(o instanceof IArchimateRelationship)) {
            throw new NotFoundException("not found");
        }
        return ModelApi.relationToDto((IArchimateRelationship) o);
    }

    /** Update relation (legacy single-item). */
    public java.util.Map<String, Object> updateRelation(UpdateRelationCmd cmd) {
        var item = new UpdateRelationItem(cmd.id, cmd.name, null, null, null);
        return updateRelation(item);
    }

    /** Update multiple relations. */
    public java.util.List<java.util.Map<String, Object>> updateRelations(UpdateRelationsCmd cmd) {
        Validators.requireNonNull(cmd.items, "items");
        Validators.require(!cmd.items.isEmpty(), "items required");
        java.util.List<java.util.Map<String, Object>> res = new java.util.ArrayList<>();
        for (UpdateRelationItem item : cmd.items) {
            res.add(updateRelation(item));
        }
        return res;
    }

    private java.util.Map<String, Object> updateRelation(UpdateRelationItem item) {
        Validators.requireNonEmpty(item.id, "id");
        var model = requireModel();
        Object o = ServiceRegistry.activeModel().findById(model, item.id);
        if (!(o instanceof IArchimateRelationship)) {
            throw new NotFoundException("not found");
        }
        IArchimateRelationship r = (IArchimateRelationship) o;
        if (item.name != null) {
            final String n = item.name;
            org.eclipse.swt.widgets.Display.getDefault().syncExec(() -> r.setName(n));
        }
        return ModelApi.relationToDto(r);
    }

    /** Delete relation (legacy single-item). */
    public void deleteRelation(DeleteRelationCmd cmd) {
        var item = new DeleteRelationItem(cmd.id);
        deleteRelation(item);
    }

    /** Delete multiple relations. */
    public java.util.Map<String, Object> deleteRelations(DeleteRelationsCmd cmd) {
        Validators.requireNonNull(cmd.items, "items");
        Validators.require(!cmd.items.isEmpty(), "items required");
        for (DeleteRelationItem item : cmd.items) {
            deleteRelation(item);
        }
        java.util.Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("total", cmd.items.size());
        resp.put("deleted", cmd.items.size());
        return resp;
    }

    private void deleteRelation(DeleteRelationItem item) {
        Validators.requireNonEmpty(item.id, "id");
        var model = requireModel();
        Object o = ServiceRegistry.activeModel().findById(model, item.id);
        if (!(o instanceof IArchimateRelationship)) {
            throw new NotFoundException("not found");
        }
        boolean ok = ServiceRegistry.relations().deleteRelation((IArchimateRelationship) o);
        if (!ok) {
            throw new BadRequestException("cannot delete");
        }
    }
}
