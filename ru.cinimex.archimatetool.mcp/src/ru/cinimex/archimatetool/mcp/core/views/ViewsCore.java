package ru.cinimex.archimatetool.mcp.core.views;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.cinimex.archimatetool.mcp.core.errors.BadRequestException;
import ru.cinimex.archimatetool.mcp.core.errors.ConflictException;
import ru.cinimex.archimatetool.mcp.core.errors.CoreException;
import ru.cinimex.archimatetool.mcp.core.errors.NotFoundException;
import ru.cinimex.archimatetool.mcp.core.errors.UnprocessableException;
import ru.cinimex.archimatetool.mcp.core.types.AddElementToViewItem;
import ru.cinimex.archimatetool.mcp.core.types.AddElementsToViewCmd;
import ru.cinimex.archimatetool.mcp.core.types.AddRelationToViewItem;
import ru.cinimex.archimatetool.mcp.core.types.AddRelationsToViewCmd;
import ru.cinimex.archimatetool.mcp.core.types.CreateViewCmd;
import ru.cinimex.archimatetool.mcp.core.types.DeleteViewCmd;
import ru.cinimex.archimatetool.mcp.core.types.GetViewContentQuery;
import ru.cinimex.archimatetool.mcp.core.types.GetViewImageQuery;
import ru.cinimex.archimatetool.mcp.core.types.GetViewQuery;
import ru.cinimex.archimatetool.mcp.core.types.DeleteViewObjectItem;
import ru.cinimex.archimatetool.mcp.core.types.DeleteViewObjectsCmd;
import ru.cinimex.archimatetool.mcp.core.types.MoveViewObjectItem;
import ru.cinimex.archimatetool.mcp.core.types.MoveViewObjectsCmd;
import ru.cinimex.archimatetool.mcp.core.types.UpdateViewObjectBoundsItem;
import ru.cinimex.archimatetool.mcp.core.types.UpdateViewObjectsBoundsCmd;
import ru.cinimex.archimatetool.mcp.core.validation.Validators;
import ru.cinimex.archimatetool.mcp.server.ModelApi;
import ru.cinimex.archimatetool.mcp.service.ServiceRegistry;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;

/** Core application layer for view operations. */
public class ViewsCore {
    /** List all views in the active model. */
    public List<Map<String, Object>> listViews() throws CoreException {
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        return ModelApi.listViews(model).stream().map(ModelApi::viewToDto).collect(Collectors.toList());
    }

    /** Create a new view. Currently only Archimate views are supported. */
    public Map<String, Object> createView(CreateViewCmd cmd) throws CoreException {
        Validators.requireNonEmpty(cmd.type, "type");
        Validators.requireNonEmpty(cmd.name, "name");
        if (!cmd.type.toLowerCase().contains("archimate")) {
            throw new UnprocessableException("only archimate view supported in MVP");
        }
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        IDiagramModel view = ServiceRegistry.views().createArchimateView(model, cmd.name);
        return ModelApi.viewToDto(view);
    }

    /** Get view by id. */
    public Map<String, Object> getView(GetViewQuery q) throws CoreException {
        Validators.requireNonEmpty(q.id, "id");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object obj = ServiceRegistry.activeModel().findById(model, q.id);
        if (!(obj instanceof IDiagramModel)) throw new NotFoundException("view not found");
        return ModelApi.viewToDto((IDiagramModel) obj);
    }

    /** Delete a view by id. */
    public void deleteView(DeleteViewCmd cmd) throws CoreException {
        Validators.requireNonEmpty(cmd.id, "id");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object obj = ServiceRegistry.activeModel().findById(model, cmd.id);
        if (!(obj instanceof IDiagramModel)) throw new NotFoundException("view not found");
        boolean ok = ServiceRegistry.views().deleteView((IDiagramModel) obj);
        if (!ok) throw new BadRequestException("cannot delete");
    }

    /** Get structured content of a view. */
    public Map<String, Object> getViewContent(GetViewContentQuery q) throws CoreException {
        Validators.requireNonEmpty(q.viewId, "viewId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object obj = ServiceRegistry.activeModel().findById(model, q.viewId);
        if (!(obj instanceof IDiagramModel)) throw new NotFoundException("view not found");
        return ModelApi.viewContentToDto((IDiagramModel) obj);
    }

    /** Add multiple elements to a view. */
    public List<Map<String, Object>> addElements(AddElementsToViewCmd cmd) throws CoreException {
        Validators.requireNonEmpty(cmd.viewId, "viewId");
        Validators.requireNonNull(cmd.items, "items");
        Validators.require(!cmd.items.isEmpty(), "items required");
        List<Map<String, Object>> res = new java.util.ArrayList<>();
        for (AddElementToViewItem item : cmd.items) {
            res.add(addElement(cmd.viewId, item));
        }
        return res;
    }

    private Map<String, Object> addElement(String viewId, AddElementToViewItem item) throws CoreException {
        Validators.requireNonEmpty(viewId, "viewId");
        Validators.requireNonEmpty(item.elementId, "elementId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object vo = ServiceRegistry.activeModel().findById(model, viewId);
        Object eo = ServiceRegistry.activeModel().findById(model, item.elementId);
        if (!(vo instanceof IDiagramModel) || !(eo instanceof IArchimateElement)) {
            throw new NotFoundException("view or element not found");
        }
        IDiagramModel view = (IDiagramModel) vo;
        IArchimateElement el = (IArchimateElement) eo;
        IDiagramModelObject parentObj = null;
        if (item.parentObjectId != null && !item.parentObjectId.isEmpty()) {
            parentObj = ModelApi.findDiagramObjectById(view, item.parentObjectId);
            if (parentObj == null) throw new NotFoundException("parentObjectId not found in view");
            if (!(parentObj instanceof IDiagramModelContainer)) {
                throw new BadRequestException("parent object is not a container");
            }
        }
        if (item.x != null) Validators.requireNonNegative(item.x, "x");
        if (item.y != null) Validators.requireNonNegative(item.y, "y");
        if (item.w != null) Validators.requireNonNegative(item.w, "w");
        if (item.h != null) Validators.requireNonNegative(item.h, "h");
        int x = item.x != null ? item.x.intValue() : 100;
        int y = item.y != null ? item.y.intValue() : 100;
        int w = item.w != null ? item.w.intValue() : 120;
        int h = item.h != null ? item.h.intValue() : 80;
        IDiagramModelArchimateObject dmo;
        if (parentObj instanceof IDiagramModelContainer) {
            dmo = ServiceRegistry.views().addElementToContainer((IDiagramModelContainer) parentObj, el, x, y, w, h);
        } else {
            dmo = ServiceRegistry.views().addElementToView(view, el, x, y, w, h);
        }
        
        // Apply styles if provided
        if (item.style != null && !item.style.isEmpty()) {
            for (Map.Entry<String, String> styleEntry : item.style.entrySet()) {
                String key = styleEntry.getKey();
                String value = styleEntry.getValue();
                try {
                    if ("fillColor".equals(key)) {
                        dmo.setFillColor(value);
                    } else if ("fontColor".equals(key)) {
                        dmo.setFontColor(value);
                    } else if ("lineColor".equals(key)) {
                        dmo.setLineColor(value);
                    } else if ("fontName".equals(key)) {
                        dmo.setFont(value);
                    } else if ("fontSize".equals(key)) {
                        // For fontSize, we need to get the current font and modify it
                        try {
                            int fontSize = Integer.parseInt(value);
                            String currentFont = dmo.getFont();
                            if (currentFont == null || currentFont.isEmpty()) {
                                // Use system default font with new size
                                dmo.setFont("1|Arial|" + fontSize + "|0|WINDOWS|1|-13|0|0|0|400|0|0|0|1|0|0|0|0|Arial");
                            } else {
                                // Parse existing font string and update size
                                String[] fontParts = currentFont.split("\\|");
                                if (fontParts.length > 2) {
                                    fontParts[2] = String.valueOf(fontSize);
                                    dmo.setFont(String.join("|", fontParts));
                                }
                            }
                        } catch (NumberFormatException e) {
                            // Ignore invalid font size
                        }
                    } else if ("opacity".equals(key) || "alpha".equals(key)) {
                        try {
                            int opacity = Integer.parseInt(value);
                            dmo.setAlpha(opacity);
                        } catch (NumberFormatException e) {
                            // Ignore invalid opacity
                        }
                    }
                } catch (Exception e) {
                    // Ignore any style application errors to prevent breaking the element creation
                }
            }
        }
        
        return Map.of("objectId", dmo.getId());
    }

    /** Add multiple relations to a view. */
    public List<Map<String, Object>> addRelations(AddRelationsToViewCmd cmd) throws CoreException {
        Validators.requireNonEmpty(cmd.viewId, "viewId");
        Validators.requireNonNull(cmd.items, "items");
        Validators.require(!cmd.items.isEmpty(), "items required");
        List<Map<String, Object>> res = new java.util.ArrayList<>();
        for (AddRelationToViewItem item : cmd.items) {
            res.add(addRelation(cmd.viewId, item));
        }
        return res;
    }

    private Map<String, Object> addRelation(String viewId, AddRelationToViewItem item) throws CoreException {
        Validators.requireNonEmpty(viewId, "viewId");
        Validators.requireNonEmpty(item.relationId, "relationId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object vo = ServiceRegistry.activeModel().findById(model, viewId);
        Object ro = ServiceRegistry.activeModel().findById(model, item.relationId);
        if (!(vo instanceof IDiagramModel) || !(ro instanceof com.archimatetool.model.IArchimateRelationship)) {
            throw new NotFoundException("view or relation not found");
        }
        IDiagramModel view = (IDiagramModel) vo;
        com.archimatetool.model.IArchimateRelationship rel = (com.archimatetool.model.IArchimateRelationship) ro;
        com.archimatetool.model.IDiagramModelObject so = null;
        com.archimatetool.model.IDiagramModelObject to = null;
        if (item.sourceObjectId != null && !item.sourceObjectId.isEmpty()) {
            so = ModelApi.findDiagramObjectById(view, item.sourceObjectId);
            if (so == null) throw new NotFoundException("sourceObjectId not found in view");
        }
        if (item.targetObjectId != null && !item.targetObjectId.isEmpty()) {
            to = ModelApi.findDiagramObjectById(view, item.targetObjectId);
            if (to == null) throw new NotFoundException("targetObjectId not found in view");
        }
        String policy = item.policy != null ? item.policy : "auto";
        if ((so == null || to == null)) {
            if (!"auto".equals(policy)) {
                throw new BadRequestException("sourceObjectId/targetObjectId required or use policy=auto");
            }
            String srcElId = rel.getSource() != null ? rel.getSource().getId() : null;
            String tgtElId = rel.getTarget() != null ? rel.getTarget().getId() : null;
            var srcObjs = ModelApi.findDiagramObjectsByElementId(view, srcElId);
            var tgtObjs = ModelApi.findDiagramObjectsByElementId(view, tgtElId);
            if (so == null) {
                if (srcObjs.size() == 1)
                    so = srcObjs.get(0);
                else
                    throw new ConflictException("ambiguous or missing source object on view");
            }
            if (to == null) {
                if (tgtObjs.size() == 1)
                    to = tgtObjs.get(0);
                else
                    throw new ConflictException("ambiguous or missing target object on view");
            }
        }
        if (Boolean.TRUE.equals(item.suppressWhenNested)) {
            if (ModelApi.isAncestorOf(so, to) || ModelApi.isAncestorOf(to, so)) {
                return Map.of("suppressed", true);
            }
        }
        var conn = ModelApi.addRelationToView(view, rel, so, to);
        return ModelApi.connectionToDto(conn);
    }

    /** Update bounds of multiple diagram objects. */
    public List<Map<String, Object>> updateBounds(UpdateViewObjectsBoundsCmd cmd) throws CoreException {
        Validators.requireNonEmpty(cmd.viewId, "viewId");
        Validators.requireNonNull(cmd.items, "items");
        Validators.require(!cmd.items.isEmpty(), "items required");
        List<Map<String, Object>> res = new java.util.ArrayList<>();
        for (UpdateViewObjectBoundsItem item : cmd.items) {
            res.add(updateBounds(cmd.viewId, item));
        }
        return res;
    }

    private Map<String, Object> updateBounds(String viewId, UpdateViewObjectBoundsItem item) throws CoreException {
        Validators.requireNonEmpty(viewId, "viewId");
        Validators.requireNonEmpty(item.objectId, "objectId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object vo = ServiceRegistry.activeModel().findById(model, viewId);
        if (!(vo instanceof IDiagramModel)) throw new NotFoundException("view not found");
        IDiagramModel view = (IDiagramModel) vo;
        com.archimatetool.model.IDiagramModelObject dmo = ModelApi.findDiagramObjectById(view, item.objectId);
        if (dmo == null) throw new NotFoundException("object not found");
        int x = item.x != null ? item.x : dmo.getBounds().getX();
        int y = item.y != null ? item.y : dmo.getBounds().getY();
        int w = item.w != null ? item.w : dmo.getBounds().getWidth();
        int h = item.h != null ? item.h : dmo.getBounds().getHeight();
        com.archimatetool.model.IBounds b = com.archimatetool.model.IArchimateFactory.eINSTANCE.createBounds(x, y, w, h);
        final com.archimatetool.model.IBounds fb = b;
        org.eclipse.swt.widgets.Display.getDefault().syncExec(() -> dmo.setBounds(fb));
        return ModelApi.viewObjectToDto(dmo);
    }

    /** Delete multiple diagram objects. */
    public Map<String, Object> deleteObjects(DeleteViewObjectsCmd cmd) throws CoreException {
        Validators.requireNonEmpty(cmd.viewId, "viewId");
        Validators.requireNonNull(cmd.items, "items");
        Validators.require(!cmd.items.isEmpty(), "items required");
        for (DeleteViewObjectItem item : cmd.items) {
            deleteObject(cmd.viewId, item);
        }
        Map<String, Object> resp = new java.util.HashMap<>();
        resp.put("total", cmd.items.size());
        resp.put("deleted", cmd.items.size());
        return resp;
    }

    private void deleteObject(String viewId, DeleteViewObjectItem item) throws CoreException {
        Validators.requireNonEmpty(viewId, "viewId");
        Validators.requireNonEmpty(item.objectId, "objectId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object vo = ServiceRegistry.activeModel().findById(model, viewId);
        if (!(vo instanceof IDiagramModel)) throw new NotFoundException("view not found");
        IDiagramModel view = (IDiagramModel) vo;
        com.archimatetool.model.IDiagramModelObject dmo = ModelApi.findDiagramObjectById(view, item.objectId);
        if (dmo == null) throw new NotFoundException("object not found");
        boolean ok = ServiceRegistry.views().deleteViewObject(dmo);
        if (!ok) throw new BadRequestException("cannot remove object");
    }

    /** Move multiple diagram objects. */
    public List<Map<String, Object>> moveObjects(MoveViewObjectsCmd cmd) throws CoreException {
        Validators.requireNonEmpty(cmd.viewId, "viewId");
        Validators.requireNonNull(cmd.items, "items");
        Validators.require(!cmd.items.isEmpty(), "items required");
        List<Map<String, Object>> res = new java.util.ArrayList<>();
        for (MoveViewObjectItem item : cmd.items) {
            res.add(moveObject(cmd.viewId, item));
        }
        return res;
    }

    private Map<String, Object> moveObject(String viewId, MoveViewObjectItem item) throws CoreException {
        Validators.requireNonEmpty(viewId, "viewId");
        Validators.requireNonEmpty(item.objectId, "objectId");
        Validators.requireNonEmpty(item.parentObjectId, "parentObjectId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object vo = ServiceRegistry.activeModel().findById(model, viewId);
        if (!(vo instanceof IDiagramModel)) throw new NotFoundException("view not found");
        IDiagramModel view = (IDiagramModel) vo;
        com.archimatetool.model.IDiagramModelObject dmo = ModelApi.findDiagramObjectById(view, item.objectId);
        if (dmo == null) throw new NotFoundException("object not found");
        com.archimatetool.model.IDiagramModelObject parentObj = null;
        com.archimatetool.model.IDiagramModelContainer targetContainer = null;
        if ("0".equals(item.parentObjectId)) {
            targetContainer = view;
        } else {
            parentObj = ModelApi.findDiagramObjectById(view, item.parentObjectId);
            if (parentObj == null) throw new NotFoundException("parentObjectId not found in view");
            if (!(parentObj instanceof com.archimatetool.model.IDiagramModelContainer)) {
                throw new BadRequestException("parent object is not a container");
            }
            targetContainer = (com.archimatetool.model.IDiagramModelContainer) parentObj;
        }
        if (parentObj != null && ModelApi.isAncestorOf(dmo, parentObj)) {
            throw new BadRequestException("cannot move into own descendant");
        }
        Integer bx = item.x;
        Integer by = item.y;
        Integer bw = item.w;
        Integer bh = item.h;
        var moved = ServiceRegistry.views().moveObjectToContainer(dmo, targetContainer, bx, by, bw, bh);
        if (!Boolean.TRUE.equals(item.keepExistingConnection) && parentObj != null) {
            if (dmo instanceof com.archimatetool.model.IDiagramModelArchimateObject && parentObj instanceof com.archimatetool.model.IDiagramModelArchimateObject) {
                var childEl = ((com.archimatetool.model.IDiagramModelArchimateObject) dmo).getArchimateConcept();
                var parentEl = ((com.archimatetool.model.IDiagramModelArchimateObject) parentObj).getArchimateConcept();
                if (childEl instanceof com.archimatetool.model.IArchimateElement && parentEl instanceof com.archimatetool.model.IArchimateElement) {
                    java.util.List<Object> toDel = new java.util.ArrayList<>();
                    for (Object co : dmo.getSourceConnections()) {
                        if (co instanceof com.archimatetool.model.IDiagramModelArchimateConnection ac) {
                            if (ac.getSource() == dmo && ac.getTarget() == parentObj) toDel.add(ac);
                        }
                    }
                    for (Object co : dmo.getTargetConnections()) {
                        if (co instanceof com.archimatetool.model.IDiagramModelArchimateConnection ac) {
                            if (ac.getTarget() == dmo && ac.getSource() == parentObj) toDel.add(ac);
                        }
                    }
                    if (!toDel.isEmpty()) {
                        final java.util.List<Object> dels = toDel;
                        org.eclipse.swt.widgets.Display.getDefault().syncExec(() -> {
                            for (Object dc : dels) {
                                org.eclipse.emf.ecore.util.EcoreUtil.delete((org.eclipse.emf.ecore.EObject) dc);
                            }
                        });
                    }
                }
            }
        }
        return ModelApi.viewObjectToDto(moved);
    }

    /** Render a view to an image (PNG or SVG). */
    public ImageData getViewImage(GetViewImageQuery q) throws CoreException {
        Validators.requireNonEmpty(q.viewId, "viewId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object vo = ServiceRegistry.activeModel().findById(model, q.viewId);
        if (!(vo instanceof IDiagramModel)) throw new NotFoundException("view not found");
        IDiagramModel view = (IDiagramModel) vo;
        String format = q.format != null ? q.format : "png";
        float scale = q.scale != null ? q.scale.floatValue() : 1.0f;
        int margin = q.margin != null ? q.margin.intValue() : 0;
        String bg = q.bg != null ? q.bg : "transparent";
        if ("svg".equalsIgnoreCase(format)) {
            byte[] svg = ModelApi.renderViewToSVG(view, scale, bg, margin);
            if (svg == null || svg.length == 0) throw new BadRequestException("render failed");
            return new ImageData(svg, "image/svg+xml; charset=utf-8");
        }
        Color bgc = null;
        if (bg != null && !"transparent".equalsIgnoreCase(bg)) {
            try { if (bg.startsWith("%23")) bg = bg.replace("%23", "#"); } catch (Exception ignore) {}
            if (bg.startsWith("#") && bg.length() == 7) {
                int r = Integer.parseInt(bg.substring(1,3),16);
                int g = Integer.parseInt(bg.substring(3,5),16);
                int b = Integer.parseInt(bg.substring(5,7),16);
                bgc = new Color(r,g,b);
            }
        }
        byte[] png = ModelApi.renderViewToPNG(view, scale, q.dpi, bgc, margin);
        if (png == null || png.length == 0) throw new BadRequestException("render failed");
        return new ImageData(png, "image/png");
    }

    /** Simple wrapper for image bytes and content type. */
    public static class ImageData {
        public final byte[] data;
        public final String contentType;
        public ImageData(byte[] data, String contentType) {
            this.data = data;
            this.contentType = contentType;
        }
    }
}
