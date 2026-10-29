# AI agents and MCP

Sextant is designed so an AI agent can understand an APK without reading thousands of decompiled files.

## MCP server

`sextant mcp` runs a [Model Context Protocol](https://modelcontextprotocol.io) server on stdin/stdout (JSON-RPC 2.0). An agent can extract an APK and then explore it with the same operations as the CLI.

=== "Claude Code"

    ```bash
    claude mcp add sextant -- sextant mcp
    ```

=== "Any MCP client"

    ```json
    {
      "mcpServers": {
        "sextant": { "command": "sextant", "args": ["mcp"] }
      }
    }
    ```

Options:

- `--dir <app.sqlite>` starts with an existing extraction (or an APK) as the current app.
- `--apk <app.apk>` sets the APK used by `source`, `smali` and `icon`.

### Tools

| Tool | What it does |
|---|---|
| `extract` | Analyze an APK into one SQLite file and make it the session's current app; returns the app summary |
| `summary` | The app overview |
| `find` | Substring search over names, strings, URLs, resources, SDKs, native symbols, findings |
| `class` | One class as JSON: hierarchy, fields, methods, callers, strings |
| `callers` / `callees` | Call trees |
| `reach` | Call paths from an entry point to targets |
| `xref` | Where a class or field is used |
| `links` | What anything connects to across kinds: component → class, resource → the files and methods that use it, … |
| `sql` | Read-only SQL; pass `"schema"` for all tables |
| `source` / `smali` | Decompiled Java / bytecode for a class or method |
| `grep` | Text or regex search in decompiled Java |
| `insights` | Frameworks, SDKs, libraries, hosts, native code |
| `endpoints` | URLs with their sources |
| `findings` | Review points with evidence |
| `icon` | Launcher icon files, adaptive layers, best file |

After `extract`, the other tools work on that app without paths; every tool also accepts an explicit `dir`. Errors come back as tool results with `isError: true` and a hint, so the agent can correct itself.

Example prompts that work well:

- "Extract `~/Downloads/app.apk` and tell me which exported components reach network code."
- "Which SDKs does this app use, and which of them can access the location?"
- "Find where the API base URL is defined and show me the method that builds requests."

## Without MCP

The SQLite file itself is agent-friendly:

1. **Start with `sextant info app.apk --json`.** The APK is indexed once into a cache, so every later command is instant.
2. **Ask the file.** `sextant find app.apk <text> --json` searches everything; `sextant sql app.apk --schema` lists every table, and any SQL tool works on the `.sqlite` file too:

    ```bash
    sextant sql app.apk "SELECT type, name FROM components WHERE exported = 1" --json
    sextant find app.apk api.example.com --json
    sextant callers app.apk "Lcom/example/Login;->auth" --json
    sextant links app.apk .LoginActivity --json   # class, permissions, resources, deep links of a screen
    sextant sql app.apk "SELECT * FROM schema_docs" --json   # what every table and column means
    sextant q unused-strings app.apk --json        # saved queries; `sextant q` lists them
    ```

3. **Use `--json`** on `info`, `find`, `class`, `sql`, `callers`, `callees`, `reach`, `xref`, `insights`, `endpoints`, `findings`, `diff` and `icon` for machine-readable output on stdout. Progress and errors always go to stderr, so piping is safe.
