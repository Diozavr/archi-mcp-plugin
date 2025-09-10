package ru.cinimex.archimatetool.mcp.server.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import ru.cinimex.archimatetool.mcp.core.elements.ElementsCore;
import ru.cinimex.archimatetool.mcp.core.errors.BadRequestException;
import ru.cinimex.archimatetool.mcp.core.folders.FoldersCore;
import ru.cinimex.archimatetool.mcp.core.model.ModelCore;
import ru.cinimex.archimatetool.mcp.core.relations.RelationsCore;
import ru.cinimex.archimatetool.mcp.core.search.SearchCore;
import ru.cinimex.archimatetool.mcp.core.types.*;
import ru.cinimex.archimatetool.mcp.core.views.ViewsCore;

/**
 * Registry of MCP tools and their metadata.
 */
public class ToolRegistry {
    private static final Map<String, Tool> TOOLS = new LinkedHashMap<>();
    private static final ElementsCore elementsCore = new ElementsCore();
    private static final RelationsCore relationsCore = new RelationsCore();
    private static final ViewsCore viewsCore = new ViewsCore();
    private static final FoldersCore foldersCore = new FoldersCore();
    private static final SearchCore searchCore = new SearchCore();
    private static final ModelCore modelCore = new ModelCore();

    static {
        // status
        register(new Tool(
            "status",
            "Service status",
            Collections.emptyList(),
            params -> Map.of("ok", Boolean.TRUE, "service", "Archi MCP")
        ));
        // list_views
        register(new Tool(
            "list_views",
            "List views",
            Collections.emptyList(),
            params -> viewsCore.listViews()
        ));
        // create_view
        register(new Tool(
            "create_view",
            "Create view",
            Arrays.asList(
                new ToolParam("type", "string", true, "View type in kebab-case", null),
                new ToolParam("name", "string", true, "Name of the view", null)
            ),
            params -> {
                CreateViewCmd cmd = new CreateViewCmd(
                    (String) params.get("type"),
                    (String) params.get("name")
                );
                return viewsCore.createView(cmd);
            }
        ));
        // get_view
        register(new Tool(
            "get_view",
            "Get view",
            Arrays.asList(
                new ToolParam("view_id", "string", true, "View id", null)
            ),
            params -> viewsCore.getView(new GetViewQuery((String) params.get("view_id")))
        ));
        // delete_view
        register(new Tool(
            "delete_view",
            "Delete view",
            Arrays.asList(
                new ToolParam("view_id", "string", true, "View id", null)
            ),
            params -> {
                viewsCore.deleteView(new DeleteViewCmd((String) params.get("view_id")));
                return Map.of("deleted", Boolean.TRUE);
            }
        ));
        // get_view_content
        register(new Tool(
            "get_view_content",
            "Get view content",
            Arrays.asList(
                new ToolParam("view_id", "string", true, "View id", null)
            ),
            params -> viewsCore.getViewContent(new GetViewContentQuery((String) params.get("view_id")))
        ));
        // get_view_image
        register(new Tool(
            "get_view_image",
            "Get view image",
            Arrays.asList(
                new ToolParam("view_id", "string", true, "View id", null),
                new ToolParam("format", "string", false, "png or svg", "png"),
                new ToolParam("scale", "number", false, "Scale factor", 1.0),
                new ToolParam("dpi", "integer", false, "DPI for png", null),
                new ToolParam("bg", "string", false, "Background color", null),
                new ToolParam("margin", "integer", false, "Margin in pixels", 0)
            ),
            params -> {
                Double scaleD = params.get("scale") instanceof Number ? ((Number) params.get("scale")).doubleValue() : null;
                Integer marginI = params.get("margin") instanceof Number ? ((Number) params.get("margin")).intValue() : null;
                GetViewImageQuery q = new GetViewImageQuery(
                    (String) params.get("view_id"),
                    (String) params.get("format"),
                    scaleD == null ? null : scaleD.floatValue(),
                    (Integer) params.get("dpi"),
                    (String) params.get("bg"),
                    marginI
                );
                ViewsCore.ImageData img = viewsCore.getViewImage(q);
                String b64 = Base64.getEncoder().encodeToString(img.data);
                return Map.of(
                    "data_base64", b64,
                    "content_type", img.contentType,
                    "length", img.data.length
                );
            }
        ));
        // get_elements
        register(new Tool(
            "get_elements",
            "Get elements",
            Arrays.asList(
                new ToolParam("ids", "array", true, "Element ids", null),
                new ToolParam("include_relations", "boolean", false, "Include relations", Boolean.FALSE),
                new ToolParam("include_elements", "boolean", false, "Include relation endpoints", Boolean.FALSE)
            ),
            params -> {
                @SuppressWarnings("unchecked") List<String> ids = (List<String>) params.get("ids");
                validateArraySize(ids, 50);
                boolean incRel = Boolean.TRUE.equals(params.get("include_relations"));
                boolean incEl = Boolean.TRUE.equals(params.get("include_elements"));
                List<Map<String, Object>> res = new ArrayList<>();
                for (String id : ids) {
                    res.add(elementsCore.getElement(new GetElementQuery(id, incRel, incEl)));
                }
                return res;
            }
        ));
        // create_elements
        register(new Tool(
            "create_elements",
            "Create elements",
            Arrays.asList(
                new ToolParam("items", "array", true, "Items to create", null)
            ),
            params -> {
                @SuppressWarnings("unchecked") List<Map<String, Object>> items =
                    (List<Map<String, Object>>) params.get("items");
                validateArraySize(items, 50);
                List<CreateElementItem> list = new ArrayList<>();
                for (Map<String, Object> i : items) {
                    @SuppressWarnings("unchecked") Map<String,String> props =
                        (Map<String,String>) i.get("properties");
                    list.add(new CreateElementItem(
                        (String) i.get("modelId"),
                        (String) i.get("type"),
                        (String) i.get("name"),
                        (String) i.get("folderId"),
                        props,
                        (String) i.get("documentation")
                    ));
                }
                return elementsCore.createElements(new CreateElementsCmd(list));
            }
        ));
        // update_elements
        register(new Tool(
            "update_elements",
            "Update elements",
            Arrays.asList(
                new ToolParam("items", "array", true, "Items to update", null)
            ),
            params -> {
                @SuppressWarnings("unchecked") List<Map<String, Object>> items =
                    (List<Map<String, Object>>) params.get("items");
                validateArraySize(items, 50);
                List<UpdateElementItem> list = new ArrayList<>();
                for (Map<String, Object> i : items) {
                    @SuppressWarnings("unchecked") Map<String,String> props =
                        (Map<String,String>) i.get("properties");
                    list.add(new UpdateElementItem(
                        (String) i.get("id"),
                        (String) i.get("name"),
                        (String) i.get("type"),
                        (String) i.get("folderId"),
                        props,
                        (String) i.get("documentation")
                    ));
                }
                return elementsCore.updateElements(new UpdateElementsCmd(list));
            }
        ));
        // delete_elements
        register(new Tool(
            "delete_elements",
            "Delete elements",
            Arrays.asList(
                new ToolParam("ids", "array", true, "Element ids", null)
            ),
            params -> {
                @SuppressWarnings("unchecked") List<String> ids = (List<String>) params.get("ids");
                validateArraySize(ids, 50);
                List<DeleteElementItem> list = ids.stream()
                    .map(DeleteElementItem::new)
                    .collect(Collectors.toList());
                return elementsCore.deleteElements(new DeleteElementsCmd(list));
            }
        ));
        // get_relations
        register(new Tool(
            "get_relations",
            "Get relations",
            Arrays.asList(
                new ToolParam("ids", "array", true, "Relation ids", null)
            ),
            params -> {
                @SuppressWarnings("unchecked") List<String> ids = (List<String>) params.get("ids");
                validateArraySize(ids, 50);
                List<Map<String, Object>> res = new ArrayList<>();
                for (String id : ids) {
                    res.add(relationsCore.getRelation(new GetRelationQuery(id)));
                }
                return res;
            }
        ));
        // create_relations
        register(new Tool(
            "create_relations",
            "Create relations",
            Arrays.asList(
                new ToolParam("items", "array", true, "Items to create", null)
            ),
            params -> {
                @SuppressWarnings("unchecked") List<Map<String, Object>> items =
                    (List<Map<String, Object>>) params.get("items");
                validateArraySize(items, 50);
                List<CreateRelationItem> list = new ArrayList<>();
                for (Map<String, Object> i : items) {
                    @SuppressWarnings("unchecked") Map<String,String> props =
                        (Map<String,String>) i.get("properties");
                    list.add(new CreateRelationItem(
                        (String) i.get("type"),
                        (String) i.get("name"),
                        (String) i.get("sourceId"),
                        (String) i.get("targetId"),
                        (String) i.get("folderId"),
                        props,
                        (String) i.get("documentation")
                    ));
                }
                return relationsCore.createRelations(new CreateRelationsCmd(list));
            }
        ));
        // update_relations
        register(new Tool(
            "update_relations",
            "Update relations",
            Arrays.asList(
                new ToolParam("items", "array", true, "Items to update", null)
            ),
            params -> {
                @SuppressWarnings("unchecked") List<Map<String, Object>> items =
                    (List<Map<String, Object>>) params.get("items");
                validateArraySize(items, 50);
                List<UpdateRelationItem> list = new ArrayList<>();
                for (Map<String, Object> i : items) {
                    @SuppressWarnings("unchecked") Map<String,String> props =
                        (Map<String,String>) i.get("properties");
                    list.add(new UpdateRelationItem(
                        (String) i.get("id"),
                        (String) i.get("name"),
                        (String) i.get("type"),
                        props,
                        (String) i.get("documentation")
                    ));
                }
                return relationsCore.updateRelations(new UpdateRelationsCmd(list));
            }
        ));
        // delete_relations
        register(new Tool(
            "delete_relations",
            "Delete relations",
            Arrays.asList(
                new ToolParam("ids", "array", true, "Relation ids", null)
            ),
            params -> {
                @SuppressWarnings("unchecked") List<String> ids = (List<String>) params.get("ids");
                validateArraySize(ids, 50);
                List<DeleteRelationItem> list = ids.stream()
                    .map(DeleteRelationItem::new)
                    .collect(Collectors.toList());
                return relationsCore.deleteRelations(new DeleteRelationsCmd(list));
            }
        ));
        // add_elements_to_view
        register(new Tool(
            "add_elements_to_view",
            "Add elements to view",
            Arrays.asList(
                new ToolParam("view_id", "string", true, "View id", null),
                new ToolParam("items", "array", true, "Items to add", null)
            ),
            params -> {
                String viewId = (String) params.get("view_id");
                @SuppressWarnings("unchecked") List<Map<String, Object>> items =
                    (List<Map<String, Object>>) params.get("items");
                validateArraySize(items, 50);
                List<AddElementToViewItem> list = new ArrayList<>();
                for (Map<String, Object> i : items) {
                    Integer x = asInt(i.get("x"));
                    Integer y = asInt(i.get("y"));
                    Integer w = asInt(i.get("w"));
                    Integer h = asInt(i.get("h"));
                    @SuppressWarnings("unchecked") Map<String,String> style =
                        (Map<String,String>) i.get("style");
                    list.add(new AddElementToViewItem(
                        (String) i.get("elementId"),
                        (String) i.get("parentObjectId"),
                        x, y, w, h,
                        style
                    ));
                }
                AddElementsToViewCmd cmd = new AddElementsToViewCmd(viewId, list);
                return viewsCore.addElements(cmd);
            }
        ));
        // add_relations_to_view
        register(new Tool(
            "add_relations_to_view",
            "Add relations to view",
            Arrays.asList(
                new ToolParam("view_id", "string", true, "View id", null),
                new ToolParam("items", "array", true, "Items to add", null)
            ),
            params -> {
                String viewId = (String) params.get("view_id");
                @SuppressWarnings("unchecked") List<Map<String, Object>> items =
                    (List<Map<String, Object>>) params.get("items");
                validateArraySize(items, 50);
                List<AddRelationToViewItem> list = new ArrayList<>();
                for (Map<String, Object> i : items) {
                    list.add(new AddRelationToViewItem(
                        (String) i.get("relationId"),
                        (String) i.get("sourceObjectId"),
                        (String) i.get("targetObjectId"),
                        (String) i.get("policy"),
                        (Boolean) i.get("suppressWhenNested")
                    ));
                }
                AddRelationsToViewCmd cmd = new AddRelationsToViewCmd(viewId, list);
                return viewsCore.addRelations(cmd);
            }
        ));
        // update_objects_bounds
        register(new Tool(
            "update_objects_bounds",
            "Update view objects bounds",
            Arrays.asList(
                new ToolParam("view_id", "string", true, "View id", null),
                new ToolParam("items", "array", true, "Items to update", null)
            ),
            params -> {
                String viewId = (String) params.get("view_id");
                @SuppressWarnings("unchecked") List<Map<String, Object>> items =
                    (List<Map<String, Object>>) params.get("items");
                validateArraySize(items, 50);
                List<UpdateViewObjectBoundsItem> list = new ArrayList<>();
                for (Map<String, Object> i : items) {
                    list.add(new UpdateViewObjectBoundsItem(
                        (String) i.get("objectId"),
                        asInt(i.get("x")),
                        asInt(i.get("y")),
                        asInt(i.get("w")),
                        asInt(i.get("h"))
                    ));
                }
                UpdateViewObjectsBoundsCmd cmd = new UpdateViewObjectsBoundsCmd(viewId, list);
                return viewsCore.updateBounds(cmd);
            }
        ));
        // move_objects_to_container
        register(new Tool(
            "move_objects_to_container",
            "Move view objects to container",
            Arrays.asList(
                new ToolParam("view_id", "string", true, "View id", null),
                new ToolParam("items", "array", true, "Items to move", null)
            ),
            params -> {
                String viewId = (String) params.get("view_id");
                @SuppressWarnings("unchecked") List<Map<String, Object>> items =
                    (List<Map<String, Object>>) params.get("items");
                validateArraySize(items, 50);
                List<MoveViewObjectItem> list = new ArrayList<>();
                for (Map<String, Object> i : items) {
                    list.add(new MoveViewObjectItem(
                        (String) i.get("objectId"),
                        (String) i.get("parentObjectId"),
                        asInt(i.get("x")),
                        asInt(i.get("y")),
                        asInt(i.get("w")),
                        asInt(i.get("h")),
                        (Boolean) i.get("keepExistingConnection")
                    ));
                }
                MoveViewObjectsCmd cmd = new MoveViewObjectsCmd(viewId, list);
                return viewsCore.moveObjects(cmd);
            }
        ));
        // remove_objects_from_view
        register(new Tool(
            "remove_objects_from_view",
            "Remove objects from view",
            Arrays.asList(
                new ToolParam("view_id", "string", true, "View id", null),
                new ToolParam("object_ids", "array", true, "Object ids", null)
            ),
            params -> {
                String viewId = (String) params.get("view_id");
                @SuppressWarnings("unchecked") List<String> ids =
                    (List<String>) params.get("object_ids");
                validateArraySize(ids, 50);
                List<DeleteViewObjectItem> list = ids.stream()
                    .map(DeleteViewObjectItem::new)
                    .collect(Collectors.toList());
                DeleteViewObjectsCmd cmd = new DeleteViewObjectsCmd(viewId, list);
                return viewsCore.deleteObjects(cmd);
            }
        ));
        // save_model
        register(new Tool(
            "save_model",
            "Save model",
            Arrays.asList(
                new ToolParam("model_id", "string", false, "Model id", null),
                new ToolParam("create_backup", "boolean", false, "Create .bak file", Boolean.TRUE)
            ),
            params -> modelCore.saveModel()
        ));
        // search
        register(new Tool(
            "search",
            "Search elements, relations, views",
            Arrays.asList(
                new ToolParam("q", "string", false, "Text query", null),
                new ToolParam("kind", "string", false, "element|relation|view", null),
                new ToolParam("element_type", "string", false, "Filter by element type", null),
                new ToolParam("relation_type", "string", false, "Filter by relation type", null),
                new ToolParam("model_id", "string", false, "Restrict to model id", null),
                new ToolParam("property", "array", false, "key=value filters", null),
                new ToolParam("include_docs", "boolean", false, "Search in documentation", Boolean.FALSE),
                new ToolParam("include_props", "boolean", false, "Search in properties", Boolean.FALSE),
                new ToolParam("limit", "integer", false, "Limit results", null),
                new ToolParam("offset", "integer", false, "Offset results", null),
                new ToolParam("debug", "boolean", false, "Include debug info", Boolean.FALSE),
                new ToolParam("log", "boolean", false, "Log search query", Boolean.FALSE)
            ),
            params -> {
                SearchQuery q = new SearchQuery();
                q.q = (String) params.get("q");
                q.kind = (String) params.get("kind");
                q.elementType = (String) params.get("element_type");
                q.relationType = (String) params.get("relation_type");
                q.modelId = (String) params.get("model_id");
                q.includeDocs = Boolean.TRUE.equals(params.get("include_docs"));
                q.includeProps = Boolean.TRUE.equals(params.get("include_props"));
                if (params.get("limit") instanceof Number) {
                    q.limit = ((Number) params.get("limit")).intValue();
                }
                if (params.get("offset") instanceof Number) {
                    q.offset = ((Number) params.get("offset")).intValue();
                }
                q.debug = Boolean.TRUE.equals(params.get("debug"));
                if (Boolean.TRUE.equals(params.get("log"))) {
                    q.logTarget = "stdout";
                }
                @SuppressWarnings("unchecked") List<String> props = (List<String>) params.get("property");
                if (props != null) {
                    for (String v : props) {
                        int eq = v.indexOf('=');
                        if (eq > 0) {
                            String pk = v.substring(0, eq);
                            String pv = v.substring(eq + 1);
                            q.propertyFilters.put(pk, pv);
                        }
                    }
                }
                return searchCore.search(q);
            }
        ));
        // types
        register(new Tool(
            "types",
            "List model types",
            Collections.emptyList(),
            params -> {
                var pkg = com.archimatetool.model.IArchimatePackage.eINSTANCE;
                var elementTypes = pkg.getEClassifiers().stream()
                    .filter(c -> c instanceof org.eclipse.emf.ecore.EClass && pkg.getArchimateElement().isSuperTypeOf((org.eclipse.emf.ecore.EClass) c))
                    .map(c -> ((org.eclipse.emf.ecore.EClass) c).getName()).collect(Collectors.toList());
                var relationTypes = pkg.getEClassifiers().stream()
                    .filter(c -> c instanceof org.eclipse.emf.ecore.EClass && pkg.getArchimateRelationship().isSuperTypeOf((org.eclipse.emf.ecore.EClass) c))
                    .map(c -> ((org.eclipse.emf.ecore.EClass) c).getName()).collect(Collectors.toList());
                return Map.of(
                    "elementTypes", elementTypes,
                    "relationTypes", relationTypes,
                    "viewTypes", List.of("ArchimateDiagramModel")
                );
            }
        ));
        // folders
        register(new Tool(
            "folders",
            "List top-level folders",
            Collections.emptyList(),
            params -> foldersCore.listFolders()
        ));
        // folder_ensure
        register(new Tool(
            "folder_ensure",
            "Ensure folder path exists",
            Arrays.asList(
                new ToolParam("path", "string", true, "Slash-delimited folder path", null)
            ),
            params -> foldersCore.ensureFolder(new EnsureFolderCmd((String) params.get("path")))
        ));
    }

    private static void register(Tool tool) {
        TOOLS.put(tool.getName(), tool);
    }

    public static Tool get(String name) {
        return TOOLS.get(name);
    }

    public static List<Tool> list() {
        return new ArrayList<>(TOOLS.values());
    }

    public static List<Map<String, Object>> describeAll() {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Tool t : list()) {
            out.add(t.toMap());
        }
        return out;
    }

    private static void validateArraySize(List<?> list, int max) {
        if (list == null) throw new BadRequestException("array required");
        if (list.size() > max) throw new BadRequestException("too many items, max " + max);
    }

    private static Integer asInt(Object o) {
        return o instanceof Number ? ((Number) o).intValue() : null;
    }
}

