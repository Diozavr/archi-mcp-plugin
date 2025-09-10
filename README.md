# Archi MCP Plugin

Provides a local HTTP API on top of the currently active [Archi](https://archimatetool.com/) model. Acts as a backend for the MCP server [TykTechnologies/api-to-mcp](https://github.com/TykTechnologies/api-to-mcp) and can be used directly via REST (localhost only).

![image](docs/image.png)


## Plugin capabilities
Enables LLMs to fetch model information, modify objects and diagrams, and execute scripts via the REST API.


Provides LLMs access to the **currently active** Archi model:
- Search elements and relations in the model
- Create and edit elements
- Create and edit relations
- Create and edit diagrams
  - Add elements and relations to a diagram, remove from a diagram, move elements
  - Support for nested elements
  - Export diagram images to PNG and SVG
- Get the list of available element and relation types
- Run scripts (requires the jArchi plugin to be installed)
- Retrieve status and openapi.json

Most operations support batch mode to speed up workflows by processing multiple items at once.


## Installation

### Archi
Requirements: Archi 5.x; running scripts requires the jArchi plugin.

Download from [Releases](https://github.com/Diozavr/archi-mcp-plugin/releases).
To install, place the .zip file into the Archi `dropins` directory.

The plugin starts automatically with Archi.

You can change the HTTP server port in Archi → Preferences → MCP.

Note: the API is available **without authentication** at http://localhost:8765/ (local connections only).

### MCP
Example configuration for Cursor

```json
{
  "mcpServers": {
    "archi-mcp": {
      "command": "npx",
      "args": [
        "-y",
        "@tyk-technologies/api-to-mcp@latest",
        "--spec",
        "http://127.0.0.1:8765/openapi.json"
      ]
    }
  }
}
```


## Archi settings
- You can change the HTTP server port in Archi → Preferences → MCP.
- Port precedence: Env `ARCHI_MCP_PORT` → Preferences → Default (`8765`).

### Toolbar (MCP Server)
The plugin adds a dedicated "MCP" toolbar with an "MCP Server" toggle. Clicking toggles the server on/off without restarting Archi. 

## Building the plugin yourself

The `com.archimatetool.mcp` folder is an Eclipse project (PDE). See detailed steps in [BUILD.md](BUILD.md).


## AI Agents
Code mostly(like 99%) written by Codex and Cursor.

## Roadmap

Next steps

* [] API Key authentication, configuration in Archi settings
* [] Headless mode support for the plugin
* [] Build instructions for the plugin
* [] Full build automation
* [] Prompt improvements for diagram operations