package com.archimatetool.mcp.core.types;

/** Command to add a relation to a view. */
public class AddRelationToViewCmd {
    public final String viewId;
    public final String relationId;
    public final String sourceObjectId;
    public final String targetObjectId;
    public final Boolean suppressWhenNested;
    public final String policy;

    public AddRelationToViewCmd(String viewId, String relationId, String sourceObjectId,
                                String targetObjectId, Boolean suppressWhenNested, String policy) {
        this.viewId = viewId;
        this.relationId = relationId;
        this.sourceObjectId = sourceObjectId;
        this.targetObjectId = targetObjectId;
        this.suppressWhenNested = suppressWhenNested;
        this.policy = policy;
    }
}
