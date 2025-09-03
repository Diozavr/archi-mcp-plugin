package com.archimatetool.mcp.tests;

import static org.junit.Assert.assertFalse;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.archimatetool.mcp.service.ServiceRegistry;

import com.archimatetool.mcp.core.elements.ElementsCore;
import com.archimatetool.mcp.core.errors.BadRequestException;
import com.archimatetool.mcp.core.relations.RelationsCore;
import com.archimatetool.mcp.core.views.ViewsCore;
import com.archimatetool.mcp.core.types.AddElementsToViewCmd;
import com.archimatetool.mcp.core.types.AddElementToViewItem;
import com.archimatetool.mcp.core.types.AddRelationsToViewCmd;
import com.archimatetool.mcp.core.types.AddRelationToViewItem;
import com.archimatetool.mcp.core.types.CreateElementItem;
import com.archimatetool.mcp.core.types.CreateElementsCmd;
import com.archimatetool.mcp.core.types.CreateRelationItem;
import com.archimatetool.mcp.core.types.CreateRelationsCmd;
import com.archimatetool.mcp.core.types.DeleteElementItem;
import com.archimatetool.mcp.core.types.DeleteElementsCmd;
import com.archimatetool.mcp.core.types.DeleteRelationItem;
import com.archimatetool.mcp.core.types.DeleteRelationsCmd;
import com.archimatetool.mcp.core.types.DeleteViewObjectItem;
import com.archimatetool.mcp.core.types.DeleteViewObjectsCmd;
import com.archimatetool.mcp.core.types.MoveViewObjectItem;
import com.archimatetool.mcp.core.types.MoveViewObjectsCmd;
import com.archimatetool.mcp.core.types.UpdateElementItem;
import com.archimatetool.mcp.core.types.UpdateElementsCmd;
import com.archimatetool.mcp.core.types.UpdateRelationItem;
import com.archimatetool.mcp.core.types.UpdateRelationsCmd;
import com.archimatetool.mcp.core.types.UpdateViewObjectBoundsItem;
import com.archimatetool.mcp.core.types.UpdateViewObjectsBoundsCmd;

public class CoreBatchValidationTest {

    @Before
    public void setUp() {
        // Use mock service to avoid Eclipse workbench dependencies
        ServiceRegistry.setActiveModelService(new MockActiveModelService());
    }
    
    @After
    public void tearDown() {
        // Reset to default service after tests
        ServiceRegistry.resetActiveModelService();
    }

    @Test(expected = BadRequestException.class)
    public void createElementsRequiresItems() {
        new ElementsCore().createElements(new CreateElementsCmd(null));
    }

    @Test(expected = BadRequestException.class)
    public void createElementsRejectsEmptyItems() {
        new ElementsCore().createElements(new CreateElementsCmd(List.of()));
    }

    @Test(expected = BadRequestException.class)
    public void createElementsRequireTypeAndName() {
        List<CreateElementItem> items = List.of(new CreateElementItem(null, null, null, null, null, null));
        new ElementsCore().createElements(new CreateElementsCmd(items));
    }

    @Test(expected = BadRequestException.class)
    public void updateElementsRequireIds() {
        List<UpdateElementItem> items = List.of(new UpdateElementItem(null, null, null, null, null, null));
        new ElementsCore().updateElements(new UpdateElementsCmd(items));
    }

    @Test(expected = BadRequestException.class)
    public void deleteElementsRequireIds() {
        List<DeleteElementItem> items = List.of(new DeleteElementItem(null));
        new ElementsCore().deleteElements(new DeleteElementsCmd(items));
    }

    @Test(expected = BadRequestException.class)
    public void createRelationsRequiresItems() {
        new RelationsCore().createRelations(new CreateRelationsCmd(null));
    }

    @Test(expected = BadRequestException.class)
    public void createRelationsRejectsEmptyItems() {
        new RelationsCore().createRelations(new CreateRelationsCmd(List.of()));
    }

    @Test(expected = BadRequestException.class)
    public void createRelationsRequireFields() {
        List<CreateRelationItem> items = List.of(new CreateRelationItem(null, null, null, null, null, null, null));
        new RelationsCore().createRelations(new CreateRelationsCmd(items));
    }

    @Test(expected = BadRequestException.class)
    public void updateRelationsRequireIds() {
        List<UpdateRelationItem> items = List.of(new UpdateRelationItem(null, null, null, null, null));
        new RelationsCore().updateRelations(new UpdateRelationsCmd(items));
    }

    @Test(expected = BadRequestException.class)
    public void deleteRelationsRequireIds() {
        List<DeleteRelationItem> items = List.of(new DeleteRelationItem(null));
        new RelationsCore().deleteRelations(new DeleteRelationsCmd(items));
    }

    @Test(expected = BadRequestException.class)
    public void addElementsToViewRequiresViewId() throws Exception {
        new ViewsCore().addElements(new AddElementsToViewCmd(null, List.of()));
    }

    @Test(expected = BadRequestException.class)
    public void addElementsToViewRejectsEmptyItems() throws Exception {
        new ViewsCore().addElements(new AddElementsToViewCmd("v", List.of()));
    }

    @Test(expected = BadRequestException.class)
    public void addElementsToViewRequireElementId() throws Exception {
        List<AddElementToViewItem> items = List.of(new AddElementToViewItem(null, null, null, null, null, null, null));
        new ViewsCore().addElements(new AddElementsToViewCmd("v", items));
    }

    @Test
    public void addElementsToViewAcceptsStylesWithoutError() throws Exception {
        // Test that styles don't cause validation errors
        Map<String, String> styles = Map.of(
            "fillColor", "#4CAF50",
            "fontColor", "#FFFFFF",
            "fontSize", "14"
        );
        List<AddElementToViewItem> items = List.of(new AddElementToViewItem("elem1", null, 100, 100, 120, 80, styles));
        // This should not throw an exception during validation
        try {
            new ViewsCore().addElements(new AddElementsToViewCmd("v", items));
        } catch (Exception e) {
            // Expected to fail with model-related errors, but not validation errors
            assertFalse("Should not be a validation error", e instanceof BadRequestException);
        }
    }

    @Test(expected = BadRequestException.class)
    public void addRelationsToViewRequiresViewId() throws Exception {
        new ViewsCore().addRelations(new AddRelationsToViewCmd(null, List.of()));
    }

    @Test(expected = BadRequestException.class)
    public void addRelationsToViewRejectsEmptyItems() throws Exception {
        new ViewsCore().addRelations(new AddRelationsToViewCmd("v", List.of()));
    }

    @Test(expected = BadRequestException.class)
    public void addRelationsToViewRequireRelationId() throws Exception {
        List<AddRelationToViewItem> items = List.of(new AddRelationToViewItem(null, null, null, null, null));
        new ViewsCore().addRelations(new AddRelationsToViewCmd("v", items));
    }

    @Test(expected = BadRequestException.class)
    public void updateBoundsRequiresViewId() throws Exception {
        new ViewsCore().updateBounds(new UpdateViewObjectsBoundsCmd(null, List.of()));
    }

    @Test(expected = BadRequestException.class)
    public void updateBoundsRejectsEmptyItems() throws Exception {
        new ViewsCore().updateBounds(new UpdateViewObjectsBoundsCmd("v", List.of()));
    }

    @Test(expected = BadRequestException.class)
    public void updateBoundsRequireObjectId() throws Exception {
        List<UpdateViewObjectBoundsItem> items = List.of(new UpdateViewObjectBoundsItem(null, null, null, null, null));
        new ViewsCore().updateBounds(new UpdateViewObjectsBoundsCmd("v", items));
    }

    @Test(expected = BadRequestException.class)
    public void moveObjectsRequiresViewId() throws Exception {
        new ViewsCore().moveObjects(new MoveViewObjectsCmd(null, List.of()));
    }

    @Test(expected = BadRequestException.class)
    public void moveObjectsRejectsEmptyItems() throws Exception {
        new ViewsCore().moveObjects(new MoveViewObjectsCmd("v", List.of()));
    }

    @Test(expected = BadRequestException.class)
    public void moveObjectsRequireFields() throws Exception {
        List<MoveViewObjectItem> items = List.of(new MoveViewObjectItem(null, null, null, null, null, null, null));
        new ViewsCore().moveObjects(new MoveViewObjectsCmd("v", items));
    }

    @Test(expected = BadRequestException.class)
    public void deleteObjectsRequiresViewId() throws Exception {
        new ViewsCore().deleteObjects(new DeleteViewObjectsCmd(null, List.of()));
    }

    @Test(expected = BadRequestException.class)
    public void deleteObjectsRejectsEmptyItems() throws Exception {
        new ViewsCore().deleteObjects(new DeleteViewObjectsCmd("v", List.of()));
    }

    @Test(expected = BadRequestException.class)
    public void deleteObjectsRequireObjectId() throws Exception {
        List<DeleteViewObjectItem> items = List.of(new DeleteViewObjectItem(null));
        new ViewsCore().deleteObjects(new DeleteViewObjectsCmd("v", items));
    }
}

