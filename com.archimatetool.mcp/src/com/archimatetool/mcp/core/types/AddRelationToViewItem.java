package com.archimatetool.mcp.core.types;

/** Item describing a relation to add to a view. */
public class AddRelationToViewItem {
    public final String relationId;
    public final String sourceObjectId;
    public final String targetObjectId;
    public final String policy;
    public final Boolean suppressWhenNested;

    public AddRelationToViewItem(String relationId, String sourceObjectId,
                                 String targetObjectId, String policy,
                                 Boolean suppressWhenNested) {
        this.relationId = relationId;
        this.sourceObjectId = sourceObjectId;
        this.targetObjectId = targetObjectId;
        this.policy = policy;
        this.suppressWhenNested = suppressWhenNested;
    }
}
