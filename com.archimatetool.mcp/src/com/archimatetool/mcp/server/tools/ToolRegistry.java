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
import com.archimatetool.mcp.core.search.SearchCore;
import com.archimatetool.mcp.core.types.*;
import com.archimatetool.mcp.core.views.ViewsCore;

/**
 * Registry of MCP tools and their metadata.
 */
public class ToolRegistry {
    private static final Map<String, Tool> TOOLS = new LinkedHashMap<>();
    private static final ElementsCore elementsCore = new ElementsCore();
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
        // get_view_content
        register(new Tool(
            "get_view_content",
            "Get view content",
            Arrays.asList(
                new ToolParam("view_id", "string", true, "View id", null)
            ),
            params -> viewsCore.getViewContent(new GetViewContentQuery((String) params.get("view_id")))
        ));
        // create_element
        register(new Tool(
            "create_element",
            "Create element",
            Arrays.asList(
                new ToolParam("type", "string", true, "Element type in kebab-case", null),
                new ToolParam("name", "string", true, "Element name", null),
                new ToolParam("folder_id", "string", false, "Target folder id", null)
            ),
            params -> {
                CreateElementItem item = new CreateElementItem(
                    null,
                    (String) params.get("type"),
                    (String) params.get("name"),
                    (String) params.get("folder_id"),
                    null,
                    null
                );
                return elementsCore.createElements(new CreateElementsCmd(List.of(item))).get(0);
            }
        ));
        // add_element_to_view
        register(new Tool(
            "add_element_to_view",
            "Add element to view",
            Arrays.asList(
                new ToolParam("view_id", "string", true, "View id", null),
                new ToolParam("element_id", "string", true, "Element id", null),
                new ToolParam("parent_object_id", "string", false, "Parent diagram object id", null),
                new ToolParam("bounds", "object", false, "Bounds {x,y,w,h}", null)
            ),
            params -> {
                @SuppressWarnings("unchecked") Map<String, Object> b = (Map<String, Object>) params.get("bounds");
                Integer x = b != null && b.get("x") instanceof Number ? ((Number) b.get("x")).intValue() : null;
                Integer y = b != null && b.get("y") instanceof Number ? ((Number) b.get("y")).intValue() : null;
                Integer w = b != null && b.get("w") instanceof Number ? ((Number) b.get("w")).intValue() : null;
                Integer h = b != null && b.get("h") instanceof Number ? ((Number) b.get("h")).intValue() : null;
                AddElementToViewItem item = new AddElementToViewItem(
                    (String) params.get("element_id"),
                    (String) params.get("parent_object_id"),
                    x, y, w, h,
                    null
                );
                AddElementsToViewCmd cmd = new AddElementsToViewCmd(
                    (String) params.get("view_id"),
                    List.of(item)
                );
                return viewsCore.addElements(cmd).get(0);
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
                    .map(c -> ((org.eclipse.emf.ecore.EClass) c).getName()).collect(java.util.stream.Collectors.toList());
                var relationTypes = pkg.getEClassifiers().stream()
                    .filter(c -> c instanceof org.eclipse.emf.ecore.EClass && pkg.getArchimateRelationship().isSuperTypeOf((org.eclipse.emf.ecore.EClass) c))
                    .map(c -> ((org.eclipse.emf.ecore.EClass) c).getName()).collect(java.util.stream.Collectors.toList());
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
}

