package io.github.rajumark.sextant.core.index

/**
 * Schema of index.sqlite (Version 3). Normalized for size: every method reference, whether defined in the APK or
 * external (framework, other libraries), has one row in `methods`, and edges point at integer ids.
 * The views give the readable, descriptor-based form.
 */
object IndexSchema {
    const val VERSION = 10

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
          label                   TEXT,
          tags                    TEXT NOT NULL, -- JSON array of verdicts: launcher, app-link, deep-link, hidden-entry, task-hijack, …
          attributes              TEXT      -- JSON object of the other type-specific attributes that are set (launchMode, theme, …)
        );

        -- res/ and assets/ (index schema 7). String resources are separate from code strings (table strings).
        CREATE TABLE string_resources (
          name    TEXT NOT NULL,
          kind    TEXT NOT NULL,            -- string | plurals | array
          config  TEXT NOT NULL,            -- '' default, fr, pt-rBR, night, …
          locale  TEXT,                     -- BCP-47, NULL for non-locale configs
          item    TEXT,                     -- plural quantity or array index
          value   TEXT NOT NULL
        );
        CREATE TABLE resource_ids (
          id      TEXT NOT NULL,            -- 0x7f0e001c
          type    TEXT NOT NULL,
          name    TEXT NOT NULL,
          configs TEXT NOT NULL             -- JSON array
        );
        CREATE TABLE styles (
          name     TEXT NOT NULL,
          config   TEXT NOT NULL,
          parent   TEXT,
          items    TEXT NOT NULL,           -- JSON array of {name, raw, resolved}
          used_by  TEXT NOT NULL            -- JSON array: application / activity names using it as theme
        );
        CREATE TABLE drawables (
          type    TEXT NOT NULL,            -- drawable | mipmap
          name    TEXT NOT NULL,
          config  TEXT NOT NULL,
          path    TEXT NOT NULL,
          format  TEXT NOT NULL,            -- png | webp | vector | shape | selector | adaptive-icon | …
          bytes   INTEGER NOT NULL,
          width   INTEGER,
          height  INTEGER
        );
        CREATE TABLE layouts (
          name         TEXT NOT NULL,
          config       TEXT NOT NULL,
          path         TEXT NOT NULL,
          root         TEXT,
          views        INTEGER NOT NULL,
          ids          TEXT NOT NULL,       -- JSON array
          custom_views TEXT NOT NULL,       -- JSON array
          fragments    TEXT NOT NULL,       -- JSON array
          includes     TEXT NOT NULL        -- JSON array
        );
        CREATE TABLE menus (
          menu           TEXT NOT NULL,
          config         TEXT NOT NULL,
          depth          INTEGER NOT NULL,
          tag            TEXT NOT NULL,     -- item | group | menu
          id             TEXT,
          title          TEXT,              -- as written (@string/x)
          title_resolved TEXT,
          icon           TEXT,
          show_as_action TEXT
        );
        CREATE TABLE navigation_destinations (
          graph       TEXT NOT NULL,
          id          TEXT,
          kind        TEXT NOT NULL,        -- fragment | activity | dialog | navigation | …
          class_name  TEXT,
          label       TEXT,
          deep_links  TEXT NOT NULL,        -- JSON array
          actions     TEXT NOT NULL         -- JSON array of {id, destination, pop_up_to}
        );
        CREATE TABLE assets (
          path        TEXT NOT NULL,
          kind        TEXT NOT NULL,
          bytes       INTEGER NOT NULL,
          magic       TEXT,                 -- dex | elf | zip when the content is code or an archive
          hidden_code INTEGER NOT NULL
        );
        CREATE TABLE xml_configs (
          name    TEXT NOT NULL,
          config  TEXT NOT NULL,
          path    TEXT NOT NULL,
          kind    TEXT NOT NULL,            -- network-security-config | file-provider-paths | backup-rules | …
          summary TEXT NOT NULL,            -- JSON object
          xml     TEXT NOT NULL
        );
        CREATE TABLE resource_refs (
          ref       TEXT NOT NULL,          -- @string/app_name
          from_kind TEXT NOT NULL,          -- layout | menu | navigation | xml | style | manifest | code (from = method, attribute = the first call after the id is loaded, usually the one it is passed to)
          "from"    TEXT NOT NULL,
          attribute TEXT NOT NULL
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

        -- Every zip entry of the APK (index schema 8): the bottom layer, so every byte is accounted for.
        -- decoded_by names the decoder that turned the file into rows; NULL = listed only. See `sextant coverage`.
        CREATE TABLE files (
          id               INTEGER PRIMARY KEY,
          path             TEXT NOT NULL UNIQUE,
          type             TEXT NOT NULL,  -- manifest | resource-table | dex | resource | native-library | asset | signature | library-version | kotlin-metadata | service-loader | meta-inf | other
          size_bytes       INTEGER NOT NULL,
          compressed_bytes INTEGER NOT NULL,
          method           TEXT NOT NULL,  -- stored | deflated
          crc32            TEXT NOT NULL,  -- hex, from the zip header
          sha256           TEXT,           -- of the uncompressed bytes; NULL = unreadable or over 1 GB
          magic            TEXT,           -- content from the first bytes: dex | elf | zip | png | webp | jpeg | gif | binary-xml | resource-table | xml | json | font | text
          resource         TEXT,           -- @layout/main: the resource this res/ file is (shrunk apps rename files)
          config           TEXT,           -- that resource's configuration: '' default, xxxhdpi-v4, night, …
          decoded_by       TEXT            -- manifest | resource-table | code | layouts | menus | navigation | xml-configs | drawables | assets | native | libraries | signing
        );

        -- The raw layer (index schema 10): every binary XML file of the APK (manifest, layouts, menus, drawables, colors,
        -- animations, …) node by node, so any view or attribute can be queried. `sextant xml` prints a file rebuilt from these rows.
        CREATE TABLE xml_nodes (
          id     INTEGER PRIMARY KEY,
          file   INTEGER,                 -- files.id
          path   TEXT NOT NULL,           -- the file inside the APK, e.g. res/BW.xml
          parent INTEGER,                 -- xml_nodes.id; NULL for the root element
          depth  INTEGER NOT NULL,        -- 0 for the root
          seq    INTEGER NOT NULL,        -- document order within the file
          tag    TEXT NOT NULL,           -- LinearLayout, com.app.ui.Card, item, vector, …
          text   TEXT                     -- element text, when it has no child elements
        );
        CREATE TABLE xml_attrs (
          node  INTEGER NOT NULL,         -- xml_nodes.id
          name  TEXT NOT NULL,            -- android:text, app:layout_constraintTop_toTopOf, …
          value TEXT NOT NULL,            -- as decoded: @string/title, 16.0dip, #ff000000, match_parent
          ref   TEXT                      -- the resource it points at (@string/title, ?attr/colorPrimary, @android:color/white); NULL for plain values
        );

        -- resources.arsc in every configuration (index schema 10). Simple values and file paths in resource_values;
        -- bags (styles, plurals, arrays, attr enums and flags) item by item in resource_bag_items.
        CREATE TABLE resource_values (
          id     TEXT NOT NULL,           -- 0x7f0e001c
          type   TEXT NOT NULL,
          name   TEXT NOT NULL,
          config TEXT NOT NULL,           -- '' default, fr, night, xxhdpi-v4, …
          value  TEXT,                    -- the value, or the file path for file resources; NULL for bags
          parent TEXT                     -- bags: parent style
        );
        CREATE TABLE resource_bag_items (
          id     TEXT NOT NULL,           -- resource_values.id
          config TEXT NOT NULL,
          seq    INTEGER NOT NULL,
          key    TEXT NOT NULL,           -- style attribute, plural quantity, array index or enum/flag name
          value  TEXT NOT NULL
        );

        -- Full records (index schema 9): every manifest, resource and insight record exactly as Sextant built it, one JSON
        -- row each, so a command can rebuild the whole extraction from this file alone. The tables above are the
        -- queryable view of the same facts. kind: manifest | permission | component | feature | metadata | query | resources | insights
        CREATE TABLE records (
          kind TEXT NOT NULL,
          seq  INTEGER NOT NULL,
          json TEXT NOT NULL
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
        -- 7 deep_link, 8 resource, 9 endpoint, 10 sdk, 11 library, 12 native_symbol, 13 finding, 14 string_resource.
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
          UNION ALL SELECT rowid * 16 + 13, 'finding', id, severity || ' ' || title FROM findings
          UNION ALL SELECT rowid * 16 + 14, 'string_resource', '@string/' || name, name || ' = ' || value FROM string_resources;
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
        CREATE INDEX files_type ON files(type);
        CREATE INDEX xml_nodes_file ON xml_nodes(path);
        CREATE INDEX xml_nodes_tag ON xml_nodes(tag);
        CREATE INDEX xml_attrs_node ON xml_attrs(node);
        CREATE INDEX xml_attrs_name ON xml_attrs(name);
        CREATE INDEX xml_attrs_ref ON xml_attrs(ref);
        CREATE INDEX resource_values_name ON resource_values(type, name);
        CREATE INDEX resource_bag_items_id ON resource_bag_items(id);
        CREATE INDEX files_resource ON files(resource);
        CREATE INDEX string_resources_name ON string_resources(name);
        CREATE INDEX resource_refs_ref ON resource_refs(ref);
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
            "string_resource",
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
