package com.archimatetool.mcp.core.elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.archimatetool.mcp.core.errors.BadRequestException;
import com.archimatetool.mcp.core.errors.ConflictException;
import com.archimatetool.mcp.core.errors.NotFoundException;
import com.archimatetool.mcp.core.types.CreateElementCmd;
import com.archimatetool.mcp.core.types.DeleteElementCmd;
import com.archimatetool.mcp.core.types.GetElementQuery;
import com.archimatetool.mcp.core.types.ListElementRelationsQuery;
import com.archimatetool.mcp.core.types.UpdateElementCmd;
import com.archimatetool.mcp.core.validation.Validators;
import com.archimatetool.mcp.server.ModelApi;
import com.archimatetool.mcp.service.ServiceRegistry;
import com.archimatetool.mcp.util.StringCaseUtil;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IFolder;

/** Core operations for elements. */
public class ElementsCore {

    private IArchimateModel requireModel() {
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) {
            throw new ConflictException("no active model");
        }
        return model;
    }

    /** Create a new element in the active model. */
    public Map<String, Object> createElement(CreateElementCmd cmd) {
        Validators.requireNonEmpty(cmd.type, "type");
        Validators.requireNonEmpty(cmd.name, "name");
        var model = requireModel();
        String camelType = StringCaseUtil.toCamelCase(cmd.type);
        try {
            var el = ServiceRegistry.elements().createElement(model, camelType, cmd.name, cmd.folderId);
            return ModelApi.elementToDto(el);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(ex.getMessage());
        }
    }

    /** Get element by id with optional relation expansion. */
    public Map<String, Object> getElement(GetElementQuery q) {
        Validators.requireNonEmpty(q.id, "id");
        var model = requireModel();
        Object o = ServiceRegistry.activeModel().findById(model, q.id);
        if (!(o instanceof IArchimateElement)) {
            throw new NotFoundException("not found");
        }
        IArchimateElement el = (IArchimateElement) o;
        Map<String, Object> dto = new HashMap<>(ModelApi.elementToDto(el));
        if (q.includeRelations) {
            List<Object> items = collectRelations(model, el, "both", q.includeElements);
            dto.put("relations", items);
        }
        return dto;
    }

    /** Update element name. */
    public Map<String, Object> updateElement(UpdateElementCmd cmd) {
        Validators.requireNonEmpty(cmd.id, "id");
        var model = requireModel();
        Object o = ServiceRegistry.activeModel().findById(model, cmd.id);
        if (!(o instanceof IArchimateElement)) {
            throw new NotFoundException("not found");
        }
        IArchimateElement el = (IArchimateElement) o;
        if (cmd.name != null) {
            final String n = cmd.name;
            org.eclipse.swt.widgets.Display.getDefault().syncExec(() -> el.setName(n));
        }
        return ModelApi.elementToDto(el);
    }

    /** Delete element by id. */
    public void deleteElement(DeleteElementCmd cmd) {
        Validators.requireNonEmpty(cmd.id, "id");
        var model = requireModel();
        Object o = ServiceRegistry.activeModel().findById(model, cmd.id);
        if (!(o instanceof IArchimateElement)) {
            throw new NotFoundException("not found");
        }
        boolean ok = ServiceRegistry.elements().deleteElement((IArchimateElement) o);
        if (!ok) {
            throw new BadRequestException("cannot delete");
        }
    }

    /** List relations for an element. */
    public Map<String, Object> listRelations(ListElementRelationsQuery q) {
        Validators.requireNonEmpty(q.id, "id");
        String dir = q.direction != null ? q.direction.toLowerCase() : "both";
        Validators.require(dir.equals("both") || dir.equals("in") || dir.equals("out"), "invalid direction");
        var model = requireModel();
        Object o = ServiceRegistry.activeModel().findById(model, q.id);
        if (!(o instanceof IArchimateElement)) {
            throw new NotFoundException("element not found");
        }
        IArchimateElement el = (IArchimateElement) o;
        List<Object> items = collectRelations(model, el, dir, q.includeElements);
        Map<String, Object> resp = new HashMap<>();
        resp.put("total", items.size());
        resp.put("items", items);
        return resp;
    }

    private List<Object> collectRelations(IArchimateModel model, IArchimateElement el, String direction, boolean includeElements) {
        List<Object> items = new ArrayList<>();
        for (Object f : model.getFolders()) {
            if (f instanceof IFolder folder) {
                for (Object e : folder.getElements()) {
                    if (e instanceof IArchimateRelationship r) {
                        boolean isOut = r.getSource() == el;
                        boolean isIn = r.getTarget() == el;
                        boolean match = "both".equals(direction) || ("out".equals(direction) && isOut)
                                || ("in".equals(direction) && isIn);
                        if (match && (isOut || isIn)) {
                            if (includeElements) {
                                Map<String, Object> m = new HashMap<>();
                                m.put("relation", ModelApi.relationToDto(r));
                                if (r.getSource() instanceof IArchimateElement) {
                                    m.put("source", ModelApi.elementToDto((IArchimateElement) r.getSource()));
                                }
                                if (r.getTarget() instanceof IArchimateElement) {
                                    m.put("target", ModelApi.elementToDto((IArchimateElement) r.getTarget()));
                                }
                                items.add(m);
                            } else {
                                items.add(ModelApi.relationToDto(r));
                            }
                        }
                    }
                }
            }
        }
        return items;
    }
}
