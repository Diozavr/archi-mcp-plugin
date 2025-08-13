package com.archimatetool.mcp.server.tools;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.archimatetool.mcp.core.elements.ElementsCore;
import com.archimatetool.mcp.core.folders.FoldersCore;
import com.archimatetool.mcp.core.model.ModelCore;
import com.archimatetool.mcp.core.relations.RelationsCore;
import com.archimatetool.mcp.core.search.SearchCore;
import com.archimatetool.mcp.core.types.*;
import com.archimatetool.mcp.core.views.ViewsCore;

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
        // tools/list
        register(new Tool(
            "tools/list",
            "List all available tools",
            Collections.emptyList(),
            params -> describeAll()
        ));
        // status
        register(new Tool(
            "status",
            "Service status",
            Collections.emptyList(),
            params -> Map.of(
                "ok", Boolean.TRUE,
                "service", "archi-mcp",
                "version", "0.1.0"
            )
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
                    .map(c -> ((org.eclipse.emf.ecore.EClass) c).getName()).collect(java.util.stream.Collectors.toList());
                var relationTypes = pkg.getEClassifiers().stream()
                    .filter(c -> c instanceof org.eclipse.emf.ecore.EClass && pkg.getArchimateRelationship().isSuperTypeOf((org.eclipse.emf.ecore.EClass) c))
                    .map(c -> ((org.eclipse.emf.ecore.EClass) c).getName()).collect(java.util.stream.Collectors.toList());
                return Map.of(
                    "elementTypes", elementTypes,
                    "relationTypes", relationTypes,
                    "viewTypes", java.util.List.of("ArchimateDiagramModel")
                );
            }
        ));
        // folders/list
        register(new Tool(
            "folders/list",
            "List top-level folders",
            Collections.emptyList(),
            params -> foldersCore.listFolders()
        ));
        // folders/ensure
        register(new Tool(
            "folders/ensure",
            "Ensure folder path exists",
            Arrays.asList(
                new ToolParam("path", "string", true, "Slash-delimited folder path", null)
            ),
            params -> foldersCore.ensureFolder(new EnsureFolderCmd((String) params.get("path")))
        ));
        // views/list
        register(new Tool(
            "views/list",
            "List views",
            Collections.emptyList(),
            params -> viewsCore.listViews()
        ));
        // views/create
        register(new Tool(
            "views/create",
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
        // views/get
        register(new Tool(
            "views/get",
            "Get view by id",
            Arrays.asList(
                new ToolParam("id", "string", true, "View id", null)
            ),
            params -> viewsCore.getView(new GetViewQuery((String) params.get("id")))
        ));
        // views/delete
        register(new Tool(
            "views/delete",
            "Delete view",
            Arrays.asList(
                new ToolParam("id", "string", true, "View id", null)
            ),
            params -> {
                viewsCore.deleteView(new DeleteViewCmd((String) params.get("id")));
                return Collections.singletonMap("deleted", true);
            }
        ));
        // views/get_content
        register(new Tool(
            "views/get_content",
            "Get view content",
            Arrays.asList(
                new ToolParam("id", "string", true, "View id", null),
                new ToolParam("limit", "integer", false, "Limit number of returned objects", null),
                new ToolParam("offset", "integer", false, "Offset for returned objects", null)
            ),
            params -> viewsCore.getViewContent(new GetViewContentQuery((String) params.get("id")))
        ));
        // views/get_image
        register(new Tool(
            "views/get_image",
            "Get view image",
            Arrays.asList(
                new ToolParam("id", "string", true, "View id", null),
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
                    (String) params.get("id"),
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
        // views/add_element
        register(new Tool(
            "views/add_element",
            "Add element to view",
            Arrays.asList(
                new ToolParam("id", "string", true, "View id", null),
                new ToolParam("element_id", "string", true, "Element id", null),
                new ToolParam("parent_object_id", "string", false, "Parent diagram object id", null),
                new ToolParam("bounds", "object", true, "Bounds {x,y,w,h}", null)
            ),
            params -> {
                @SuppressWarnings("unchecked") Map<String, Object> b = (Map<String, Object>) params.get("bounds");
                Integer x = b != null && b.get("x") instanceof Number ? ((Number) b.get("x")).intValue() : null;
                Integer y = b != null && b.get("y") instanceof Number ? ((Number) b.get("y")).intValue() : null;
                Integer w = b != null && b.get("w") instanceof Number ? ((Number) b.get("w")).intValue() : null;
                Integer h = b != null && b.get("h") instanceof Number ? ((Number) b.get("h")).intValue() : null;
                AddElementToViewCmd cmd = new AddElementToViewCmd(
                    (String) params.get("id"),
                    (String) params.get("element_id"),
                    (String) params.get("parent_object_id"),
                    x, y, w, h
                );
                return viewsCore.addElement(cmd);
            }
        ));
        // views/add_relation
        register(new Tool(
            "views/add_relation",
            "Add relation to view",
            Arrays.asList(
                new ToolParam("id", "string", true, "View id", null),
                new ToolParam("relation_id", "string", true, "Relation id", null),
                new ToolParam("source_object_id", "string", false, "Source diagram object id", null),
                new ToolParam("target_object_id", "string", false, "Target diagram object id", null),
                new ToolParam("policy", "string", false, "Connection policy", "auto"),
                new ToolParam("suppress_when_nested", "boolean", false, "Suppress when nested", true)
            ),
            params -> {
                AddRelationToViewCmd cmd = new AddRelationToViewCmd(
                    (String) params.get("id"),
                    (String) params.get("relation_id"),
                    (String) params.get("source_object_id"),
                    (String) params.get("target_object_id"),
                    (Boolean) params.get("suppress_when_nested"),
                    (String) params.get("policy")
                );
                return viewsCore.addRelation(cmd);
            }
        ));
        // views/update_bounds
        register(new Tool(
            "views/update_bounds",
            "Update object bounds",
            Arrays.asList(
                new ToolParam("id", "string", true, "View id", null),
                new ToolParam("object_id", "string", true, "Diagram object id", null),
                new ToolParam("x", "integer", false, "X position", null),
                new ToolParam("y", "integer", false, "Y position", null),
                new ToolParam("w", "integer", false, "Width", null),
                new ToolParam("h", "integer", false, "Height", null)
            ),
            params -> {
                UpdateViewObjectBoundsCmd cmd = new UpdateViewObjectBoundsCmd(
                    (String) params.get("id"),
                    (String) params.get("object_id"),
                    (Integer) params.get("x"),
                    (Integer) params.get("y"),
                    (Integer) params.get("w"),
                    (Integer) params.get("h")
                );
                return viewsCore.updateBounds(cmd);
            }
        ));
        // views/move_object
        register(new Tool(
            "views/move_object",
            "Move object to container",
            Arrays.asList(
                new ToolParam("id", "string", true, "View id", null),
                new ToolParam("object_id", "string", true, "Diagram object id", null),
                new ToolParam("parent_object_id", "string", true, "Target parent diagram object id", null),
                new ToolParam("bounds", "object", false, "Optional bounds {x,y,w,h}", null)
            ),
            params -> {
                @SuppressWarnings("unchecked") Map<String, Object> b = (Map<String, Object>) params.get("bounds");
                Integer x = b != null && b.get("x") instanceof Number ? ((Number) b.get("x")).intValue() : null;
                Integer y = b != null && b.get("y") instanceof Number ? ((Number) b.get("y")).intValue() : null;
                Integer w = b != null && b.get("w") instanceof Number ? ((Number) b.get("w")).intValue() : null;
                Integer h = b != null && b.get("h") instanceof Number ? ((Number) b.get("h")).intValue() : null;
                MoveViewObjectCmd cmd = new MoveViewObjectCmd(
                    (String) params.get("id"),
                    (String) params.get("object_id"),
                    (String) params.get("parent_object_id"),
                    x, y, w, h,
                    null
                );
                return viewsCore.moveObject(cmd);
            }
        ));
        // views/remove_object
        register(new Tool(
            "views/remove_object",
            "Remove object from view",
            Arrays.asList(
                new ToolParam("id", "string", true, "View id", null),
                new ToolParam("object_id", "string", true, "Diagram object id", null)
            ),
            params -> {
                viewsCore.deleteObject(new DeleteViewObjectCmd(
                    (String) params.get("id"),
                    (String) params.get("object_id")
                ));
                return Collections.singletonMap("deleted", true);
            }
        ));
        // elements/create
        register(new Tool(
            "elements/create",
            "Create element",
            Arrays.asList(
                new ToolParam("type", "string", true, "Element type in kebab-case", null),
                new ToolParam("name", "string", true, "Element name", null),
                new ToolParam("folder_id", "string", false, "Target folder id", null),
                new ToolParam("properties", "object", false, "Properties map", null),
                new ToolParam("documentation", "string", false, "Element documentation", null)
            ),
            params -> {
                CreateElementCmd cmd = new CreateElementCmd(
                    (String) params.get("type"),
                    (String) params.get("name"),
                    (String) params.get("folder_id")
                );
                return elementsCore.createElement(cmd);
            }
        ));
        // elements/get
        register(new Tool(
            "elements/get",
            "Get element",
            Arrays.asList(
                new ToolParam("id", "string", true, "Element id", null),
                new ToolParam("include", "string", false, "Additional data to include", "relations"),
                new ToolParam("include_elements", "boolean", false, "Include related elements", false)
            ),
            params -> {
                String include = (String) params.get("include");
                boolean includeRelations = include != null && include.contains("relations");
                boolean includeElements = Boolean.TRUE.equals(params.get("include_elements"));
                GetElementQuery q = new GetElementQuery(
                    (String) params.get("id"),
                    includeRelations,
                    includeElements
                );
                return elementsCore.getElement(q);
            }
        ));
        // elements/update
        register(new Tool(
            "elements/update",
            "Update element",
            Arrays.asList(
                new ToolParam("id", "string", true, "Element id", null),
                new ToolParam("name", "string", false, "New name", null),
                new ToolParam("type", "string", false, "New element type", null),
                new ToolParam("folder_id", "string", false, "New folder id", null),
                new ToolParam("properties", "object", false, "Properties map", null),
                new ToolParam("documentation", "string", false, "Documentation", null)
            ),
            params -> {
                UpdateElementCmd cmd = new UpdateElementCmd(
                    (String) params.get("id"),
                    (String) params.get("name")
                );
                return elementsCore.updateElement(cmd);
            }
        ));
        // elements/delete
        register(new Tool(
            "elements/delete",
            "Delete element",
            Arrays.asList(
                new ToolParam("id", "string", true, "Element id", null)
            ),
            params -> {
                elementsCore.deleteElement(new DeleteElementCmd((String) params.get("id")));
                return Collections.singletonMap("deleted", true);
            }
        ));
        // elements/list_relations
        register(new Tool(
            "elements/list_relations",
            "List element relations",
            Arrays.asList(
                new ToolParam("id", "string", true, "Element id", null),
                new ToolParam("direction", "string", false, "both|in|out", "both"),
                new ToolParam("include_elements", "boolean", false, "Include related elements", false)
            ),
            params -> {
                ListElementRelationsQuery q = new ListElementRelationsQuery(
                    (String) params.get("id"),
                    (String) params.get("direction"),
                    Boolean.TRUE.equals(params.get("include_elements"))
                );
                return elementsCore.listRelations(q);
            }
        ));
        // relations/create
        register(new Tool(
            "relations/create",
            "Create relation",
            Arrays.asList(
                new ToolParam("type", "string", true, "Relation type in kebab-case", null),
                new ToolParam("source_id", "string", true, "Source element id", null),
                new ToolParam("target_id", "string", true, "Target element id", null),
                new ToolParam("name", "string", false, "Relation name", null),
                new ToolParam("folder_id", "string", false, "Target folder id", null),
                new ToolParam("properties", "object", false, "Properties map", null),
                new ToolParam("documentation", "string", false, "Relation documentation", null)
            ),
            params -> {
                CreateRelationCmd cmd = new CreateRelationCmd(
                    (String) params.get("type"),
                    (String) params.get("name"),
                    (String) params.get("source_id"),
                    (String) params.get("target_id"),
                    (String) params.get("folder_id")
                );
                return relationsCore.createRelation(cmd);
            }
        ));
        // relations/get
        register(new Tool(
            "relations/get",
            "Get relation",
            Arrays.asList(
                new ToolParam("id", "string", true, "Relation id", null)
            ),
            params -> relationsCore.getRelation(new GetRelationQuery((String) params.get("id")))
        ));
        // relations/update
        register(new Tool(
            "relations/update",
            "Update relation",
            Arrays.asList(
                new ToolParam("id", "string", true, "Relation id", null),
                new ToolParam("name", "string", false, "New name", null),
                new ToolParam("type", "string", false, "New relation type", null),
                new ToolParam("properties", "object", false, "Properties map", null),
                new ToolParam("documentation", "string", false, "Documentation", null)
            ),
            params -> {
                UpdateRelationCmd cmd = new UpdateRelationCmd(
                    (String) params.get("id"),
                    (String) params.get("name")
                );
                return relationsCore.updateRelation(cmd);
            }
        ));
        // relations/delete
        register(new Tool(
            "relations/delete",
            "Delete relation",
            Arrays.asList(
                new ToolParam("id", "string", true, "Relation id", null)
            ),
            params -> {
                relationsCore.deleteRelation(new DeleteRelationCmd((String) params.get("id")));
                return Collections.singletonMap("deleted", true);
            }
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
                new ToolParam("include_docs", "boolean", false, "Search in documentation", false),
                new ToolParam("include_props", "boolean", false, "Search in properties", false),
                new ToolParam("limit", "integer", false, "Limit results", null),
                new ToolParam("offset", "integer", false, "Offset results", null),
                new ToolParam("debug", "boolean", false, "Include debug info", false),
                new ToolParam("log", "boolean", false, "Log search query", false)
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
        // model/save
        register(new Tool(
            "model/save",
            "Save model",
            Arrays.asList(
                new ToolParam("model_id", "string", false, "Model id", null),
                new ToolParam("create_backup", "boolean", false, "Create .bak file", true)
            ),
            params -> modelCore.saveModel()
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
}
