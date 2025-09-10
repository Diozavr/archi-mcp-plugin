package ru.cinimex.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.util.HashMap;
import java.util.stream.Collectors;

import ru.cinimex.archimatetool.mcp.http.ResponseUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class TypesHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        var out = new HashMap<String, Object>();
        out.put("elementTypes", com.archimatetool.model.IArchimatePackage.eINSTANCE.getEClassifiers().stream()
            .filter(c -> c instanceof org.eclipse.emf.ecore.EClass && com.archimatetool.model.IArchimatePackage.eINSTANCE.getArchimateElement().isSuperTypeOf((org.eclipse.emf.ecore.EClass)c))
            .map(c -> ((org.eclipse.emf.ecore.EClass)c).getName()).collect(Collectors.toList()));
        out.put("relationTypes", com.archimatetool.model.IArchimatePackage.eINSTANCE.getEClassifiers().stream()
            .filter(c -> c instanceof org.eclipse.emf.ecore.EClass && com.archimatetool.model.IArchimatePackage.eINSTANCE.getArchimateRelationship().isSuperTypeOf((org.eclipse.emf.ecore.EClass)c))
            .map(c -> ((org.eclipse.emf.ecore.EClass)c).getName()).collect(Collectors.toList()));
        out.put("viewTypes", java.util.List.of("ArchimateDiagramModel"));
        ResponseUtil.ok(exchange, out);
    }
}


