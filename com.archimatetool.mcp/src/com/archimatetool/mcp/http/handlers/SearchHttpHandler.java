package com.archimatetool.mcp.http.handlers;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.archimatetool.mcp.Config;
import com.archimatetool.mcp.http.ResponseUtil;
import com.archimatetool.mcp.server.ModelApi;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IDiagramModel;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class SearchHttpHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) { ResponseUtil.methodNotAllowed(exchange); return; }
        String q = null;
        String kind = null; // element | relation | view
        String elementType = null;
        String relationType = null;
        String modelIdFilter = null;
        boolean includeDocs = false;
        boolean includeProps = false;
        int limit = 100;
        int offset = 0;
        boolean debugRequested = false;
        String logTarget = "stdout"; // stdout | script (best-effort)
        Map<String,String> propEq = new HashMap<>();
        var query = exchange.getRequestURI().getQuery();
        if (query != null) {
            for (String p: query.split("&")) {
                int i=p.indexOf('='); if (i<=0) continue; String k=p.substring(0,i); String v=URLDecoder.decode(p.substring(i+1), StandardCharsets.UTF_8);
                if ("q".equals(k)) q = v; else if ("kind".equals(k)) kind = v; else if ("elementType".equals(k)) elementType = v; else if ("relationType".equals(k)) relationType = v; else if ("modelId".equals(k)) modelIdFilter = v;
                else if ("includeDocs".equals(k)) includeDocs = "true".equalsIgnoreCase(v) || "1".equals(v);
                else if ("includeProps".equals(k)) includeProps = "true".equalsIgnoreCase(v) || "1".equals(v);
                else if ("property".equals(k)) { int eq = v.indexOf('='); if (eq>0) { String pk = v.substring(0,eq); String pv = v.substring(eq+1); propEq.put(pk, pv); } }
                else if ("limit".equals(k)) { try { limit = Math.max(1, Math.min(1000, Integer.parseInt(v))); } catch (Exception ex) { /* ignore */ } }
                else if ("offset".equals(k)) { try { offset = Math.max(0, Integer.parseInt(v)); } catch (Exception ex) { /* ignore */ } }
                else if ("debug".equals(k)) { debugRequested = isTrue(v); }
                else if ("log".equals(k) || "logTarget".equals(k)) { logTarget = v != null ? v : logTarget; }
            }
        }
        var model = com.archimatetool.mcp.service.ServiceRegistry.activeModel().getActiveModel();
        if (model == null) { ResponseUtil.conflictNoActiveModel(exchange); return; }
        if (modelIdFilter != null && !model.getId().equals(modelIdFilter)) {
            ResponseUtil.ok(exchange, Map.of("total",0,"items", List.of()));
            return;
        }
        String qlc = q != null ? q.toLowerCase() : null;
        List<Object> items = new ArrayList<>();

        final int[] foldersScanned = new int[]{0};
        final int[] elementsScanned = new int[]{0};
        final int[] elementsMatched = new int[]{0};
        final int[] relationsScanned = new int[]{0};
        final int[] relationsMatched = new int[]{0};
        final int[] viewsScanned = new int[]{0};
        final int[] viewsMatched = new int[]{0};
        final List<String> sampleMatches = new ArrayList<>();

        final String fKind = kind;
        final String fElementType = elementType;
        final String fRelationType = relationType;
        final boolean fIncludeDocs = includeDocs;
        final boolean fIncludeProps = includeProps;
        final String fQlc = qlc;
        final Map<String,String> fPropEq = propEq;

        Consumer<com.archimatetool.model.IFolder> scanFolder = new Consumer<com.archimatetool.model.IFolder>() {
            @Override public void accept(com.archimatetool.model.IFolder folder) {
                foldersScanned[0]++;
                for (Object e : folder.getElements()) {
                    if (e instanceof IArchimateElement) {
                        IArchimateElement el = (IArchimateElement)e;
                        elementsScanned[0]++;
                        boolean ok = (fKind == null || "element".equalsIgnoreCase(fKind));
                        if (ok && fQlc != null) ok &= (el.getName() != null && el.getName().toLowerCase().contains(fQlc))
                                || (fIncludeDocs && el.getDocumentation() != null && el.getDocumentation().toLowerCase().contains(fQlc));
                        if (ok && fElementType != null) ok &= el.eClass().getName().equalsIgnoreCase(fElementType) || el.eClass().getName().equals(fElementType);
                        if (!fPropEq.isEmpty() || fIncludeProps) {
                            Map<String,String> have = new HashMap<>();
                            for (Object pr : el.getProperties()) {
                                if (pr instanceof com.archimatetool.model.IProperty) {
                                    com.archimatetool.model.IProperty ip = (com.archimatetool.model.IProperty) pr;
                                    have.put(ip.getKey(), ip.getValue());
                                }
                            }
                            for (Map.Entry<String,String> en : fPropEq.entrySet()) {
                                String hv = have.get(en.getKey());
                                if (hv == null || !hv.equals(en.getValue())) { ok = false; break; }
                            }
                            if (fIncludeProps && fPropEq.isEmpty() && fQlc != null) {
                                boolean any = false;
                                for (Map.Entry<String,String> en : have.entrySet()) {
                                    if ((en.getKey() != null && en.getKey().toLowerCase().contains(fQlc)) || (en.getValue() != null && en.getValue().toLowerCase().contains(fQlc))) { any = true; break; }
                                }
                                ok = ok || any;
                            }
                        }
                        if (ok) {
                            elementsMatched[0]++;
                            if (sampleMatches.size() < 5) sampleMatches.add(el.getName());
                            items.add(Map.of("kind","element","element", ModelApi.elementToDto(el)));
                        }
                    }
                    else if (e instanceof com.archimatetool.model.IArchimateRelationship) {
                        com.archimatetool.model.IArchimateRelationship r = (com.archimatetool.model.IArchimateRelationship)e;
                        relationsScanned[0]++;
                        boolean ok = (fKind == null || "relation".equalsIgnoreCase(fKind));
                        if (ok && fQlc != null) ok &= (r.getName() != null && r.getName().toLowerCase().contains(fQlc));
                        if (ok && fRelationType != null) ok &= r.eClass().getName().equalsIgnoreCase(fRelationType) || r.eClass().getName().equals(fRelationType);
                        if (ok) {
                            relationsMatched[0]++;
                            if (sampleMatches.size() < 5) sampleMatches.add(r.getName());
                            items.add(Map.of("kind","relation","relation", ModelApi.relationToDto(r)));
                        }
                    }
                    else if (e instanceof IDiagramModel) {
                        IDiagramModel v = (IDiagramModel) e;
                        viewsScanned[0]++;
                        boolean ok = (fKind == null || "view".equalsIgnoreCase(fKind));
                        if (ok && fQlc != null) ok &= (v.getName() != null && v.getName().toLowerCase().contains(fQlc));
                        if (ok) {
                            viewsMatched[0]++;
                            if (sampleMatches.size() < 5) sampleMatches.add(v.getName());
                            items.add(Map.of("kind","view","view", ModelApi.viewToDto(v)));
                        }
                    }
                }
                for (Object sf : folder.getFolders()) if (sf instanceof com.archimatetool.model.IFolder) accept((com.archimatetool.model.IFolder) sf);
            }
        };

        for (Object f : model.getFolders()) if (f instanceof com.archimatetool.model.IFolder) scanFolder.accept((com.archimatetool.model.IFolder) f);

        int total = items.size();
        int from = Math.min(offset, total);
        int to = Math.min(from + limit, total);
        List<Object> page = items.subList(from, to);

        boolean doLog = Config.isDebugEnabled() || debugRequested;
        if (doLog) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Archi MCP][search] q='").append(q).append("' kind=").append(kind)
              .append(" includeDocs=").append(includeDocs).append(" includeProps=").append(includeProps)
              .append(" limit=").append(limit).append(" offset=").append(offset).append('\n');
            sb.append("  scanned: folders=").append(foldersScanned[0])
              .append(" elements=").append(elementsScanned[0])
              .append(" relations=").append(relationsScanned[0])
              .append(" views=").append(viewsScanned[0]).append('\n');
            sb.append("  matched: elements=").append(elementsMatched[0])
              .append(" relations=").append(relationsMatched[0])
              .append(" views=").append(viewsMatched[0])
              .append(" total=").append(total).append('\n');
            if (!sampleMatches.isEmpty()) {
                sb.append("  samples: ").append(String.join(", ", sampleMatches));
            }
            String msg = sb.toString();
            if ("script".equalsIgnoreCase(logTarget)) {
                boolean scripted = false;
                try {
                    Class<?> consoleClass = Class.forName("com.archimatetool.script.views.ConsoleView");
                    java.lang.reflect.Method getInstance = consoleClass.getMethod("getDefault");
                    Object console = getInstance.invoke(null);
                    java.lang.reflect.Method println = consoleClass.getMethod("println", String.class);
                    println.invoke(console, msg);
                    scripted = true;
                } catch (Throwable ignore) { /* fall back to stdout */ }
                if (!scripted) System.out.println(msg);
            } else {
                System.out.println(msg);
            }
        }
        Map<String,Object> resp = new HashMap<>();
        resp.put("total", total);
        resp.put("items", page);
        if (doLog) {
            Map<String,Object> dbg = new HashMap<>();
            dbg.put("query", q);
            dbg.put("kind", kind);
            dbg.put("includeDocs", includeDocs);
            dbg.put("includeProps", includeProps);
            dbg.put("limit", limit);
            dbg.put("offset", offset);
            Map<String,Object> scanned = new HashMap<>();
            scanned.put("folders", foldersScanned[0]);
            scanned.put("elements", elementsScanned[0]);
            scanned.put("relations", relationsScanned[0]);
            scanned.put("views", viewsScanned[0]);
            Map<String,Object> matched = new HashMap<>();
            matched.put("elements", elementsMatched[0]);
            matched.put("relations", relationsMatched[0]);
            matched.put("views", viewsMatched[0]);
            matched.put("total", total);
            dbg.put("scanned", scanned);
            dbg.put("matched", matched);
            if (!sampleMatches.isEmpty()) dbg.put("samples", new ArrayList<>(sampleMatches));
            resp.put("debug", dbg);
        }
        ResponseUtil.ok(exchange, resp);
    }

    private static boolean isTrue(String s) {
        return s != null && ("true".equalsIgnoreCase(s) || "1".equals(s) || "yes".equalsIgnoreCase(s));
    }
}


