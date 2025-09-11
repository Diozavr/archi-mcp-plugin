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
package ru.cinimex.archimatetool.mcp.server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.archimatetool.editor.diagram.ArchimateDiagramModelFactory;
import com.archimatetool.editor.model.IArchiveManager;
import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.FolderType;
import com.archimatetool.model.IArchimateConcept;
import com.archimatetool.model.IArchimateDiagramModel;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateFactory;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateModelObject;
import com.archimatetool.model.IArchimatePackage;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IBounds;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IDiagramModelArchimateComponent;
import com.archimatetool.model.IDiagramModelArchimateObject;
import com.archimatetool.model.IDiagramModelArchimateConnection;
import com.archimatetool.model.IDiagramModelConnection;
import com.archimatetool.model.IDiagramModelContainer;
import com.archimatetool.model.IDiagramModelObject;
import com.archimatetool.model.IFolder;
import com.archimatetool.model.util.ArchimateModelUtils;

public class ModelApi {

    public static Object findById(IArchimateModel model, String id) {
        return ArchimateModelUtils.getObjectByID(model, id);
    }

    public static Map<String, Object> elementToDto(IArchimateElement e) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", e.getId());
        m.put("modelId", e.getArchimateModel().getId());
        m.put("type", e.eClass().getName());
        m.put("name", e.getName());
        // Include documentation by default so agents can understand purpose/usage
        try {
            String doc = e.getDocumentation();
            if (doc != null && !doc.isEmpty()) {
                m.put("documentation", doc);
            }
        } catch (Exception ex) {
            // older Archi versions may not expose documentation on all objects; ignore
        }
        IFolder folder = (IFolder) e.eContainer();
        m.put("folderId", folder != null ? folder.getId() : null);
        return m;
    }

    public static Map<String, Object> relationToDto(IArchimateRelationship r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("modelId", r.getArchimateModel().getId());
        m.put("type", r.eClass().getName());
        m.put("name", r.getName());
        m.put("sourceId", r.getSource() != null ? r.getSource().getId() : null);
        m.put("targetId", r.getTarget() != null ? r.getTarget().getId() : null);
        return m;
    }

    public static Map<String, Object> viewToDto(IDiagramModel v) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", v.getId());
        m.put("modelId", v.getArchimateModel().getId());
        m.put("type", v.eClass().getName());
        m.put("name", v.getName());
        if (v.eContainer() instanceof IFolder) {
            IFolder folder = (IFolder) v.eContainer();
            m.put("folderId", folder.getId());
            m.put("folderPath", buildFolderPath(folder));
        }
        return m;
    }

    public static Map<String, Object> viewObjectToDto(IDiagramModelObject obj) {
        Map<String, Object> m = new HashMap<>();
        m.put("objectId", obj.getId());
        m.put("type", obj.eClass().getName());
        // expose parentObjectId when parent is a diagram object
        Object parent = obj.eContainer();
        if (parent instanceof IDiagramModelObject) {
            m.put("parentObjectId", ((IDiagramModelObject) parent).getId());
        }
        if (obj instanceof IDiagramModelArchimateObject) {
            IDiagramModelArchimateObject ao = (IDiagramModelArchimateObject) obj;
            IArchimateConcept c = ao.getArchimateConcept();
            if (c instanceof IArchimateElement) {
                IArchimateElement e = (IArchimateElement) c;
                m.put("elementId", e.getId());
            }
            if (c instanceof IArchimateRelationship) {
                IArchimateRelationship r = (IArchimateRelationship) c;
                m.put("relationId", r.getId());
            }
        }
        IBounds b = obj.getBounds();
        if (b != null) {
            Map<String, Object> bounds = new HashMap<>();
            bounds.put("x", b.getX());
            bounds.put("y", b.getY());
            bounds.put("w", b.getWidth());
            bounds.put("h", b.getHeight());
            m.put("bounds", bounds);
        }
        return m;
    }

    public static Map<String, Object> viewContentToDto(IDiagramModel v) {
        Map<String, Object> content = new HashMap<>();
        List<Object> objects = new ArrayList<>();
        // collect all objects recursively to include nested ones
        List<IDiagramModelObject> allObjects = new ArrayList<>();
        collectAllObjects(v, allObjects);
        for (IDiagramModelObject dmo : allObjects) {
            objects.add(viewObjectToDto(dmo));
        }
        // collect connections attached to all objects
        List<Object> conns = new ArrayList<>();
        for (IDiagramModelObject obj : allObjects) {
            for (Object co : obj.getSourceConnections()) {
                if (co instanceof IDiagramModelConnection) {
                    IDiagramModelConnection c = (IDiagramModelConnection) co;
                    conns.add(connectionToDto(c));
                }
            }
        }
        content.put("objects", objects);
        content.put("connections", conns);
        return content;
    }

    public static boolean isAncestorOf(IDiagramModelObject maybeAncestor, IDiagramModelObject node) {
        if (maybeAncestor == null || node == null) return false;
        Object p = node.eContainer();
        while (p instanceof IDiagramModelObject) {
            if (p == maybeAncestor) return true;
            p = ((IDiagramModelObject) p).eContainer();
        }
        return false;
    }

    public static List<IDiagramModel> listViews(IArchimateModel model) {
        List<IDiagramModel> list = new ArrayList<>();
        IFolder diagrams = model.getFolder(FolderType.DIAGRAMS);
        if (diagrams != null) collectViewsRecursive(diagrams, list);
        return list;
    }

    private static void collectViewsRecursive(IFolder folder, List<IDiagramModel> out) {
        for (Object o : folder.getElements()) {
            if (o instanceof IDiagramModel) out.add((IDiagramModel) o);
        }
        for (Object f : folder.getFolders()) {
            if (f instanceof IFolder) collectViewsRecursive((IFolder) f, out);
        }
    }

    private static String buildFolderPath(IFolder folder) {
        // Build relative path under DIAGRAMS folder using folder names
        List<String> names = new ArrayList<String>();
        IFolder current = folder;
        IFolder diagramsRoot = folder.getArchimateModel().getFolder(FolderType.DIAGRAMS);
        while (current != null && current != diagramsRoot) {
            names.add(0, safeName(current.getName()));
            if (current.eContainer() instanceof IFolder) {
                current = (IFolder) current.eContainer();
            } else {
                break;
            }
        }
        // If current == diagramsRoot and it has a name, we usually omit it from relative path
        if (names.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append('/');
            sb.append(names.get(i));
        }
        return sb.toString();
    }

    private static String safeName(String s) {
        return s == null ? "" : s;
    }

    public static byte[] renderViewToPNG(IDiagramModel view, float scale, Integer dpi, java.awt.Color bg, int margin) {
        final byte[][] out = new byte[1][1];
        Display.getDefault().syncExec(() -> {
            org.eclipse.swt.graphics.Image image = null;
            try {
                int intScale = Math.max(1, Math.round(scale));
                // Direct use of Archi's DiagramUtils and ImageFactory as в scripting‑plugin
                image = com.archimatetool.editor.diagram.util.DiagramUtils.createImage(view, intScale, margin);
                org.eclipse.swt.graphics.ImageData data = image.getImageData(com.archimatetool.editor.ui.ImageFactory.getImageDeviceZoom());
                if (dpi != null && dpi > 0) {
                    try {
                        java.lang.reflect.Field fxdpi = data.getClass().getField("xdpi");
                        java.lang.reflect.Field fydpi = data.getClass().getField("ydpi");
                        fxdpi.setInt(data, dpi.intValue());
                        fydpi.setInt(data, dpi.intValue());
                    } catch (Throwable ignoreDpi) { /* ignore */ }
                }
                org.eclipse.swt.graphics.ImageLoader loader = new org.eclipse.swt.graphics.ImageLoader();
                loader.data = new org.eclipse.swt.graphics.ImageData[] { data };
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                loader.save(baos, org.eclipse.swt.SWT.IMAGE_PNG);
                out[0] = baos.toByteArray();
            } catch (Throwable t) {
                out[0] = new byte[0];
            } finally {
                if (image != null && !image.isDisposed()) image.dispose();
            }
        });
        return out[0];
    }

    public static byte[] renderViewToSVG(IDiagramModel view, float scale, String bg, int margin) {
        final byte[][] out = new byte[1][1];
        Display.getDefault().syncExec(() -> {
            try {
                // Archi 5 uses the export plugin for SVG output
                com.archimatetool.export.svg.SVGExportProvider exporter =
                        new com.archimatetool.export.svg.SVGExportProvider();
                String svg = exporter.getSVGString(view, true);
                if (svg == null) { out[0] = new byte[0]; return; }

                try {
                    javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
                    dbf.setNamespaceAware(true);
                    org.w3c.dom.Document doc = dbf.newDocumentBuilder().parse(
                            new org.xml.sax.InputSource(new java.io.StringReader(svg)));
                    org.w3c.dom.Element root = doc.getDocumentElement();

                    String viewBox = root.getAttribute("viewBox");
                    float vbX = 0, vbY = 0, vbW = 0, vbH = 0;
                    if (viewBox != null && !viewBox.isEmpty()) {
                        String[] parts = viewBox.trim().split("\\s+");
                        if (parts.length == 4) {
                            vbX = Float.parseFloat(parts[0]);
                            vbY = Float.parseFloat(parts[1]);
                            vbW = Float.parseFloat(parts[2]);
                            vbH = Float.parseFloat(parts[3]);
                        }
                    }
                    if (vbW == 0 || vbH == 0) {
                        vbW = Float.parseFloat(root.getAttribute("width"));
                        vbH = Float.parseFloat(root.getAttribute("height"));
                    }

                    // apply margin
                    vbX -= margin;
                    vbY -= margin;
                    vbW += margin * 2;
                    vbH += margin * 2;

                    // apply scale by adjusting width/height
                    root.setAttribute("viewBox", vbX + " " + vbY + " " + vbW + " " + vbH);
                    root.setAttribute("width", Float.toString(vbW * scale));
                    root.setAttribute("height", Float.toString(vbH * scale));

                    // background color if requested
                    if (bg != null && !"transparent".equalsIgnoreCase(bg)) {
                        root.setAttribute("style", "background-color:" + bg);
                    }

                    javax.xml.transform.Transformer tf = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
                    tf.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                    tf.transform(new javax.xml.transform.dom.DOMSource(doc),
                            new javax.xml.transform.stream.StreamResult(baos));
                    out[0] = baos.toByteArray();
                } catch (Exception e) {
                    // fallback to the original string
                    out[0] = svg.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                }
            } catch (Throwable t) {
                out[0] = new byte[0];
            }
        });
        return out[0];
    }


    public static IDiagramModelObject findDiagramObjectById(IDiagramModel view, String objectId) {
        if (objectId == null) return null;
        List<IDiagramModelObject> all = new ArrayList<>();
        collectAllObjects(view, all);
        for (IDiagramModelObject obj : all) {
            if (objectId.equals(obj.getId())) return obj;
        }
        return null;
    }

    public static List<IDiagramModelObject> findDiagramObjectsByElementId(IDiagramModel view, String elementId) {
        List<IDiagramModelObject> matches = new ArrayList<>();
        if (elementId == null) return matches;
        List<IDiagramModelObject> all = new ArrayList<>();
        collectAllObjects(view, all);
        for (IDiagramModelObject obj : all) {
            if (obj instanceof IDiagramModelArchimateObject) {
                IDiagramModelArchimateObject ao = (IDiagramModelArchimateObject) obj;
                IArchimateConcept c = ao.getArchimateConcept();
                if (c instanceof IArchimateElement) {
                    IArchimateElement e = (IArchimateElement) c;
                    if (elementId.equals(e.getId())) {
                        matches.add(obj);
                    }
                }
            }
        }
        return matches;
    }

    public static IDiagramModelArchimateConnection addRelationToView(IDiagramModel view, IArchimateRelationship relation,
                                                                     IDiagramModelObject sourceObject,
                                                                     IDiagramModelObject targetObject) {
        final IDiagramModelArchimateConnection[] result = new IDiagramModelArchimateConnection[1];
        Display.getDefault().syncExec(() -> {
            // Create proper archimate connection and attach to diagram via source object's connections
            IDiagramModelArchimateConnection conn = ArchimateDiagramModelFactory.createDiagramModelArchimateConnection(relation);
            conn.setSource(sourceObject);
            conn.setTarget(targetObject);
            // Ensure containment is established (some Archi versions require explicit add)
            if (!sourceObject.getSourceConnections().contains(conn)) {
                sourceObject.getSourceConnections().add(conn);
            }
            if (!targetObject.getTargetConnections().contains(conn)) {
                targetObject.getTargetConnections().add(conn);
            }
            result[0] = conn;
        });
        return result[0];
    }

    public static Map<String, Object> connectionToDto(IDiagramModelConnection c) {
        Map<String, Object> m = new HashMap<>();
        m.put("objectId", c.getId());
        if (c.getSource() != null) m.put("sourceObjectId", c.getSource().getId());
        if (c.getTarget() != null) m.put("targetObjectId", c.getTarget().getId());
        if (c instanceof IDiagramModelArchimateConnection) {
            IDiagramModelArchimateConnection ac = (IDiagramModelArchimateConnection) c;
            if (ac.getArchimateRelationship() != null) {
                m.put("relationId", ac.getArchimateRelationship().getId());
            }
        }
        return m;
    }

    // Moved to ElementService
    @Deprecated
    public static IArchimateElement createElement(IArchimateModel model, String camelCaseType, String name, String folderId) {
        return new ru.cinimex.archimatetool.mcp.service.ElementService().createElement(model, camelCaseType, name, folderId);
    }

    // Moved to RelationService
    @Deprecated
    public static IArchimateRelationship createRelation(IArchimateModel model, String camelCaseType, String name, IArchimateElement source, IArchimateElement target, String folderId) {
        return new ru.cinimex.archimatetool.mcp.service.RelationService().createRelation(model, camelCaseType, name, source, target, folderId);
    }

    // Moved to ViewService
    @Deprecated
    public static IDiagramModel createArchimateView(IArchimateModel model, String name) {
        return new ru.cinimex.archimatetool.mcp.service.ViewService().createArchimateView(model, name);
    }

    // Moved to ViewService
    @Deprecated
    public static IDiagramModelArchimateObject addElementToView(IDiagramModel view, IArchimateElement element, int x, int y, int w, int h) {
        return new ru.cinimex.archimatetool.mcp.service.ViewService().addElementToView(view, element, x, y, w, h);
    }

    // Moved to ElementService
    @Deprecated
    public static boolean deleteElement(IArchimateElement el) { return new ru.cinimex.archimatetool.mcp.service.ElementService().deleteElement(el); }

    // Moved to ViewService
    @Deprecated
    public static boolean deleteViewObject(IDiagramModelObject obj) { return new ru.cinimex.archimatetool.mcp.service.ViewService().deleteViewObject(obj); }

    // Moved to ViewService
    @Deprecated
    public static boolean deleteView(IDiagramModel view) { return new ru.cinimex.archimatetool.mcp.service.ViewService().deleteView(view); }

    // Moved to RelationService
    @Deprecated
    public static boolean deleteRelation(IArchimateRelationship rel) { return new ru.cinimex.archimatetool.mcp.service.RelationService().deleteRelation(rel); }

    private static void collectAllObjects(IDiagramModelContainer container, List<IDiagramModelObject> out) {
        for (Object o : container.getChildren()) {
            if (o instanceof IDiagramModelObject) {
                IDiagramModelObject d = (IDiagramModelObject) o;
                out.add(d);
                if (d instanceof IDiagramModelContainer) {
                    collectAllObjects((IDiagramModelContainer) d, out);
                }
            }
        }
    }

    public static int removeElementOccurrences(IArchimateModel model, IArchimateElement element) {
        int removed = 0;
        List<IDiagramModel> views = listViews(model);
        for (IDiagramModel v : views) {
            List<IDiagramModelObject> all = new ArrayList<>();
            collectAllObjects(v, all);
            for (IDiagramModelObject obj : all) {
                if (obj instanceof IDiagramModelArchimateObject) {
                    IDiagramModelArchimateObject ao = (IDiagramModelArchimateObject) obj;
                    IArchimateConcept c = ao.getArchimateConcept();
                    if (c == element) {
                        // remove connections first
                        List<Object> toDel = new ArrayList<>();
                        for (Object co : obj.getSourceConnections()) {
                            if (co instanceof IDiagramModelConnection) toDel.add(co);
                        }
                        for (Object co : obj.getTargetConnections()) {
                            if (co instanceof IDiagramModelConnection) toDel.add(co);
                        }
                        final List<Object> del = toDel;
                        Display.getDefault().syncExec(() -> {
                            for (Object dc : del) {
                                org.eclipse.emf.ecore.util.EcoreUtil.delete((org.eclipse.emf.ecore.EObject) dc);
                            }
                        });
                        // remove object
                        removed += deleteViewObject(obj) ? 1 : 0;
                    }
                }
            }
        }
        return removed;
    }

    public static int removeRelationOccurrences(IArchimateModel model, IArchimateRelationship relation) {
        int removed = 0;
        List<IDiagramModel> views = listViews(model);
        for (IDiagramModel v : views) {
            List<IDiagramModelObject> all = new ArrayList<>();
            collectAllObjects(v, all);
            // connections are attached to objects
            List<Object> toDel = new ArrayList<>();
            for (IDiagramModelObject obj : all) {
                for (Object co : obj.getSourceConnections()) {
                    if (co instanceof IDiagramModelArchimateConnection) {
                        IDiagramModelArchimateConnection ac = (IDiagramModelArchimateConnection) co;
                        if (ac.getArchimateRelationship() == relation) toDel.add(ac);
                    }
                }
                for (Object co : obj.getTargetConnections()) {
                    if (co instanceof IDiagramModelArchimateConnection) {
                        IDiagramModelArchimateConnection ac = (IDiagramModelArchimateConnection) co;
                        if (ac.getArchimateRelationship() == relation) toDel.add(ac);
                    }
                }
            }
            final List<Object> del = toDel;
            if (!del.isEmpty()) {
                Display.getDefault().syncExec(() -> {
                    for (Object dc : del) {
                        org.eclipse.emf.ecore.util.EcoreUtil.delete((org.eclipse.emf.ecore.EObject) dc);
                    }
                });
                removed += del.size();
            }
        }
        return removed;
    }

    // Moved to ActiveModelService
    @Deprecated
    public static boolean saveActiveModel() { return new ru.cinimex.archimatetool.mcp.service.ActiveModelService().saveActiveModel(); }

    // Moved to StringCaseUtil
    @Deprecated
    public static String toCamelCase(String kebab) { return ru.cinimex.archimatetool.mcp.util.StringCaseUtil.toCamelCase(kebab); }

    private static IFolder resolveFolderFor(IArchimateModel model, IArchimateModelObject object, String folderId) {
        IFolder folder = null;
        if (folderId != null) {
            Object o = ArchimateModelUtils.getObjectByID(model, folderId);
            if (o instanceof IFolder) {
                folder = (IFolder) o;
            }
        }
        if (folder == null) {
            folder = model.getDefaultFolderForObject(object);
        }
        return folder;
    }
}

