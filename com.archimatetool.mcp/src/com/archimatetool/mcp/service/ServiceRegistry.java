package com.archimatetool.mcp.service;

public final class ServiceRegistry {
    private static final ActiveModelService ACTIVE_MODEL_SERVICE = new ActiveModelService();
    private static final ElementService ELEMENT_SERVICE = new ElementService();
    private static final RelationService RELATION_SERVICE = new RelationService();
    private static final ViewService VIEW_SERVICE = new ViewService();

    private ServiceRegistry() {}

    public static ActiveModelService activeModel() { return ACTIVE_MODEL_SERVICE; }
    public static ElementService elements() { return ELEMENT_SERVICE; }
    public static RelationService relations() { return RELATION_SERVICE; }
    public static ViewService views() { return VIEW_SERVICE; }
}


