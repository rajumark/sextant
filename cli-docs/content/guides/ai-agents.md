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

- `--dir <folder>` starts with an existing extraction as the current app.
- `--apk <app.apk>` sets the APK used by `source`, `smali` and `icon`.

### Tools

| Tool | What it does |
|---|---|
| `extract` | Analyze an APK and make it the session's current app; returns `summary.md` |
| `summary` | The app overview |
| `find` | Substring search over names, strings, URLs, resources, SDKs, native symbols, findings |
| `class` | One class as JSON: hierarchy, fields, methods, callers, strings |
| `callers` / `callees` | Call trees |
| `reach` | Call paths from an entry point to targets |
| `xref` | Where a class or field is used |
| `sql` | Read-only SQL; pass `"schema"` for all tables |
| `source` / `smali` | Decompiled Java / bytecode for a class or method |
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

The output folder itself is agent-friendly:

1. **Read `summary.md` first.** It's under ~80 lines: identity, counts, exported components, insights, findings, and which file holds what.
2. **`grep` the JSONL files.** Every line is a complete record with full names, so a single hit is understandable on its own:

    ```bash
    grep '"exported":true' app/components.jsonl
    grep 'api.example.com' app/code/strings.jsonl
    grep '"caller":"Lcom/example/Login;->auth' app/code/calls.jsonl
    ```

3. **Use `--json`** on `info`, `find`, `class`, `sql`, `callers`, `callees`, `reach`, `xref`, `insights`, `endpoints`, `findings`, `diff` and `icon` for machine-readable output on stdout. Progress and errors always go to stderr, so piping is safe.
