/*
 * Copyright 2025 Cinimex
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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


