package com.archimatetool.mcp.core.types;

import java.util.HashMap;
import java.util.Map;

/** Typed parameters for search operations. */
public class SearchQuery {
    public String q;
    public String kind;
    public String elementType;
    public String relationType;
    public String modelId;
    public boolean includeDocs;
    public boolean includeProps;
    public int limit = 100;
    public int offset = 0;
    public boolean debug;
    public String logTarget;
    public Map<String,String> propertyFilters = new HashMap<>();
}
