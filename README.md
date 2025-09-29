![Archi MCP Plugin Logo](logo.png)

# Archi MCP Plugin

Provides a local MCP and REST API on top of the currently active [Archi](https://archimatetool.com/) model.

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
      "url": "http://127.0.0.1:8765/mcp",
    }
  }
}
```


## Archi settings
- You can change the HTTP server host and port in Archi → Preferences → MCP. Warning, changing host might be unsafe, because no auth and encryption is used!
- Port precedence: Env `ARCHI_MCP_PORT` → Preferences → Default (`8765`).

### Toolbar (MCP Server)
The plugin adds a dedicated "MCP" toolbar with an "MCP Server" toggle. Clicking toggles the server on/off without restarting Archi. 

## Building the plugin yourself

The `ru.cinimex.archimatetool.mcp` folder is an Eclipse project (PDE). See detailed steps in [BUILD.md](BUILD.md).


## 🧪 Testing

The plugin includes comprehensive smoke tests for AI agents:

- **[AGENT_SMOKE_TEST.md](AGENT_SMOKE_TEST.md)** - Complete testing guide with AI agent prompts and step-by-step instructions

## Trouble Shooting
Run Archi with `-consoleLog` to see MCP plugin logs.

## AI Agents
Code mostly(like 99%) written by Codex and Cursor.

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

Copyright 2025 Cinimex

## Roadmap

Next steps

* [] API Key authentication, configuration in Archi settings
* [] Archi Headless mode support for the plugin
* [] Build instructions for the plugin
* [] Full build automation
* [] Prompt improvements for diagram operations