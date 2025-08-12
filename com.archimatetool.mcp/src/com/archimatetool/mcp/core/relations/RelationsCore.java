package com.archimatetool.mcp.core.relations;

import com.archimatetool.mcp.core.errors.BadRequestException;
import com.archimatetool.mcp.core.errors.ConflictException;
import com.archimatetool.mcp.core.errors.NotFoundException;
import com.archimatetool.mcp.core.types.CreateRelationCmd;
import com.archimatetool.mcp.core.types.DeleteRelationCmd;
import com.archimatetool.mcp.core.types.GetRelationQuery;
import com.archimatetool.mcp.core.types.UpdateRelationCmd;
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

    /** Create a relation. */
    public java.util.Map<String, Object> createRelation(CreateRelationCmd cmd) {
        Validators.requireNonEmpty(cmd.type, "type");
        Validators.requireNonEmpty(cmd.sourceId, "sourceId");
        Validators.requireNonEmpty(cmd.targetId, "targetId");
        var model = requireModel();
        Object so = ServiceRegistry.activeModel().findById(model, cmd.sourceId);
        Object to = ServiceRegistry.activeModel().findById(model, cmd.targetId);
        if (!(so instanceof IArchimateElement) || !(to instanceof IArchimateElement)) {
            throw new NotFoundException("source or target not found");
        }
        String camelType = StringCaseUtil.toCamelCase(cmd.type);
        try {
            var rel = ServiceRegistry.relations().createRelation(model, camelType, cmd.name != null ? cmd.name : "",
                    (IArchimateElement) so, (IArchimateElement) to, cmd.folderId);
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

    /** Update relation. */
    public java.util.Map<String, Object> updateRelation(UpdateRelationCmd cmd) {
        Validators.requireNonEmpty(cmd.id, "id");
        var model = requireModel();
        Object o = ServiceRegistry.activeModel().findById(model, cmd.id);
        if (!(o instanceof IArchimateRelationship)) {
            throw new NotFoundException("not found");
        }
        IArchimateRelationship r = (IArchimateRelationship) o;
        if (cmd.name != null) {
            final String n = cmd.name;
            org.eclipse.swt.widgets.Display.getDefault().syncExec(() -> r.setName(n));
        }
        return ModelApi.relationToDto(r);
    }

    /** Delete relation. */
    public void deleteRelation(DeleteRelationCmd cmd) {
        Validators.requireNonEmpty(cmd.id, "id");
        var model = requireModel();
        Object o = ServiceRegistry.activeModel().findById(model, cmd.id);
        if (!(o instanceof IArchimateRelationship)) {
            throw new NotFoundException("not found");
        }
        boolean ok = ServiceRegistry.relations().deleteRelation((IArchimateRelationship) o);
        if (!ok) {
            throw new BadRequestException("cannot delete");
        }
    }
}
