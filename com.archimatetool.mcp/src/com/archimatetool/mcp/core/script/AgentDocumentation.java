package com.archimatetool.mcp.core.script;

import java.util.List;
import java.util.Map;

/**
 * Documentation and guidance for autonomous agents working with Archi MCP.
 * This class provides comprehensive information about jArchi scripting,
 * ArchiMate modeling concepts, and common usage patterns.
 */
public class AgentDocumentation {

    /**
     * Complete jArchi scripting guide for autonomous agents.
     */
    public static final String JARCHI_GUIDE = """
# jArchi Scripting Guide for Autonomous Agents

## Core Concepts

### 1. Model Access
- `model` - Current active model
- `model.name` - Model name
- `model.createElement(type, name)` - Create new element
- `model.createRelationship(type, name, source, target)` - Create relationship
- `model.createArchimateView(name)` - Create new view

### 2. Element and View Discovery
- `$('element')` - Find all elements
- `$('view')` - Find all views
- `$('relation')` - Find all relationships
- `$(selector).each(function(item) {...})` - Iterate over results
- `$(selector).first()` - Get first match
- `$(selector).filter(condition)` - Filter results

### 3. View Manipulation
- `view.add(element, x, y, width, height)` - Add element to view
- `$(view).children()` - Get all objects in view
- `child.concept` - Get underlying ArchiMate element from view object
- `child.concept.name` - Element name
- `child.concept.type` - Element type

### 4. Styling Objects
- `object.fillColor = '#RRGGBB'` - Set fill color
- `object.fontColor = '#RRGGBB'` - Set font color
- `object.fontSize = number` - Set font size
- `object.opacity = number` - Set opacity (0-255)
- `object.lineColor = '#RRGGBB'` - Set line color

### 5. Properties and Documentation
- `element.documentation = 'text'` - Set documentation
- `element.prop('key', 'value')` - Set property
- `element.prop('key')` - Get property value

### 6. Console and Debugging
- `console.show()` - Show console window
- `console.log(message)` - Log message
- `console.clear()` - Clear console

## Best Practices

1. **Always start with `console.show()`** to see script output
2. **Use try-catch blocks** for error handling
3. **Check if objects exist** before manipulating them
4. **Use meaningful variable names** for clarity
5. **Log progress** at key steps for debugging

## Error Handling
```javascript
try {
    // Your script code here
    console.log('Operation successful');
} catch (error) {
    console.log('Error: ' + error.message);
}
```
""";

    /**
     * Common scripting patterns with examples.
     */
    public static final List<Map<String, String>> COMMON_PATTERNS = List.of(
        Map.of(
            "name", "Find and Style Elements",
            "description", "Find elements by name and change their visual style",
            "code", """
console.show();
$('view').each(function(view) {
    $(view).children().each(function(child) {
        if (child.concept && child.concept.name.includes('Actor')) {
            child.fillColor = '#4CAF50';
            child.fontColor = '#FFFFFF';
            console.log('Styled: ' + child.concept.name);
        }
    });
});"""
        ),
        Map.of(
            "name", "Create Element and Add to View",
            "description", "Create a new element and add it to an existing view with styling",
            "code", """
console.show();
var newActor = model.createElement('business-actor', 'New Business Actor');
newActor.documentation = 'Created by script';

var targetView = $('view').first();
if (targetView) {
    var visualObject = targetView.add(newActor, 100, 100, 120, 80);
    visualObject.fillColor = '#2196F3';
    visualObject.fontColor = '#FFFFFF';
    console.log('Added element to view: ' + targetView.name);
}"""
        ),
        Map.of(
            "name", "Create Complete Model Structure",
            "description", "Create elements, relationships, and a view with proper layout",
            "code", """
console.show();

// Create elements
var actor = model.createElement('business-actor', 'Customer');
var process = model.createElement('business-process', 'Order Processing');
var service = model.createElement('business-service', 'Order Service');

// Create relationships
var assignment = model.createRelationship('assignment-relationship', '', actor, process);
var realization = model.createRelationship('realization-relationship', '', process, service);

// Create view and add elements
var view = model.createArchimateView('Business Overview');
var actorObj = view.add(actor, 50, 100, 120, 80);
var processObj = view.add(process, 250, 100, 120, 80);
var serviceObj = view.add(service, 450, 100, 120, 80);

// Add relationships
view.add(assignment, actorObj, processObj);
view.add(realization, processObj, serviceObj);

// Style elements
actorObj.fillColor = '#FF9800';
processObj.fillColor = '#4CAF50';
serviceObj.fillColor = '#2196F3';

console.log('Created complete model structure');"""
        ),
        Map.of(
            "name", "Batch Style Update",
            "description", "Update styles for multiple elements based on their type",
            "code", """
console.show();
var colorMap = {
    'BusinessActor': '#FF9800',
    'BusinessProcess': '#4CAF50', 
    'BusinessService': '#2196F3',
    'ApplicationComponent': '#9C27B0',
    'TechnologyService': '#607D8B'
};

$('view').each(function(view) {
    console.log('Processing view: ' + view.name);
    $(view).children().each(function(child) {
        if (child.concept) {
            var color = colorMap[child.concept.type];
            if (color) {
                child.fillColor = color;
                child.fontColor = '#FFFFFF';
            }
        }
    });
});
console.log('Batch styling completed');"""
        ),
        Map.of(
            "name", "Model Analysis and Reporting",
            "description", "Analyze model contents and generate a report",
            "code", """
console.show();
console.log('=== Model Analysis Report ===');
console.log('Model: ' + model.name);

// Count elements by type
var elementCounts = {};
$('element').each(function(element) {
    var type = element.type;
    elementCounts[type] = (elementCounts[type] || 0) + 1;
});

console.log('\\nElement counts:');
for (var type in elementCounts) {
    console.log('  ' + type + ': ' + elementCounts[type]);
}

// Count views
var viewCount = $('view').size();
console.log('\\nTotal views: ' + viewCount);

// Count relationships
var relationCount = $('relation').size();
console.log('Total relationships: ' + relationCount);

console.log('\\n=== Analysis Complete ===');"""
        )
    );

    /**
     * ArchiMate modeling context and tips for agents.
     */
    public static final Map<String, Object> ARCHIMATE_CONTEXT = Map.of(
        "elementTypes", List.of(
            "business-actor", "business-role", "business-collaboration",
            "business-interface", "business-process", "business-function", 
            "business-interaction", "business-event", "business-service",
            "business-object", "contract", "representation", "product",
            "application-component", "application-collaboration", 
            "application-interface", "application-function", "application-interaction",
            "application-process", "application-event", "application-service",
            "data-object", "node", "device", "system-software",
            "technology-collaboration", "technology-interface", 
            "technology-function", "technology-process", "technology-interaction",
            "technology-event", "technology-service", "artifact",
            "equipment", "facility", "distribution-network", 
            "communication-network", "path", "material"
        ),
        "relationTypes", List.of(
            "composition-relationship", "aggregation-relationship", 
            "assignment-relationship", "realization-relationship",
            "serving-relationship", "access-relationship", "influence-relationship",
            "triggering-relationship", "flow-relationship", "specialization-relationship",
            "association-relationship"
        ),
        "viewTypes", List.of(
            "archimate-diagram-model", "sketch-view", "canvas-view"
        ),
        "modelingTips", List.of(
            "Use business-actor for people or organizational units",
            "Use business-process for sequences of business activities", 
            "Use business-service for externally visible business behavior",
            "Use application-component for modular software units",
            "Use composition-relationship for part-of relationships",
            "Use assignment-relationship for responsibility assignments",
            "Use realization-relationship when something realizes behavior",
            "Use serving-relationship for service provision",
            "Group related elements in folders for better organization",
            "Use meaningful names and documentation for all elements",
            "Apply consistent color coding by layer or type",
            "Keep views focused on specific concerns or stakeholders"
        )
    );
}
