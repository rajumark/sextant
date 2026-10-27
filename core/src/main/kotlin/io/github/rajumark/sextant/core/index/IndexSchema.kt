package io.github.rajumark.sextant.core.index

/**
 * Schema of index.sqlite (Version 3). Normalized for size: every method reference, whether defined in the APK or
 * external (framework, other libraries), has one row in `methods`, and edges point at integer ids.
 * The views give the readable, descriptor-based form.
 */
object IndexSchema {
    const val VERSION = 6

    val DDL =
        """
        CREATE TABLE meta (
          key   TEXT PRIMARY KEY,
          value TEXT
        );

        CREATE TABLE permissions (
          name             TEXT NOT NULL,
          kind             TEXT NOT NULL,   -- uses-permission | uses-permission-sdk-23 | implied | declared
          max_sdk          INTEGER,
          dangerous        INTEGER NOT NULL,
          protection_level TEXT,
          custom           INTEGER NOT NULL,
          implied_by       TEXT
        );

        CREATE TABLE components (
          type            TEXT NOT NULL,    -- activity | activity-alias | service | receiver | provider
          name            TEXT NOT NULL,
          exported        INTEGER NOT NULL,
          exported_source TEXT NOT NULL,    -- explicit | default
          enabled         INTEGER NOT NULL,
          permission      TEXT,
          process         TEXT,
          launcher        INTEGER NOT NULL,
          target_activity TEXT,
          authorities     TEXT NOT NULL,    -- JSON array
          intent_filters  TEXT NOT NULL,    -- JSON array, same shape as components.jsonl
          read_permission         TEXT,     -- providers
          write_permission        TEXT,     -- providers
          grant_uri_permissions   INTEGER,  -- providers; NULL = not set
          path_permissions        TEXT NOT NULL, -- JSON array of <path-permission>
          launch_mode             TEXT,     -- activities
          task_affinity           TEXT,     -- activities
          foreground_service_type TEXT,     -- services
          isolated_process        INTEGER,
          direct_boot_aware       INTEGER,
          label                   TEXT
        );

        -- uses-feature / uses-library / uses-native-library
        CREATE TABLE features (
          kind     TEXT NOT NULL,           -- feature | library | native-library
          name     TEXT NOT NULL,
          required INTEGER,                 -- NULL = not set (defaults to required)
          version  TEXT
        );

        -- meta-data and property on the application (scope = 'application') or a component (scope = class name)
        CREATE TABLE metadata (
          scope    TEXT NOT NULL,
          kind     TEXT NOT NULL,           -- meta-data | property
          name     TEXT NOT NULL,
          value    TEXT,
          resource TEXT
        );

        -- <queries>: package visibility (Android 11+)
        CREATE TABLE queries (
          kind       TEXT NOT NULL,         -- package | intent | provider
          name       TEXT NOT NULL,
          actions    TEXT NOT NULL,         -- JSON array
          categories TEXT NOT NULL,         -- JSON array
          data       TEXT NOT NULL          -- JSON array
        );

        CREATE TABLE deep_links (
          component TEXT NOT NULL,
          url       TEXT NOT NULL
        );

        CREATE TABLE classes (
          id           INTEGER PRIMARY KEY,
          descriptor   TEXT NOT NULL UNIQUE, -- Lcom/app/Foo;
          name         TEXT NOT NULL,        -- com.app.Foo
          package      TEXT NOT NULL,
          superclass   TEXT,
          interfaces   TEXT NOT NULL,        -- JSON array of descriptors
          access       TEXT NOT NULL,        -- space separated
          source_file  TEXT,
          dex          TEXT NOT NULL,
          kotlin       INTEGER NOT NULL,
          annotations  TEXT NOT NULL,        -- JSON array of descriptors
          method_count INTEGER NOT NULL,
          field_count  INTEGER NOT NULL
        );

        -- Every method reference. defined = 1 for methods with a definition in the APK; external ones have NULL details.
        CREATE TABLE methods (
          id           INTEGER PRIMARY KEY,
          descriptor   TEXT NOT NULL UNIQUE, -- Lcom/app/Foo;->bar(I)V
          class        TEXT NOT NULL,        -- Lcom/app/Foo;
          name         TEXT NOT NULL,
          defined      INTEGER NOT NULL,
          params       TEXT,                 -- JSON array of type descriptors
          return_type  TEXT,
          access       TEXT,
          has_code     INTEGER,
          instructions INTEGER,
          registers    INTEGER,
          calls_out    INTEGER,
          string_loads INTEGER
        );

        CREATE TABLE fields (
          id            INTEGER PRIMARY KEY,
          descriptor    TEXT NOT NULL UNIQUE, -- Lcom/app/Foo;->count:I
          class         TEXT NOT NULL,
          name          TEXT NOT NULL,
          type          TEXT NOT NULL,
          access        TEXT NOT NULL,
          initial_value TEXT
        );

        CREATE TABLE calls (
          caller  INTEGER NOT NULL REFERENCES methods(id),
          callee  INTEGER NOT NULL REFERENCES methods(id),
          kind    TEXT NOT NULL,             -- virtual | static | direct | super | interface | polymorphic
          count   INTEGER NOT NULL,
          offsets TEXT NOT NULL              -- JSON array, 16-bit code units, max 32
        );

        CREATE TABLE strings (
          id    INTEGER PRIMARY KEY,
          value TEXT NOT NULL UNIQUE
        );

        CREATE TABLE string_refs (
          string  INTEGER NOT NULL REFERENCES strings(id),
          method  INTEGER NOT NULL REFERENCES methods(id),
          count   INTEGER NOT NULL,
          offsets TEXT NOT NULL
        );

        -- Field reads/writes and type uses (new, const-class, check-cast, instance-of, arrays) per method.
        CREATE TABLE field_refs (
          method  INTEGER NOT NULL REFERENCES methods(id),
          field   TEXT NOT NULL,             -- Lcom/app/Foo;->count:I
          kind    TEXT NOT NULL,             -- read | write
          count   INTEGER NOT NULL,
          offsets TEXT NOT NULL
        );

        CREATE TABLE type_refs (
          method  INTEGER NOT NULL REFERENCES methods(id),
          type    TEXT NOT NULL,             -- Lcom/app/Foo;
          kind    TEXT NOT NULL,             -- new | class | cast | instanceof | array
          count   INTEGER NOT NULL,
          offsets TEXT NOT NULL
        );

        CREATE VIEW call_edges AS
          SELECT a.descriptor AS caller, b.descriptor AS callee, c.kind, c.count, c.offsets
          FROM calls c JOIN methods a ON a.id = c.caller JOIN methods b ON b.id = c.callee;

        CREATE VIEW string_uses AS
          SELECT s.value, m.descriptor AS method, r.count, r.offsets
          FROM string_refs r JOIN strings s ON s.id = r.string JOIN methods m ON m.id = r.method;

        -- Version 5 insights.
        CREATE TABLE libraries (
          "group"  TEXT NOT NULL,
          artifact TEXT NOT NULL,
          version  TEXT NOT NULL,
          evidence TEXT NOT NULL
        );

        CREATE TABLE frameworks (
          name     TEXT NOT NULL,
          evidence TEXT NOT NULL             -- JSON array
        );

        CREATE TABLE sdks (
          name     TEXT NOT NULL,
          category TEXT NOT NULL,
          classes  INTEGER NOT NULL,
          packages TEXT NOT NULL,            -- JSON array
          manifest TEXT NOT NULL             -- JSON array
        );

        CREATE TABLE endpoints (
          id     INTEGER PRIMARY KEY,
          url    TEXT NOT NULL UNIQUE,
          scheme TEXT NOT NULL,
          host   TEXT NOT NULL
        );

        CREATE TABLE endpoint_sources (
          endpoint INTEGER NOT NULL REFERENCES endpoints(id),
          kind     TEXT NOT NULL,            -- code | resource | deep_link | manifest | asset | native
          location TEXT NOT NULL             -- method descriptor, @string/name or component
        );

        CREATE TABLE resources (
          type  TEXT NOT NULL,               -- string | bool | integer | color | dimen
          name  TEXT NOT NULL,
          value TEXT NOT NULL
        );

        CREATE TABLE native_libraries (
          path             TEXT NOT NULL,
          abi              TEXT NOT NULL,
          name             TEXT NOT NULL,
          size_bytes       INTEGER NOT NULL,
          sha256           TEXT NOT NULL,
          elf_class        TEXT,
          exported_symbols INTEGER NOT NULL,
          jni_methods      INTEGER NOT NULL
        );

        CREATE TABLE native_symbols (
          library    TEXT NOT NULL,
          name       TEXT NOT NULL,
          jni_class  TEXT,
          jni_method TEXT
        );

        -- Launcher icon files: role is icon | round_icon, or <role>.background/.foreground/.monochrome for adaptive layers.
        CREATE TABLE icons (
          role       TEXT NOT NULL,
          resource   TEXT NOT NULL,          -- @mipmap/ic_launcher
          path       TEXT NOT NULL,          -- file inside the APK
          config     TEXT NOT NULL,          -- xxxhdpi-v4, anydpi-v26, …
          density    INTEGER,                -- dpi; NULL for anydpi/nodpi
          format     TEXT NOT NULL,          -- png | webp | jpg | gif | xml
          width      INTEGER,
          height     INTEGER,
          size_bytes INTEGER NOT NULL,
          best       INTEGER NOT NULL        -- 1 for the file Sextant picks to show
        );

        -- Review findings (static observations to check, not proven vulnerabilities).
        CREATE TABLE findings (
          id       TEXT NOT NULL,
          severity TEXT NOT NULL,            -- high | medium | low | info
          title    TEXT NOT NULL,
          detail   TEXT NOT NULL,
          count    INTEGER NOT NULL
        );

        CREATE TABLE finding_evidence (
          finding  TEXT NOT NULL,            -- findings.id
          kind     TEXT NOT NULL,            -- manifest | method | class | string | resource
          location TEXT NOT NULL,
          value    TEXT
        );

        -- Substring search (trigram: any 3+ character fragment matches). Contentless to save space:
        -- rowid = (source id << 4) | kind; kinds: 1 class, 2 method, 3 field, 4 string, 5 component, 6 permission,
        -- 7 deep_link, 8 resource, 9 endpoint, 10 sdk, 11 library, 12 native_symbol, 13 finding.
        -- Indexed text: class full name; method and field "SimpleClass.name" (field + " = value"); first 1000 chars of strings.
        CREATE VIRTUAL TABLE search USING fts5(text, content = '', tokenize = 'trigram');

        -- What each search rowid points at, for SQL users:
        --   SELECT i.kind, i.ref, i.text FROM search JOIN search_items i ON i.search_rowid = search.rowid WHERE search MATCH 'Cipher'
        CREATE VIEW search_items AS
          SELECT id * 16 + 1 AS search_rowid, 'class' AS kind, descriptor AS ref, name AS text FROM classes
          UNION ALL SELECT id * 16 + 2, 'method', descriptor, descriptor FROM methods
          UNION ALL SELECT id * 16 + 3, 'field', descriptor, descriptor || coalesce(' = ' || initial_value, '') FROM fields
          UNION ALL SELECT id * 16 + 4, 'string', id, value FROM strings
          UNION ALL SELECT rowid * 16 + 5, 'component', name, type || ' ' || name FROM components
          UNION ALL SELECT rowid * 16 + 6, 'permission', name, name FROM permissions
          UNION ALL SELECT rowid * 16 + 7, 'deep_link', component, url FROM deep_links
          UNION ALL SELECT rowid * 16 + 8, 'resource', '@' || type || '/' || name, name || ' = ' || value FROM resources
          UNION ALL SELECT id * 16 + 9, 'endpoint', url, url FROM endpoints
          UNION ALL SELECT rowid * 16 + 10, 'sdk', name, name || ' (' || category || ')' FROM sdks
          UNION ALL SELECT rowid * 16 + 11, 'library', "group" || ':' || artifact, "group" || ':' || artifact || ':' || version FROM libraries
          UNION ALL SELECT rowid * 16 + 12, 'native_symbol', library, name FROM native_symbols
          UNION ALL SELECT rowid * 16 + 13, 'finding', id, severity || ' ' || title FROM findings;
        """.trimIndent()

    /** Created after the bulk insert: much faster than maintaining them row by row. */
    val INDEXES =
        """
        CREATE INDEX methods_class ON methods(class);
        CREATE INDEX methods_name ON methods(name);
        CREATE INDEX fields_class ON fields(class);
        CREATE INDEX classes_name ON classes(name);
        CREATE INDEX classes_super ON classes(superclass);
        CREATE INDEX calls_caller ON calls(caller);
        CREATE INDEX calls_callee ON calls(callee);
        CREATE INDEX string_refs_string ON string_refs(string);
        CREATE INDEX string_refs_method ON string_refs(method);
        CREATE INDEX components_name ON components(name);
        CREATE INDEX permissions_name ON permissions(name);
        CREATE INDEX endpoints_host ON endpoints(host);
        CREATE INDEX field_refs_field ON field_refs(field);
        CREATE INDEX field_refs_method ON field_refs(method);
        CREATE INDEX type_refs_type ON type_refs(type);
        CREATE INDEX endpoint_sources_endpoint ON endpoint_sources(endpoint);
        CREATE INDEX native_symbols_jni_class ON native_symbols(jni_class);
        """.trimIndent()

    /** Kind codes stored in the low 3 bits of a search rowid. */
    val KINDS =
        listOf(
            "class",
            "method",
            "field",
            "string",
            "component",
            "permission",
            "deep_link",
            "resource",
            "endpoint",
            "sdk",
            "library",
            "native_symbol",
            "finding",
        )

    /** Search rowids keep the kind in the low 4 bits. */
    const val KIND_BITS = 4

    fun kindCode(kind: String) = KINDS.indexOf(kind) + 1

    const val MAX_SEARCH_TEXT = 1000

    /** Splits a script into statements. Comments are removed first: they may contain ';' (as in `Lcom/app/Foo;`). */
    fun statements(sql: String) =
        sql
            .lines()
            .joinToString("\n") { it.substringBefore("--") }
            .split(";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
}
