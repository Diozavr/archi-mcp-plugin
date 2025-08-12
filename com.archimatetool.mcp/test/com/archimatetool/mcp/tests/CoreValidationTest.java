package com.archimatetool.mcp.tests;

import org.junit.Test;

import com.archimatetool.mcp.core.elements.ElementsCore;
import com.archimatetool.mcp.core.errors.BadRequestException;
import com.archimatetool.mcp.core.relations.RelationsCore;
import com.archimatetool.mcp.core.types.CreateElementCmd;
import com.archimatetool.mcp.core.types.ListElementRelationsQuery;
import com.archimatetool.mcp.core.types.CreateRelationCmd;
import com.archimatetool.mcp.core.types.GetRelationQuery;
import com.archimatetool.mcp.core.types.UpdateElementCmd;
import com.archimatetool.mcp.core.types.UpdateRelationCmd;
import com.archimatetool.mcp.core.validation.Validators;

public class CoreValidationTest {

    @Test(expected = BadRequestException.class)
    public void createElementRequiresTypeAndName() {
        new ElementsCore().createElement(new CreateElementCmd(null, null, null));
    }

    @Test(expected = BadRequestException.class)
    public void updateElementRequiresId() {
        new ElementsCore().updateElement(new UpdateElementCmd(null, "n"));
    }

    @Test(expected = BadRequestException.class)
    public void listRelationsRejectsInvalidDirection() {
        new ElementsCore().listRelations(new ListElementRelationsQuery("x", "north", false));
    }

    @Test(expected = BadRequestException.class)
    public void createRelationRequiresTypeAndIds() {
        new RelationsCore().createRelation(new CreateRelationCmd(null, null, null, null, null));
    }

    @Test(expected = BadRequestException.class)
    public void getRelationRequiresId() {
        new RelationsCore().getRelation(new GetRelationQuery(null));
    }

    @Test(expected = BadRequestException.class)
    public void updateRelationRequiresId() {
        new RelationsCore().updateRelation(new UpdateRelationCmd(null, "n"));
    }

    @Test
    public void validatorsAcceptValidInput() {
        Validators.require(true, "ok");
        Validators.requireNonNull("x", "x");
        Validators.requireNonEmpty("a", "a");
        Validators.requireNonNegative(0, "z");
    }
}
