# 🧪 Smoke Test for Archi MCP Plugin

A single guide for testing the MCP API for Archi via an AI agent.

## 🤖 Prompt for the AI Agent

```
You are the smoke‑testing agent for Archi MCP. Use ONLY MCP tools. At each step print "[SMOKE] <step>" and the result. If you get the error "no active model" — end the test as SUCCESSFUL.

IMPORTANT:
- The API uses EMF PascalCase: BusinessActor, AssignmentRelationship
- Kebab-case is accepted on input: business-actor → BusinessActor
- For batch operations pass JSON strings in arrays
- If "no active model" = skip model steps and finish SUCCESS
```

## 🚀 Quick Test (12 steps)

For a fast check of core functionality:

### 1. Status → 2. Types → 3. Folders
```
status() → {"ok": true}
types() → verify PascalCase: "BusinessActor", "AssignmentRelationship" 
folders() → if "no active model" = SUCCESS and STOP
```

### 4. Create → 5. Relations → 6. Get
```
create_elements(['{"type":"BusinessActor","name":"TestActor"}']) → save id1
create_relations(['{"type":"AssignmentRelationship","sourceId":"id1","targetId":"id2"}']) → save rel_id
get_elements([id1]) → verify PascalCase in type
```

### 7. Search → 8. Views → 9. Cleanup → 10. Save
```
search({"q":"Test","kind":"element"}) → verify PascalCase
list_views() → obtain view_id
delete_relations([rel_id]) + delete_elements([id1])
save_model() → finish
```

### 11. Script Engines → 12. Run Script
```
list_script_engines() → if empty: log "no scripting plugin" and continue
run_script({"engine":"ajs","code":"console.log('SMOKE AJS'); return 2+2;"}) → expect ok=true, result=4
```

## 📋 Full Test (15 steps)

For a thorough verification of all features:

### Step 1: Status Check
```
[SMOKE] Step 1: Status Check
- Call "status"
- Expect: {"ok": true, "service": "Archi MCP"}
```

### Step 2: Get Types
```
[SMOKE] Step 2: Get Types
- Call "types"
- Verify PascalCase: "BusinessActor", "AssignmentRelationship", "ArchimateDiagramModel"
- Save the lists for later use
```

### Step 3: Model Check
```
[SMOKE] Step 3: Check Model Folders
- Call "folders"
- If error "no active model" → report "No active model, skipping model checks" and FINISH as SUCCESSFUL
- Otherwise continue with the active model
```

### Step 4: Create Elements
```
[SMOKE] Step 4: Create Elements
- Call "create_elements" with a batch:
  [
    '{"type":"BusinessActor","name":"E1"}',
    '{"type":"BusinessRole","name":"E2"}', 
    '{"type":"BusinessProcess","name":"E3"}',
    '{"type":"BusinessFunction","name":"E4"}',
    '{"type":"BusinessEvent","name":"E5"}'
  ]
- Save eids in creation order
- Verify PascalCase in responses: "type": "BusinessActor"
```

### Step 5: Get Elements
```
[SMOKE] Step 5: Get Elements
- Call "get_elements" with ids = eids
- Verify PascalCase in the returned types
```

### Step 6: Update Elements
```
[SMOKE] Step 6: Update Elements
- Call "update_elements":
  [
    '{"id":"' + eids[0] + '","name":"E1 Updated"}',
    '{"id":"' + eids[1] + '","name":"E2 Updated"}'
  ]
```

### Step 7: Create Relations
```
[SMOKE] Step 7: Create Relations
- Call "create_relations":
  [
    '{"type":"AssignmentRelationship","sourceId":"' + eids[0] + '","targetId":"' + eids[1] + '","name":"R1"}',
    '{"type":"AssignmentRelationship","sourceId":"' + eids[1] + '","targetId":"' + eids[2] + '","name":"R2"}',
    '{"type":"AssignmentRelationship","sourceId":"' + eids[2] + '","targetId":"' + eids[3] + '","name":"R3"}'
  ]
- Save rids
- Verify PascalCase: "type": "AssignmentRelationship"
```

### Step 8: Get Relations
```
[SMOKE] Step 8: Get Relations
- Call "get_relations" with ids = rids
- Verify PascalCase in types
```

### Step 9: Update Relations
```
[SMOKE] Step 9: Update Relations
- Call "update_relations":
  [
    '{"id":"' + rids[0] + '","name":"R1 Updated"}',
    '{"id":"' + rids[1] + '","name":"R2 Updated"}'
  ]
```

### Step 10: Working with Views
```
[SMOKE] Step 10: Views
10.1) list_views → get existing or create a new one
10.2) If there are no views:
      create_view({"type":"ArchimateDiagramModel","name":"Smoke Test View"}) → viewId
10.3) add_elements_to_view:
      {
        "view_id": viewId,
        "items": [
          '{"elementId":"' + eids[0] + '","x":50,"y":50,"w":120,"h":80}',
          '{"elementId":"' + eids[1] + '","x":200,"y":50,"w":120,"h":80}'
        ]
      }
10.4) add_relations_to_view:
      {
        "view_id": viewId,
        "items": ['{"relationId":"' + rids[0] + '"}']
      }
```

### Step 11: Temporary Objects
```
[SMOKE] Step 11: Temporary Objects
11.1) create_view({"type":"ArchimateDiagramModel","name":"Temp View"}) → tmpViewId
11.2) create_elements(['{"type":"BusinessActor","name":"TempActor"}']) → tmpElId
11.3) add_elements_to_view with tmpElId into tmpViewId
11.4) get_view_content(tmpViewId) → obtain objectId
11.5) remove_objects_from_view
11.6) delete_elements([tmpElId])
11.7) delete_view(tmpViewId)
```

### Step 12: Final Cleanup
```
[SMOKE] Step 12: Cleanup
12.1) delete_relations({"ids": rids})
12.2) delete_elements({"ids": eids})
```

### Step 13: Save
```
[SMOKE] Step 13: Save
- save_model({}) → log the save path
```

### Step 14: Script Engines
```
[SMOKE] Step 14: Script Engines
- Call "list_script_engines"
- If result is empty → report "No scripting plugin installed" and CONTINUE
- Otherwise verify that "ajs" is present (at minimum)
```

### Step 15: Run Script
```
[SMOKE] Step 15: Run Script
- Call "run_script" with:
  {
    "engine": "ajs",
    "code": "console.log('SMOKE AJS'); return 2+2;",
    "timeoutMs": 3000
  }
- Expect: ok=true, result=4, stdout contains "SMOKE AJS"
- On systems without scripting plugin: request may be rejected (501/Not Implemented) → log and CONTINUE
```

## 🔍 Additional Checks

### Search
```
- search({"q":"E","kind":"element","limit":5}) → verify PascalCase in results
- search({"q":"Updated","kind":"relation","limit":3}) → verify relation types
```

### Image Export
```
- get_view_image({
    "view_id": viewId,
    "format": "png", 
    "scale": 1,
    "margin": 0
  }) → verify data_base64 length
```

### Format Compatibility
```
- Test kebab-case: create_elements(['{"type":"business-actor","name":"KebabTest"}'])
- Verify that the response is PascalCase: "type": "BusinessActor"
```

### Scripting Edge Cases
```
- Unknown engine: run_script({"engine":"unknown","code":"return 1;"}) → expect validation error
- Timeout: run_script({"engine":"ajs","code":"while(true){}","timeoutMs":100}) → expect timeout
```

## ✅ Success Criteria

### Mandatory Checks:
- ✅ Server responds (status.ok = true)
- ✅ Types are in PascalCase: "BusinessActor", "AssignmentRelationship"
- ✅ JSON parsing works (no ClassCastException)
- ✅ CRUD operations function
- ✅ Compatibility: kebab-case input → PascalCase output
- ✅ Scripting: list engines works; run_script returns result when plugin installed, otherwise graceful Not Implemented

### Data Formats:
- ✅ "Input types": `"BusinessActor"` or `"business-actor"` (both work)
- ✅ "Output types": always `"BusinessActor"` (EMF PascalCase)
- ✅ "JSON arrays": strings are parsed correctly

## 🎯 Final Report Template

```
[SMOKE] FINAL SUMMARY

✅ SMOKE PASSED

Statistics:
- Created: 5 elements, 3 relations
- Updated: 2 elements, 2 relations  
- Deleted: 5 elements, 3 relations
- Used IDs: eids=[...], rids=[...]
- View: "Smoke Test View" (ID: viewId)

Checks:
- ✅ JSON parsing: Works without errors
- ✅ Type format: EMF PascalCase standard
- ✅ Compatibility: kebab-case → PascalCase
- ✅ API operations: All functioning
- ✅ Model: Saved successfully

Status: SMOKE PASSED 🎉
```

## 🐛 Error Handling

### Known Scenarios:
1. "no active model" → Graceful exit as SUCCESS
2. ClassCastException → Fixed in the current version
3. "Unknown element type" → Check PascalCase/kebab-case
4. View errors → Check that elements exist on the view

### Fallbacks:
- On creation errors → use existing objects
- On view errors → skip view operations
- On cleanup errors → log and continue
