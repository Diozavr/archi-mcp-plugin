package com.archimatetool.mcp.core.views;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.archimatetool.mcp.core.errors.BadRequestException;
import com.archimatetool.mcp.core.errors.ConflictException;
import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.errors.NotFoundException;
import com.archimatetool.mcp.core.errors.UnprocessableException;
import com.archimatetool.mcp.core.types.AddElementToViewCmd;
import com.archimatetool.mcp.core.types.AddRelationToViewCmd;
import com.archimatetool.mcp.core.types.CreateViewCmd;
import com.archimatetool.mcp.core.types.DeleteViewCmd;
import com.archimatetool.mcp.core.types.GetViewContentQuery;
import com.archimatetool.mcp.core.types.GetViewImageQuery;
import com.archimatetool.mcp.core.types.GetViewQuery;
import com.archimatetool.mcp.core.types.DeleteViewObjectCmd;
import com.archimatetool.mcp.core.types.MoveViewObjectCmd;
import com.archimatetool.mcp.core.types.UpdateViewObjectBoundsCmd;
import com.archimatetool.mcp.core.validation.Validators;
import com.archimatetool.mcp.server.ModelApi;
import com.archimatetool.mcp.service.ServiceRegistry;
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

    /** Add element to a view or container within the view. */
    public Map<String, Object> addElement(AddElementToViewCmd cmd) throws CoreException {
        Validators.requireNonEmpty(cmd.viewId, "viewId");
        Validators.requireNonEmpty(cmd.elementId, "elementId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object vo = ServiceRegistry.activeModel().findById(model, cmd.viewId);
        Object eo = ServiceRegistry.activeModel().findById(model, cmd.elementId);
        if (!(vo instanceof IDiagramModel) || !(eo instanceof IArchimateElement)) {
            throw new NotFoundException("view or element not found");
        }
        IDiagramModel view = (IDiagramModel) vo;
        IArchimateElement el = (IArchimateElement) eo;
        IDiagramModelObject parentObj = null;
        if (cmd.parentObjectId != null && !cmd.parentObjectId.isEmpty()) {
            parentObj = ModelApi.findDiagramObjectById(view, cmd.parentObjectId);
            if (parentObj == null) throw new NotFoundException("parentObjectId not found in view");
            if (!(parentObj instanceof IDiagramModelContainer)) {
                throw new BadRequestException("parent object is not a container");
            }
        }
        if (cmd.x != null) Validators.requireNonNegative(cmd.x, "x");
        if (cmd.y != null) Validators.requireNonNegative(cmd.y, "y");
        if (cmd.w != null) Validators.requireNonNegative(cmd.w, "w");
        if (cmd.h != null) Validators.requireNonNegative(cmd.h, "h");
        int x = cmd.x != null ? cmd.x.intValue() : 100;
        int y = cmd.y != null ? cmd.y.intValue() : 100;
        int w = cmd.w != null ? cmd.w.intValue() : 120;
        int h = cmd.h != null ? cmd.h.intValue() : 80;
        IDiagramModelArchimateObject dmo;
        if (parentObj instanceof IDiagramModelContainer) {
            dmo = ServiceRegistry.views().addElementToContainer((IDiagramModelContainer) parentObj, el, x, y, w, h);
        } else {
            dmo = ServiceRegistry.views().addElementToView(view, el, x, y, w, h);
        }
        return Map.of("objectId", dmo.getId());
    }

    /** Add relation to a view. */
    public Map<String, Object> addRelation(AddRelationToViewCmd cmd) throws CoreException {
        Validators.requireNonEmpty(cmd.viewId, "viewId");
        Validators.requireNonEmpty(cmd.relationId, "relationId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object vo = ServiceRegistry.activeModel().findById(model, cmd.viewId);
        Object ro = ServiceRegistry.activeModel().findById(model, cmd.relationId);
        if (!(vo instanceof IDiagramModel) || !(ro instanceof com.archimatetool.model.IArchimateRelationship)) {
            throw new NotFoundException("view or relation not found");
        }
        IDiagramModel view = (IDiagramModel) vo;
        com.archimatetool.model.IArchimateRelationship rel = (com.archimatetool.model.IArchimateRelationship) ro;
        com.archimatetool.model.IDiagramModelObject so = null;
        com.archimatetool.model.IDiagramModelObject to = null;
        if (cmd.sourceObjectId != null && !cmd.sourceObjectId.isEmpty()) {
            so = ModelApi.findDiagramObjectById(view, cmd.sourceObjectId);
            if (so == null) throw new NotFoundException("sourceObjectId not found in view");
        }
        if (cmd.targetObjectId != null && !cmd.targetObjectId.isEmpty()) {
            to = ModelApi.findDiagramObjectById(view, cmd.targetObjectId);
            if (to == null) throw new NotFoundException("targetObjectId not found in view");
        }
        String policy = cmd.policy != null ? cmd.policy : "auto";
        if ((so == null || to == null)) {
            if (!"auto".equals(policy)) {
                throw new BadRequestException("sourceObjectId/targetObjectId required or use policy=auto");
            }
            String srcElId = rel.getSource() != null ? rel.getSource().getId() : null;
            String tgtElId = rel.getTarget() != null ? rel.getTarget().getId() : null;
            var srcObjs = ModelApi.findDiagramObjectsByElementId(view, srcElId);
            var tgtObjs = ModelApi.findDiagramObjectsByElementId(view, tgtElId);
            if (so == null) {
                if (srcObjs.size() == 1) so = srcObjs.get(0); else throw new ConflictException("ambiguous or missing source object on view");
            }
            if (to == null) {
                if (tgtObjs.size() == 1) to = tgtObjs.get(0); else throw new ConflictException("ambiguous or missing target object on view");
            }
        }
        if (Boolean.TRUE.equals(cmd.suppressWhenNested)) {
            if (ModelApi.isAncestorOf(so, to) || ModelApi.isAncestorOf(to, so)) {
                return Map.of("suppressed", true);
            }
        }
        var conn = ModelApi.addRelationToView(view, rel, so, to);
        return ModelApi.connectionToDto(conn);
    }

    /** Update bounds of a diagram object. */
    public Map<String, Object> updateBounds(UpdateViewObjectBoundsCmd cmd) throws CoreException {
        Validators.requireNonEmpty(cmd.viewId, "viewId");
        Validators.requireNonEmpty(cmd.objectId, "objectId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object vo = ServiceRegistry.activeModel().findById(model, cmd.viewId);
        if (!(vo instanceof IDiagramModel)) throw new NotFoundException("view not found");
        IDiagramModel view = (IDiagramModel) vo;
        com.archimatetool.model.IDiagramModelObject dmo = ModelApi.findDiagramObjectById(view, cmd.objectId);
        if (dmo == null) throw new NotFoundException("object not found");
        int x = cmd.x != null ? cmd.x : dmo.getBounds().getX();
        int y = cmd.y != null ? cmd.y : dmo.getBounds().getY();
        int w = cmd.w != null ? cmd.w : dmo.getBounds().getWidth();
        int h = cmd.h != null ? cmd.h : dmo.getBounds().getHeight();
        com.archimatetool.model.IBounds b = com.archimatetool.model.IArchimateFactory.eINSTANCE.createBounds(x, y, w, h);
        final com.archimatetool.model.IBounds fb = b;
        org.eclipse.swt.widgets.Display.getDefault().syncExec(() -> dmo.setBounds(fb));
        return ModelApi.viewObjectToDto(dmo);
    }

    /** Delete a diagram object from a view. */
    public void deleteObject(DeleteViewObjectCmd cmd) throws CoreException {
        Validators.requireNonEmpty(cmd.viewId, "viewId");
        Validators.requireNonEmpty(cmd.objectId, "objectId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object vo = ServiceRegistry.activeModel().findById(model, cmd.viewId);
        if (!(vo instanceof IDiagramModel)) throw new NotFoundException("view not found");
        IDiagramModel view = (IDiagramModel) vo;
        com.archimatetool.model.IDiagramModelObject dmo = ModelApi.findDiagramObjectById(view, cmd.objectId);
        if (dmo == null) throw new NotFoundException("object not found");
        boolean ok = ServiceRegistry.views().deleteViewObject(dmo);
        if (!ok) throw new BadRequestException("cannot remove object");
    }

    /** Move a diagram object to a new container. */
    public Map<String, Object> moveObject(MoveViewObjectCmd cmd) throws CoreException {
        Validators.requireNonEmpty(cmd.viewId, "viewId");
        Validators.requireNonEmpty(cmd.objectId, "objectId");
        Validators.requireNonEmpty(cmd.parentObjectId, "parentObjectId");
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) throw new ConflictException("no active model");
        Object vo = ServiceRegistry.activeModel().findById(model, cmd.viewId);
        if (!(vo instanceof IDiagramModel)) throw new NotFoundException("view not found");
        IDiagramModel view = (IDiagramModel) vo;
        com.archimatetool.model.IDiagramModelObject dmo = ModelApi.findDiagramObjectById(view, cmd.objectId);
        if (dmo == null) throw new NotFoundException("object not found");
        com.archimatetool.model.IDiagramModelObject parentObj = null;
        com.archimatetool.model.IDiagramModelContainer targetContainer = null;
        if ("0".equals(cmd.parentObjectId)) {
            targetContainer = view;
        } else {
            parentObj = ModelApi.findDiagramObjectById(view, cmd.parentObjectId);
            if (parentObj == null) throw new NotFoundException("parentObjectId not found in view");
            if (!(parentObj instanceof com.archimatetool.model.IDiagramModelContainer)) {
                throw new BadRequestException("parent object is not a container");
            }
            targetContainer = (com.archimatetool.model.IDiagramModelContainer) parentObj;
        }
        if (parentObj != null && ModelApi.isAncestorOf(dmo, parentObj)) {
            throw new BadRequestException("cannot move into own descendant");
        }
        Integer bx = cmd.x;
        Integer by = cmd.y;
        Integer bw = cmd.w;
        Integer bh = cmd.h;
        var moved = ServiceRegistry.views().moveObjectToContainer(dmo, targetContainer, bx, by, bw, bh);
        if (!Boolean.TRUE.equals(cmd.keepExistingConnection) && parentObj != null) {
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
