package ru.cinimex.archimatetool.mcp.service;

public final class ServiceRegistry {
    private static IActiveModelService activeModelService = new ActiveModelService();
    private static final ElementService ELEMENT_SERVICE = new ElementService();
    private static final RelationService RELATION_SERVICE = new RelationService();
    private static final ViewService VIEW_SERVICE = new ViewService();

    private ServiceRegistry() {}

    public static IActiveModelService activeModel() { return activeModelService; }
    public static ElementService elements() { return ELEMENT_SERVICE; }
    public static RelationService relations() { return RELATION_SERVICE; }
    public static ViewService views() { return VIEW_SERVICE; }
    
    /**
     * Set a custom active model service implementation (for testing).
     * @param service the service implementation to use
     */
    public static void setActiveModelService(IActiveModelService service) {
        activeModelService = service;
    }
    
    /**
     * Reset to default active model service (for cleanup after tests).
     */
    public static void resetActiveModelService() {
        activeModelService = new ActiveModelService();
    }
}


