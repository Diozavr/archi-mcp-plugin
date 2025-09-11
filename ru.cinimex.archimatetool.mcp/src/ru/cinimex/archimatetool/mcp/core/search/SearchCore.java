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
package ru.cinimex.archimatetool.mcp.core.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ru.cinimex.archimatetool.mcp.Config;
import ru.cinimex.archimatetool.mcp.core.errors.ConflictException;
import ru.cinimex.archimatetool.mcp.core.types.SearchQuery;
import ru.cinimex.archimatetool.mcp.core.validation.Validators;
import ru.cinimex.archimatetool.mcp.server.ModelApi;
import ru.cinimex.archimatetool.mcp.service.ServiceRegistry;
import com.archimatetool.model.IArchimateElement;
import com.archimatetool.model.IArchimateModel;
import com.archimatetool.model.IArchimateRelationship;
import com.archimatetool.model.IDiagramModel;
import com.archimatetool.model.IFolder;

/** Core search operations. */
public class SearchCore {

    private IArchimateModel requireModel() {
        var model = ServiceRegistry.activeModel().getActiveModel();
        if (model == null) {
            throw new ConflictException("no active model");
        }
        return model;
    }

    /** Perform a search over the active model. */
    public Map<String, Object> search(SearchQuery q) {
        Validators.requireNonNegative(q.limit, "limit");
        Validators.requireNonNegative(q.offset, "offset");
        int limit = Math.max(1, Math.min(1000, q.limit));
        int offset = Math.max(0, q.offset);

        var model = requireModel();
        if (q.modelId != null && !model.getId().equals(q.modelId)) {
            return Map.of("total", 0, "items", List.of());
        }

        String qlc = q.q != null ? q.q.toLowerCase() : null;
        List<Object> items = new ArrayList<>();

        final int[] foldersScanned = new int[]{0};
        final int[] elementsScanned = new int[]{0};
        final int[] elementsMatched = new int[]{0};
        final int[] relationsScanned = new int[]{0};
        final int[] relationsMatched = new int[]{0};
        final int[] viewsScanned = new int[]{0};
        final int[] viewsMatched = new int[]{0};
        final List<String> sampleMatches = new ArrayList<>();

        final String fKind = q.kind;
        final String fElementType = q.elementType;
        final String fRelationType = q.relationType;
        final boolean fIncludeDocs = q.includeDocs;
        final boolean fIncludeProps = q.includeProps;
        final String fQlc = qlc;
        final Map<String,String> fPropEq = q.propertyFilters;

        java.util.function.Consumer<IFolder> scanFolder = new java.util.function.Consumer<>() {
            @Override public void accept(IFolder folder) {
                foldersScanned[0]++;
                for (Object e : folder.getElements()) {
                    if (e instanceof IArchimateElement el) {
                        elementsScanned[0]++;
                        boolean ok = (fKind == null || "element".equalsIgnoreCase(fKind));
                        if (ok && fQlc != null) ok &= (el.getName() != null && el.getName().toLowerCase().contains(fQlc))
                                || (fIncludeDocs && el.getDocumentation() != null && el.getDocumentation().toLowerCase().contains(fQlc));
                        if (ok && fElementType != null) ok &= el.eClass().getName().equalsIgnoreCase(fElementType) || el.eClass().getName().equals(fElementType);
                        if (!fPropEq.isEmpty() || fIncludeProps) {
                            Map<String,String> have = new HashMap<>();
                            for (Object pr : el.getProperties()) {
                                if (pr instanceof com.archimatetool.model.IProperty ip) {
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
                    } else if (e instanceof IArchimateRelationship r) {
                        relationsScanned[0]++;
                        boolean ok = (fKind == null || "relation".equalsIgnoreCase(fKind));
                        if (ok && fQlc != null) ok &= (r.getName() != null && r.getName().toLowerCase().contains(fQlc));
                        if (ok && fRelationType != null) ok &= r.eClass().getName().equalsIgnoreCase(fRelationType) || r.eClass().getName().equals(fRelationType);
                        if (ok) {
                            relationsMatched[0]++;
                            if (sampleMatches.size() < 5) sampleMatches.add(r.getName());
                            items.add(Map.of("kind","relation","relation", ModelApi.relationToDto(r)));
                        }
                    } else if (e instanceof IDiagramModel v) {
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
                for (Object sf : folder.getFolders()) if (sf instanceof IFolder) accept((IFolder) sf);
            }
        };

        for (Object f : model.getFolders()) if (f instanceof IFolder) scanFolder.accept((IFolder) f);

        int total = items.size();
        int from = Math.min(offset, total);
        int to = Math.min(from + limit, total);
        List<Object> page = items.subList(from, to);

        boolean doLog = Config.isDebugEnabled() || q.debug;
        if (doLog) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Archi MCP][search] q='").append(q.q).append("' kind=").append(q.kind)
              .append(" includeDocs=").append(q.includeDocs).append(" includeProps=").append(q.includeProps)
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
            if ("script".equalsIgnoreCase(q.logTarget)) {
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
            dbg.put("query", q.q);
            dbg.put("kind", q.kind);
            dbg.put("includeDocs", q.includeDocs);
            dbg.put("includeProps", q.includeProps);
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
        return resp;
    }
}
