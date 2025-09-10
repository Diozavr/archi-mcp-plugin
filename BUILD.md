## Building the plugin in Eclipse (PDE)

This document describes how to build and install the `ru.cinimex.archimatetool.mcp` plugin for Archi 5.x using Eclipse PDE.

### Requirements
- **Eclipse**: Eclipse IDE for RCP and RAP Developers (2023‑12 or newer) with PDE.
- **JDK**: Java 17 (Temurin/OpenJDK). In Eclipse, set JavaSE‑17 as the default JRE.
- **Archi**: Installed Archi 5.x. You will need the path to its `plugins` folder.

### Import the project
1. File → Import → Existing Projects into Workspace.
2. Select the `archi-mcp-plugin` directory and import the `ru.cinimex.archimatetool.mcp` project.
3. Project → Properties → Java Compiler → set **17**.

### Target Platform (Archi 5.x)
1. Preferences → Plug‑in Development → Target Platform → Add…
2. Choose “Nothing: Start with an empty target” → Next.
3. Add → Directory → point to the `plugins` folder of your Archi installation (for example, `C:\\Program Files\\Archi\\plugins` or a path to an unpacked Archi distribution).
4. Name it “Archi 5.x” → Finish → mark Active → Apply & Close.

Verify that dependencies listed in `META-INF/MANIFEST.MF` (Dependencies tab) resolve: `com.archimatetool.editor`, `com.archimatetool.model`, `org.eclipse.*`, etc.

### Export the plugin (PDE Export)
1. Project → Clean.
2. File → Export → Plug‑in Development → Deployable plug‑ins and fragments.
3. Tick `ru.cinimex.archimatetool.mcp`.
4. Destination:
   - Choose Archive file and a path for `ru.cinimex.archimatetool.mcp_<version>.zip`.
   - Uncheck “Include required plug‑ins” (a full Archi bundle is not needed).
   - “Use class files” — default is fine.
5. Finish.

The `build.properties` file already includes the necessary resources (`lib/`, `img/`, `resources/`) and Jackson jars, so they will be packaged into the resulting ZIP.

### Install into Archi
- Place the produced ZIP into the `dropins` folder of your Archi installation (for example, `C:\\Program Files\\Archi\\dropins`) and restart Archi.
- Alternatively, unpack the ZIP contents into a subfolder `dropins\\ru.cinimex.archimatetool.mcp`.

### Run/debug from Eclipse (optional)
1. Run → Run Configurations… → Eclipse Application → New.
2. Select “Run an application: `com.archimatetool.editor.product`” (if the Archi product is available in the Target Platform).
3. VM arguments (optional): `-Darchi.mcp.port=8765`.
4. Run. In Archi you should see the “MCP” toolbar with the “MCP Server” toggle.

### Quick checks
- Toggle “MCP Server” on the toolbar to start the server.
- Open `http://127.0.0.1:8765/status` — expected `{"ok":true,...}`.
- If jArchi is installed, script REST routes will be available.

### Notes
- The port is configurable: System Property `archi.mcp.port`, Env `ARCHI_MCP_PORT`, or Preferences (Archi → Preferences → MCP). Precedence: System Property → Env → Preferences → Default (`8765`).
- Dependency `com.archimatetool.script` is optional — without jArchi, scripting features are disabled, everything else works.
- Requires Java 17 (`Bundle-RequiredExecutionEnvironment: JavaSE-17`).

### Common issues
- Build errors: run Clean Project.
- “Unresolved bundle”/red dependencies: activate the Target Platform and point to the correct Archi 5.x `plugins` folder.
- Wrong JRE version: ensure the project compiles with 17 and JDK 17 is installed.
- Port in use: change the port via VM argument or Preferences.


