package com.archimatetool.mcp.server.tools;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.archimatetool.mcp.core.elements.ElementsCore;
import com.archimatetool.mcp.core.errors.CoreException;
import com.archimatetool.mcp.core.folders.FoldersCore;
import com.archimatetool.mcp.core.model.ModelCore;
import com.archimatetool.mcp.core.relations.RelationsCore;
import com.archimatetool.mcp.core.search.SearchCore;
import com.archimatetool.mcp.core.types.AddElementToViewCmd;
import com.archimatetool.mcp.core.types.AddRelationToViewCmd;
import com.archimatetool.mcp.core.types.CreateElementCmd;
import com.archimatetool.mcp.core.types.CreateRelationCmd;
import com.archimatetool.mcp.core.types.CreateViewCmd;
import com.archimatetool.mcp.core.types.DeleteElementCmd;
import com.archimatetool.mcp.core.types.DeleteRelationCmd;
import com.archimatetool.mcp.core.types.DeleteViewCmd;
import com.archimatetool.mcp.core.types.DeleteViewObjectCmd;
import com.archimatetool.mcp.core.types.EnsureFolderCmd;
import com.archimatetool.mcp.core.types.GetElementQuery;
import com.archimatetool.mcp.core.types.GetRelationQuery;
import com.archimatetool.mcp.core.types.GetViewContentQuery;
import com.archimatetool.mcp.core.types.GetViewImageQuery;
import com.archimatetool.mcp.core.types.GetViewQuery;
import com.archimatetool.mcp.core.types.MoveViewObjectCmd;
import com.archimatetool.mcp.core.types.SearchQuery;
import com.archimatetool.mcp.core.types.UpdateElementCmd;
import com.archimatetool.mcp.core.types.UpdateRelationCmd;
import com.archimatetool.mcp.core.types.UpdateViewObjectBoundsCmd;
import com.archimatetool.mcp.core.views.ViewsCore;
import com.archimatetool.mcp.server.JacksonJson;
import com.fasterxml.jackson.databind.JsonNode;

public class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    public void register(Tool tool) {
        tools.put(tool.getName(), tool);
    }

    public Tool get(String name) {
        return tools.get(name);
    }

    public Collection<Tool> all() {
        return tools.values();
    }

    public static ToolRegistry createDefault(ElementsCore elements, RelationsCore relations,
            ViewsCore views, SearchCore search, FoldersCore folders, ModelCore model) {
        ToolRegistry reg = new ToolRegistry();

        reg.register(new Tool("status", "Service status", Map.of(), args -> Map.of("ok", true)));

        reg.register(new Tool("openapi", "OpenAPI spec", Map.of(), args -> {
            try {
                JsonNode node = JacksonJson.readTree(ToolRegistry.class.getClassLoader()
                        .getResourceAsStream("resources/openapi.json"));
                return node;
            } catch (Exception ex) {
                throw new CoreException("openapi not found");
            }
        }));

        reg.register(new Tool("types", "Available element, relation and view types", Map.of(), args -> {
            return Map.of(
                "elementTypes", com.archimatetool.model.IArchimatePackage.eINSTANCE.getEClassifiers().stream()
                    .filter(c -> c instanceof org.eclipse.emf.ecore.EClass && com.archimatetool.model.IArchimatePackage.eINSTANCE.getArchimateElement().isSuperTypeOf((org.eclipse.emf.ecore.EClass)c))
                    .map(c -> ((org.eclipse.emf.ecore.EClass)c).getName()).collect(java.util.stream.Collectors.toList()),
                "relationTypes", com.archimatetool.model.IArchimatePackage.eINSTANCE.getEClassifiers().stream()
                    .filter(c -> c instanceof org.eclipse.emf.ecore.EClass && com.archimatetool.model.IArchimatePackage.eINSTANCE.getArchimateRelationship().isSuperTypeOf((org.eclipse.emf.ecore.EClass)c))
                    .map(c -> ((org.eclipse.emf.ecore.EClass)c).getName()).collect(java.util.stream.Collectors.toList()),
                "viewTypes", java.util.List.of("ArchimateDiagramModel"));
        }));

        reg.register(new Tool("folders", "List root folders", Map.of(), args -> folders.listFolders()));

        reg.register(new Tool("folder_ensure", "Ensure folder by path", Map.of(
                "path", new Param("path", "string", "Folder path", true)
        ), args -> folders.ensureFolder(new EnsureFolderCmd(text(args, "path")))));

        reg.register(new Tool("search", "Search model", Map.of(
                "q", new Param("q", "string", "Query text", false)
        ), args -> {
            SearchQuery q = new SearchQuery();
            if (args != null) {
                q.q = text(args, "q");
                q.kind = text(args, "kind");
                q.elementType = text(args, "elementType");
                q.relationType = text(args, "relationType");
                q.modelId = text(args, "modelId");
                q.includeDocs = bool(args, "includeDocs");
                q.includeProps = bool(args, "includeProps");
                q.limit = intOr(args, "limit", 100);
                q.offset = intOr(args, "offset", 0);
                q.debug = bool(args, "debug");
                q.logTarget = textOr(args, "logTarget", "stdout");
                JsonNode props = args.get("propertyFilters");
                if (props != null && props.isObject()) {
                    props.fields().forEachRemaining(e -> q.propertyFilters.put(e.getKey(), e.getValue().asText()));
                }
            }
            return search.search(q);
        }));

        reg.register(new Tool("list_views", "List views", Map.of(), args -> views.listViews()));

        reg.register(new Tool("create_view", "Create view", Map.of(
                "type", new Param("type", "string", "View type", true),
                "name", new Param("name", "string", "View name", false),
                "folderId", new Param("folderId", "string", "Folder id", false)
        ), args -> views.createView(new CreateViewCmd(text(args, "type"), text(args, "name"), text(args, "folderId")))));

        reg.register(new Tool("get_view", "Get view", Map.of(
                "id", new Param("id", "string", "View id", true)
        ), args -> views.getView(new GetViewQuery(text(args, "id")))));

        reg.register(new Tool("delete_view", "Delete view", Map.of(
                "id", new Param("id", "string", "View id", true)
        ), args -> { views.deleteView(new DeleteViewCmd(text(args, "id"))); return Map.of("deleted", true); }));

        reg.register(new Tool("get_view_content", "View content", Map.of(
                "id", new Param("id", "string", "View id", true)
        ), args -> views.getViewContent(new GetViewContentQuery(text(args, "id")))));

        reg.register(new Tool("get_view_image", "Render view image", Map.of(
                "id", new Param("id", "string", "View id", true),
                "format", new Param("format", "string", "png|svg", false),
                "scale", new Param("scale", "number", "Scale", false),
                "dpi", new Param("dpi", "integer", "DPI for png", false),
                "bg", new Param("bg", "string", "Background", false),
                "margin", new Param("margin", "integer", "Margin", false)
        ), args -> {
            ViewsCore.ImageData img = views.getViewImage(new GetViewImageQuery(
                text(args, "id"), text(args, "format"), floatOr(args, "scale", null), intOr(args, "dpi", null),
                text(args, "bg"), intOr(args, "margin", null)));
            String b64 = java.util.Base64.getEncoder().encodeToString(img.data);
            return Map.of("data", b64, "contentType", img.contentType);
        }));

        reg.register(new Tool("create_element", "Create element", Map.of(
                "type", new Param("type", "string", "Element type", true),
                "name", new Param("name", "string", "Element name", false),
                "folderId", new Param("folderId", "string", "Folder id", false)
        ), args -> elements.createElement(new CreateElementCmd(text(args, "type"), text(args, "name"), text(args, "folderId")))));

        reg.register(new Tool("get_element", "Get element", Map.of(
                "id", new Param("id", "string", "Element id", true),
                "includeRelations", new Param("includeRelations", "boolean", "Include relations", false),
                "includeElements", new Param("includeElements", "boolean", "Include elements", false)
        ), args -> elements.getElement(new GetElementQuery(text(args, "id"), bool(args, "includeRelations"), bool(args, "includeElements")))));

        reg.register(new Tool("update_element", "Update element", Map.of(
                "id", new Param("id", "string", "Element id", true),
                "name", new Param("name", "string", "New name", false)
        ), args -> elements.updateElement(new UpdateElementCmd(text(args, "id"), text(args, "name")))));

        reg.register(new Tool("delete_element", "Delete element", Map.of(
                "id", new Param("id", "string", "Element id", true)
        ), args -> { elements.deleteElement(new DeleteElementCmd(text(args, "id"))); return Map.of("deleted", true); }));

        reg.register(new Tool("create_relation", "Create relation", Map.of(
                "type", new Param("type", "string", "Relation type", true),
                "sourceId", new Param("sourceId", "string", "Source id", true),
                "targetId", new Param("targetId", "string", "Target id", true),
                "name", new Param("name", "string", "Relation name", false),
                "folderId", new Param("folderId", "string", "Folder id", false)
        ), args -> relations.createRelation(new CreateRelationCmd(text(args, "type"), text(args, "sourceId"), text(args, "targetId"), text(args, "name"), text(args, "folderId")))));

        reg.register(new Tool("get_relation", "Get relation", Map.of(
                "id", new Param("id", "string", "Relation id", true)
        ), args -> relations.getRelation(new GetRelationQuery(text(args, "id")))));

        reg.register(new Tool("update_relation", "Update relation", Map.of(
                "id", new Param("id", "string", "Relation id", true),
                "name", new Param("name", "string", "New name", false)
        ), args -> relations.updateRelation(new UpdateRelationCmd(text(args, "id"), text(args, "name")))));

        reg.register(new Tool("delete_relation", "Delete relation", Map.of(
                "id", new Param("id", "string", "Relation id", true)
        ), args -> { relations.deleteRelation(new DeleteRelationCmd(text(args, "id"))); return Map.of("deleted", true); }));

        reg.register(new Tool("add_element_to_view", "Add element to view", Map.of(
                "viewId", new Param("viewId", "string", "View id", true),
                "elementId", new Param("elementId", "string", "Element id", true),
                "parentObjectId", new Param("parentObjectId", "string", "Parent object id", false),
                "bounds", new Param("bounds", "object", "Bounds object", false)
        ), args -> views.addElementToView(new AddElementToViewCmd(
                text(args, "viewId"), text(args, "elementId"), text(args, "parentObjectId"),
                bounds(args.get("bounds"))))));

        reg.register(new Tool("add_relation_to_view", "Add relation to view", Map.of(
                "viewId", new Param("viewId", "string", "View id", true),
                "relationId", new Param("relationId", "string", "Relation id", true),
                "sourceObjectId", new Param("sourceObjectId", "string", "Source object id", false),
                "targetObjectId", new Param("targetObjectId", "string", "Target object id", false),
                "policy", new Param("policy", "string", "auto|explicit", false),
                "suppressWhenNested", new Param("suppressWhenNested", "boolean", "Suppress when nested", false)
        ), args -> views.addRelationToView(new AddRelationToViewCmd(
                text(args, "viewId"), text(args, "relationId"), text(args, "sourceObjectId"), text(args, "targetObjectId"),
                text(args, "policy"), bool(args, "suppressWhenNested")))));

        reg.register(new Tool("update_object_bounds", "Update view object bounds", Map.of(
                "viewId", new Param("viewId", "string", "View id", true),
                "objectId", new Param("objectId", "string", "Object id", true),
                "x", new Param("x", "integer", "x", false),
                "y", new Param("y", "integer", "y", false),
                "w", new Param("w", "integer", "width", false),
                "h", new Param("h", "integer", "height", false)
        ), args -> views.updateBounds(new UpdateViewObjectBoundsCmd(
                text(args, "viewId"), text(args, "objectId"), intOr(args, "x", null), intOr(args, "y", null),
                intOr(args, "w", null), intOr(args, "h", null)))));

        reg.register(new Tool("move_object_to_container", "Move view object", Map.of(
                "viewId", new Param("viewId", "string", "View id", true),
                "objectId", new Param("objectId", "string", "Object id", true),
                "parentObjectId", new Param("parentObjectId", "string", "Parent object", false),
                "x", new Param("x", "integer", "x", false),
                "y", new Param("y", "integer", "y", false),
                "w", new Param("w", "integer", "width", false),
                "h", new Param("h", "integer", "height", false),
                "keepExistingConnection", new Param("keepExistingConnection", "boolean", "Keep connection", false)
        ), args -> views.moveObject(new MoveViewObjectCmd(
                text(args, "viewId"), text(args, "objectId"), text(args, "parentObjectId"),
                intOr(args, "x", null), intOr(args, "y", null), intOr(args, "w", null), intOr(args, "h", null),
                bool(args, "keepExistingConnection")))));

        reg.register(new Tool("remove_object_from_view", "Remove object from view", Map.of(
                "viewId", new Param("viewId", "string", "View id", true),
                "objectId", new Param("objectId", "string", "Object id", true)
        ), args -> { views.deleteObject(new DeleteViewObjectCmd(text(args, "viewId"), text(args, "objectId"))); return Map.of("deleted", true); }));

        reg.register(new Tool("model_save", "Save model", Map.of(), args -> model.saveModel()));

        return reg;
    }

    private static String text(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asText(null) : null;
    }
    private static String textOr(JsonNode node, String field, String d) {
        return node != null && node.has(field) ? node.get(field).asText(d) : d;
    }
    private static boolean bool(JsonNode node, String field) {
        return node != null && node.has(field) && node.get(field).asBoolean(false);
    }
    private static Integer intOr(JsonNode node, String field, Integer d) {
        return node != null && node.has(field) ? (node.get(field).isNull()?d:node.get(field).asInt()) : d;
    }
    private static Float floatOr(JsonNode node, String field, Float d) {
        return node != null && node.has(field) ? (node.get(field).isNull()?d:(float)node.get(field).asDouble()) : d;
    }
    private static com.archimatetool.mcp.core.types.Bounds bounds(JsonNode node) {
        if (node == null || node.isNull()) return null;
        return new com.archimatetool.mcp.core.types.Bounds(intOr(node, "x", null), intOr(node, "y", null), intOr(node, "w", null), intOr(node, "h", null));
    }
}
