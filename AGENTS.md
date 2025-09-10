# AGENTS

Key guidance for working with the plugin and its REST API.

Note on localization parity: this document has both English and Russian versions (`AGENTS.md` and `AGENTS_ru.md` if present). Any change made to one language MUST be mirrored in the other to keep them in sync.

## Environment
- OS: Windows, shell — WSL. Command examples use bash.
- For REST checks use `curl` and `jq`.

## Configuration
- You can start/stop the MCP server with a button on the dedicated “MCP” toolbar; the icon reflects the state.
- The MCP HTTP server port can be set via System Property `archi.mcp.port`, environment variable `ARCHI_MCP_PORT`, or Preferences (Archi → Preferences → MCP). Precedence: System Property → Env → Preferences → Default (`8765`).

## API invariants
- Bind only to `127.0.0.1:8765` (local).
- If there is no active model — HTTP 409 and `{"error":"no active model"}`.
- Path compatibility:
  - New: `/views/{id}/content`, `/views/{id}/add-element` (POST), sub-resources like `/views/{id}/objects/...`.
  - Legacy: `/views/content?id=...`, `/views/add-element`.
- `/search` supports repeated parameters `property=key=value`.
- `/views/{id}/image` supports `format=png|svg`; `dpi` applies to PNG only.

### Search semantics
- `q` is OR-matched:
  - always by `name`
  - by `documentation` if `includeDocs=true`
  - by `properties` (key/value) if `includeProps=true`
- `property=key=value` are strict AND-filters on top of the text search.
- `kind` narrows the scope to `element|relation|view`.
- `debug=true` adds a `debug` field with counters and examples.

## Working with the model
- Any EMF model changes must go via `org.eclipse.swt.widgets.Display.getDefault().syncExec(...)`.
- Lookup by id — `ArchimateModelUtils.getObjectByID(...)`.

## Architecture
- Business logic lives in `ru.cinimex.archimatetool.mcp.core.*`.
- REST handlers only parse protocol and delegate to the core.
- The core throws `CoreException` subtypes; `ResponseUtil` and MCP map them to HTTP/JSON‑RPC errors.

## Quality/evolution
- JSON is processed with Jackson.
- Do not widen the network surface (0.0.0.0, authentication) without prior agreement: the service is intended to be local.
  For the REST contract the source of truth is `openapi.json`.

## Quick checks
- `GET /status` → `{"ok":true,...}`
- `POST /elements` with a type in kebab-case creates an element.
- `POST /model/save` → `{"saved":true,...}`
- `GET /script/engines` → `{installed:false, engines:[]}` (when the scripting plugin is missing).
- `POST /script/run` returns 501 if the scripting plugin is not installed.
- `POST /script/run` rejects unknown `engine`, invalid `timeoutMs`, and truncates `stdout/stderr` after ~10k chars.
- Extended smoke test: `ru.cinimex.archimatetool.mcp/test/test_smoke.sh` covers routes
  `/openapi.json`, `/types`, `/folders`, creating views/elements/relations,
  view object operations, `/views/{id}/image`, `/search`, error scenarios, and cleanup.
  Before running, open the test model `testdata/Archisurance.archimate` in Archi or ensure there is an active model.

## MCP Server via npx (universal way)
- Preferred way to run MCP: `npx @tyk-technologies/api-to-mcp@latest --spec http://127.0.0.1:8765/openapi.json`
- Automatically generates MCP tools from the OpenAPI spec
- Does not require the built‑in JSON‑RPC endpoint `/mcp`
- Configuration in `.cursor/mcp.json`:
  ```json
  {
    "mcpServers": {
      "archi-api": {
        "command": "npx",
        "args": ["-y", "@tyk-technologies/api-to-mcp@latest", "--spec", "http://127.0.0.1:8765/openapi.json"]
      }
    }
  }
  ```