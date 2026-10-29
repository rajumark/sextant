package io.github.rajumark.sextant.core.index

/**
 * The `links` table (index schema 11): every edge that connects one kind of thing to another, with readable keys,
 * built from the other tables at the end of the index so it can never disagree with them.
 *
 * Keys: `app`, `component:<name>`, `class:<descriptor>`, `method:<descriptor>`, `permission:<name>`,
 * `@type/name` (resources), `url:<url>`, `native:<path>`, `file:<path>` (assets). Edges inside the code (calls, field and type uses, string
 * loads) stay in their own tables; `sextant links` adds them on the fly when it walks a class or a method.
 */
internal object Links {
    /** Run after the B-tree indexes exist: the joins on classes.name and files.id use them. */
    val BUILD =
        listOf(
            // The app declares its components and requests (or defines) permissions.
            "INSERT INTO links SELECT 'app', 'declares', 'component:' || name, type FROM components",
            "INSERT INTO links SELECT DISTINCT 'app', CASE kind WHEN 'declared' THEN 'defines' ELSE 'requests' END, " +
                "'permission:' || name, kind FROM permissions",
            // A component is implemented by its class, may alias another component, needs permissions, opens URLs.
            "INSERT INTO links SELECT 'component:' || c.name, 'implemented_by', 'class:' || k.descriptor, c.type " +
                "FROM components c JOIN classes k ON k.name = c.name",
            "INSERT INTO links SELECT 'component:' || name, 'aliases', 'component:' || target_activity, NULL " +
                "FROM components WHERE target_activity IS NOT NULL",
            "INSERT INTO links SELECT 'component:' || name, 'requires', 'permission:' || permission, 'android:permission' " +
                "FROM components WHERE permission IS NOT NULL",
            "INSERT INTO links SELECT 'component:' || name, 'requires', 'permission:' || read_permission, 'android:readPermission' " +
                "FROM components WHERE read_permission IS NOT NULL",
            "INSERT INTO links SELECT 'component:' || name, 'requires', 'permission:' || write_permission, 'android:writePermission' " +
                "FROM components WHERE write_permission IS NOT NULL",
            "INSERT INTO links SELECT DISTINCT 'component:' || component, 'opens', 'url:' || url, 'deep link' FROM deep_links",
            // Resource references from code, the manifest and styles (resources.arsc), as harvested.
            "INSERT INTO links SELECT DISTINCT CASE " +
                "WHEN from_kind = 'code' THEN 'method:' || \"from\" " +
                "WHEN from_kind = 'manifest' AND \"from\" LIKE 'application%' THEN 'app' " +
                "WHEN from_kind = 'manifest' AND instr(\"from\", ' ') > 0 THEN 'component:' || substr(\"from\", instr(\"from\", ' ') + 1) " +
                "WHEN from_kind = 'manifest' THEN 'app' " +
                "ELSE '@' || \"from\" END, 'uses', ref, nullif(attribute, '') FROM resource_refs WHERE from_kind IN ('code', 'manifest', 'style')",
            // Every reference in every resource XML file (layouts, menus, drawables, colors, animations, …), from the raw layer.
            "INSERT INTO links SELECT DISTINCT f.resource, CASE WHEN a.value LIKE '@+%' THEN 'declares' ELSE 'uses' END, a.ref, a.name " +
                "FROM xml_attrs a JOIN xml_nodes n ON n.id = a.node JOIN files f ON f.id = n.file " +
                "WHERE f.resource IS NOT NULL AND a.ref IS NOT NULL",
            // Classes a resource file names: custom views as tags, fragments, behaviors, preference fragments as values.
            "INSERT INTO links SELECT DISTINCT f.resource, 'instantiates', 'class:' || k.descriptor, 'tag' " +
                "FROM xml_nodes n JOIN files f ON f.id = n.file JOIN classes k ON k.name = n.tag WHERE f.resource IS NOT NULL",
            "INSERT INTO links SELECT DISTINCT f.resource, 'instantiates', 'class:' || k.descriptor, a.name " +
                "FROM xml_attrs a JOIN xml_nodes n ON n.id = a.node JOIN files f ON f.id = n.file JOIN classes k ON k.name = a.value " +
                "WHERE f.resource IS NOT NULL",
            // Where each URL comes from.
            "INSERT INTO links SELECT DISTINCT CASE s.kind WHEN 'code' THEN 'method:' || s.location " +
                "WHEN 'resource' THEN s.location WHEN 'native' THEN 'native:' || s.location " +
                "WHEN 'asset' THEN 'file:' || s.location ELSE 'app' END, " +
                "'contains', 'url:' || e.url, s.kind FROM endpoint_sources s JOIN endpoints e ON e.id = s.endpoint " +
                "WHERE s.kind IN ('code', 'resource', 'native', 'manifest', 'asset')",
            // Native libraries load other libraries: one in the APK (same folder) or one from the system.
            "INSERT INTO links SELECT DISTINCT 'native:' || path, 'needs', coalesce('native:' || resolved, 'system:' || needed), NULL FROM elf_needed",
            // Native libraries implement the Java classes their JNI symbols name.
            "INSERT INTO links SELECT DISTINCT 'native:' || s.library, 'implements', 'class:' || k.descriptor, s.jni_method " +
                "FROM native_symbols s JOIN classes k ON k.name = s.jni_class",
        )

    val INDEXES =
        listOf(
            "CREATE INDEX links_src ON links(src, rel)",
            "CREATE INDEX links_dst ON links(dst, rel)",
        )
}
