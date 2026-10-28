package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.DecodedManifest
import io.github.rajumark.sextant.core.RawResource
import io.github.rajumark.sextant.core.model.AssetRecord
import io.github.rajumark.sextant.core.model.DrawableRecord
import io.github.rajumark.sextant.core.model.LayoutRecord
import io.github.rajumark.sextant.core.model.LayoutView
import io.github.rajumark.sextant.core.model.MenuItem
import io.github.rajumark.sextant.core.model.MenuRecord
import io.github.rajumark.sextant.core.model.NavAction
import io.github.rajumark.sextant.core.model.NavDestination
import io.github.rajumark.sextant.core.model.NavigationRecord
import io.github.rajumark.sextant.core.model.ResValue
import io.github.rajumark.sextant.core.model.ResourceHarvest
import io.github.rajumark.sextant.core.model.ResourceIdRecord
import io.github.rajumark.sextant.core.model.ResourceRef
import io.github.rajumark.sextant.core.model.StringResource
import io.github.rajumark.sextant.core.model.StyleRecord
import io.github.rajumark.sextant.core.model.XmlConfigRecord
import io.github.rajumark.sextant.core.model.XmlNode
import io.github.rajumark.sextant.core.output.ManifestXml
import java.util.zip.ZipFile

/**
 * Harvests res/ and assets/: strings in every locale, resource ids, styles and themes, drawables, layouts, menus,
 * navigation graphs, assets and XML configs, plus who references what. References are kept as written (`@string/x`)
 * and resolved to their default-configuration value, so readers see both.
 */
object ResourceHarvester {
    private val ZIP_FORMATS =
        setOf(
            "ja",
            "xpi",
            "crx",
            "odp",
            "ods",
            "odt",
            "odg",
            "pptx",
            "xlsx",
            "docx",
            "epub",
            "lottie",
            "mbtiles",
            "kmz",
            "obb",
            "ttf",
            "woff",
            "pkpass",
            "sketch",
            "usdz",
            "3mf",
            "whl",
            "nupkg",
            "war",
            "ear",
        )
    private const val MAX_FILE_BYTES = 4L * 1024 * 1024
    private const val MAX_VIEWS = 2000

    /** Text assets up to this size keep their content, so readers can see the file itself. */
    private const val MAX_ASSET_TEXT = 32L * 1024
    private val TEXT_ASSETS = setOf("text", "json", "html", "js")
    private val FRAMEWORK_VIEW = Regex("""^(android\.|androidx\.|com\.google\.android\.material\.)|^[A-Z][A-Za-z]*$""")
    private val PLURAL =
        mapOf(
            0x01000004 to "other",
            0x01000005 to "zero",
            0x01000006 to "one",
            0x01000007 to "two",
            0x01000008 to "few",
            0x01000009 to "many",
        )

    /** Layout attributes worth keeping, by local name (shrunk resources drop the android: prefix). */
    private val LAYOUT_ATTRS =
        setOf(
            "text",
            "hint",
            "contentDescription",
            "src",
            "srcCompat",
            "onClick",
            "layout",
            "name",
            "class",
            "style",
            "title",
            "tooltipText",
            "navGraph",
            "inputType",
            "autofillHints",
            "importantForAutofill",
            "password",
        )

    fun harvest(
        zip: ZipFile,
        decoded: DecodedManifest,
    ): ResourceHarvest {
        val table = decoded.table
        val resolve = decoded.resources::resolve
        val refs = mutableListOf<ResourceRef>()

        fun value(
            name: String,
            raw: String,
            from: Pair<String, String>?,
        ): ResValue {
            if (raw.startsWith("@") && from != null) refs += ResourceRef(raw.removePrefix("@+"), from.first, from.second, name)
            val resolved = if (raw.startsWith("@") && !raw.startsWith("@+")) resolve(raw)?.takeIf { it != raw } else null
            return ResValue(name, raw, resolved)
        }

        val ids =
            table
                .groupBy { it.id }
                .map { (id, entries) ->
                    ResourceIdRecord("0x%08x".format(id), entries[0].type, entries[0].name, entries.map { it.config }.distinct().sorted())
                }.sortedBy { it.id }

        val strings =
            table
                .filter { it.type == "string" || it.type == "plurals" || it.type == "array" || it.type == "string-array" }
                .flatMap { e ->
                    val locale = decoded.locale(e.config)
                    when {
                        e.value != null -> listOf(StringResource(e.name, "string", e.config, locale, null, e.value))
                        e.type == "plurals" ->
                            e.items.map { (k, v) ->
                                StringResource(
                                    e.name,
                                    "plurals",
                                    e.config,
                                    locale,
                                    PLURAL[k.toIntOrNull() ?: -1] ?: k,
                                    v,
                                )
                            }
                        else -> e.items.mapIndexed { i, (_, v) -> StringResource(e.name, "array", e.config, locale, i.toString(), v) }
                    }
                }.sortedWith(compareBy({ it.name }, { it.config }, { it.item?.padStart(6, '0') ?: "" }))

        val themeUsers = mutableMapOf<String, MutableList<String>>()

        fun walkManifest(n: XmlNode) {
            n.a("theme")?.let { t ->
                val who = if (n.tag == "application") "application" else "${n.tag} ${n.a("name") ?: ""}"
                themeUsers.getOrPut(t.removePrefix("@").removePrefix("style/")) { mutableListOf() } += who
            }
            n.attributes.forEach { (k, v) ->
                if (v.startsWith("@") &&
                    !v.startsWith("@android")
                ) {
                    refs += ResourceRef(v, "manifest", "${n.tag} ${n.a("name") ?: ""}".trim(), k)
                }
            }
            n.children.forEach(::walkManifest)
        }
        walkManifest(decoded.root)

        val styles =
            table
                .filter { it.type == "style" }
                .map { e ->
                    StyleRecord(
                        e.name,
                        e.config,
                        e.parent?.removePrefix("@")?.removePrefix("style/"),
                        e.items.map { (k, v) -> value(k, v, "style" to "style/${e.name}") },
                        themeUsers[e.name].orEmpty().sorted(),
                    )
                }.sortedWith(compareBy({ it.name }, { it.config }))

        val files = table.filter { it.value?.startsWith("res/") == true }
        val drawables = mutableListOf<DrawableRecord>()
        val layouts = mutableListOf<LayoutRecord>()
        val menus = mutableListOf<MenuRecord>()
        val navigation = mutableListOf<NavigationRecord>()
        val xml = mutableListOf<XmlConfigRecord>()
        for (e in files.distinctBy { it.value }.sortedWith(compareBy({ it.type }, { it.name }, { it.config }))) {
            val path = e.value!!
            val entry = zip.getEntry(path) ?: continue
            val size = entry.size
            val bytes by lazy { if (size in 0..MAX_FILE_BYTES) zip.getInputStream(entry).use { it.readBytes() } else null }
            val tree by lazy { if (path.endsWith(".xml")) bytes?.let(decoded.decodeXml) else null }
            val key = "${e.type}/${e.name}"
            when (e.type) {
                "drawable", "mipmap" -> drawables += drawable(e, path, size, bytes, tree)
                "layout" -> tree?.let { layouts += layout(e, path, it) { n, r -> value(n, r, "layout" to key) } }
                "menu" -> tree?.let { menus += menu(e, path, it) { n, r -> value(n, r, "menu" to key) } }
                "navigation" -> tree?.let { navigation += nav(e, path, it) { n, r -> value(n, r, "navigation" to key) } }
                "xml" -> tree?.let { xml += xmlConfig(e, path, it) { n, r -> value(n, r, "xml" to key) } }
            }
        }

        val assets =
            zip
                .entries()
                .asSequence()
                .filter { !it.isDirectory && it.name.startsWith("assets/") }
                .map { a -> asset(zip, a) }
                .sortedBy { it.path }
                .toList()

        return ResourceHarvest(
            ids = ids,
            strings = strings,
            styles = styles,
            drawables = drawables,
            layouts = layouts,
            menus = menus,
            navigation = navigation,
            assets = assets,
            xml = xml,
            refs = refs.distinct().sortedWith(compareBy({ it.ref }, { it.fromKind }, { it.from }, { it.attribute })),
        )
    }

    private fun drawable(
        e: RawResource,
        path: String,
        size: Long,
        bytes: ByteArray?,
        tree: XmlNode?,
    ): DrawableRecord {
        val format =
            when {
                path.endsWith(".9.png") -> "9-patch"
                path.endsWith(".png") -> "png"
                path.endsWith(".webp") -> "webp"
                path.endsWith(".jpg") || path.endsWith(".jpeg") -> "jpg"
                path.endsWith(".gif") -> "gif"
                path.endsWith(".xml") ->
                    when (tree?.tag) {
                        "vector" -> "vector"
                        "shape" -> "shape"
                        "selector" -> "selector"
                        "layer-list" -> "layer-list"
                        "adaptive-icon" -> "adaptive-icon"
                        "bitmap", "nine-patch" -> "bitmap"
                        "animated-vector" -> "animated-vector"
                        "ripple" -> "ripple"
                        "inset" -> "inset"
                        else -> "xml-other"
                    }
                else -> "other"
            }
        val (w, h) = bytes?.let { ImageSize.of(it) } ?: (null to null)
        return DrawableRecord(e.type, e.name, e.config, path, format, size, w, h, tree?.let(::source).orEmpty())
    }

    private fun walk(
        n: XmlNode,
        depth: Int = 0,
        visit: (XmlNode, Int) -> Unit,
    ) {
        visit(n, depth)
        n.children.forEach { walk(it, depth + 1, visit) }
    }

    private fun layout(
        e: RawResource,
        path: String,
        root: XmlNode,
        value: (String, String) -> ResValue,
    ): LayoutRecord {
        val views = mutableListOf<LayoutView>()
        val ids = mutableListOf<String>()
        val custom = sortedSetOf<String>()
        val fragments = sortedSetOf<String>()
        val includes = sortedSetOf<String>()
        walk(root) { n, d ->
            val id = n.a("id")?.removePrefix("@+")?.removePrefix("@")
            id?.let { ids += it }
            if ('.' in n.tag && !FRAMEWORK_VIEW.containsMatchIn(n.tag)) custom += n.tag
            if (n.tag == "fragment" || n.tag.endsWith("FragmentContainerView")) (n.a("name") ?: n.a("class"))?.let { fragments += it }
            if (n.tag == "include") n.a("layout")?.let { includes += it }
            if (n.tag == "view") n.a("class")?.let { if (!FRAMEWORK_VIEW.containsMatchIn(it)) custom += it }
            if (views.size <
                MAX_VIEWS
            ) {
                views +=
                    LayoutView(
                        d,
                        n.tag,
                        id,
                        n.attributes.filterKeys { it.substringAfter(':') in LAYOUT_ATTRS }.map { (k, v) ->
                            value(k, v)
                        },
                    )
            }
        }
        return LayoutRecord(
            e.name,
            e.config,
            path,
            root.tag,
            views.size,
            ids.distinct(),
            custom.toList(),
            fragments.toList(),
            includes.toList(),
            views,
            source(root),
        )
    }

    private fun menu(
        e: RawResource,
        path: String,
        root: XmlNode,
        value: (String, String) -> ResValue,
    ): MenuRecord {
        val items = mutableListOf<MenuItem>()
        walk(root) { n, d ->
            if (d == 0 && n.tag == "menu") return@walk
            val title = (n.a("title"))?.let { value("android:title", it) }
            items +=
                MenuItem(
                    d,
                    n.tag,
                    n.a("id")?.removePrefix("@+")?.removePrefix("@"),
                    title,
                    n.a("icon"),
                    n.a("showAsAction"),
                    n.a("actionViewClass"),
                )
        }
        return MenuRecord(e.name, e.config, path, items, source(root))
    }

    private fun nav(
        e: RawResource,
        path: String,
        root: XmlNode,
        value: (String, String) -> ResValue,
    ): NavigationRecord {
        val dests = mutableListOf<NavDestination>()

        fun id(raw: String?) = raw?.removePrefix("@+")?.removePrefix("@")
        walk(root) { n, d ->
            if (n.tag in setOf("action", "argument", "deepLink", "deeplink")) return@walk
            if (d == 0 && n.tag == "navigation") return@walk
            dests +=
                NavDestination(
                    id = id(n.a("id")),
                    kind = n.tag,
                    className = n.a("name"),
                    label = n.a("label")?.let { value("android:label", it) },
                    actions =
                        n.children.filter { it.tag == "action" }.map { a ->
                            NavAction(id(a.a("id")), id(a.a("destination")), id(a.a("popUpTo")))
                        },
                    deepLinks = n.children.filter { it.tag.equals("deepLink", true) }.mapNotNull { it.a("uri") ?: it.a("action") },
                    arguments =
                        n.children.filter { it.tag == "argument" }.mapNotNull { a ->
                            a.a("name")?.let {
                                it +
                                    (a.a("argType")?.let { t -> ": $t" } ?: "")
                            }
                        },
                )
        }
        return NavigationRecord(e.name, path, id(root.a("startDestination")), dests, source(root))
    }

    private fun xmlConfig(
        e: RawResource,
        path: String,
        root: XmlNode,
        value: (String, String) -> ResValue,
    ): XmlConfigRecord {
        walk(root) { n, _ -> n.attributes.forEach { (k, v) -> if (v.startsWith("@")) value(k, v) } }
        val kind =
            when (root.tag) {
                "network-security-config" -> "network-security-config"
                "full-backup-content" -> "backup-rules"
                "data-extraction-rules" -> "data-extraction-rules"
                "paths" -> "file-provider-paths"
                "locale-config" -> "locale-config"
                "shortcuts" -> "shortcuts"
                "accessibility-service" -> "accessibility-service"
                "appwidget-provider" -> "appwidget-provider"
                "searchable" -> "searchable"
                "device-admin" -> "device-admin"
                "autofill-service" -> "autofill-service"
                "sync-adapter" -> "sync-adapter"
                "account-authenticator" -> "account-authenticator"
                "input-method" -> "input-method"
                "credential-provider" -> "credential-provider"
                "automotiveApp" -> "automotive-app"
                "host-apdu-service", "offhost-apdu-service" -> "nfc-apdu-service"
                else -> if (root.tag.endsWith("PreferenceScreen")) "preferences" else root.tag
            }
        return XmlConfigRecord(e.name, e.config, path, root.tag, kind, XmlSummaries.of(kind, root), source(root))
    }

    private fun asset(
        zip: ZipFile,
        a: java.util.zip.ZipEntry,
    ): AssetRecord {
        val ext =
            a.name
                .substringAfterLast('/')
                .substringAfterLast('.', "")
                .lowercase()
        val head = runCatching { zip.getInputStream(a).use { it.readNBytes(4) } }.getOrDefault(ByteArray(0))
        val magic =
            when {
                head.size >= 4 &&
                    head[0] == 'd'.code.toByte() &&
                    head[1] == 'e'.code.toByte() &&
                    head[2] == 'x'.code.toByte() &&
                    head[3] == 0x0a.toByte() -> "dex"
                head.size >= 4 &&
                    head[0] == 0x7f.toByte() &&
                    head[1] == 'E'.code.toByte() &&
                    head[2] == 'L'.code.toByte() &&
                    head[3] == 'F'.code.toByte() -> "elf"
                head.size >= 4 &&
                    head[0] == 'P'.code.toByte() &&
                    head[1] == 'K'.code.toByte() &&
                    head[2] == 3.toByte() &&
                    head[3] == 4.toByte() -> "zip"
                else -> null
            }
        val kind =
            when (ext) {
                "js", "bundle", "hbc", "mjs" -> "js"
                "json" -> "json"
                "html", "htm", "css" -> "html"
                "ttf", "otf", "woff", "woff2" -> "font"
                "png", "jpg", "jpeg", "webp", "gif", "svg" -> "image"
                "mp3", "ogg", "wav", "m4a", "aac", "opus" -> "audio"
                "mp4", "webm", "mkv" -> "video"
                "db", "sqlite", "realm" -> "db"
                "tflite", "onnx", "pb", "bin", "mlmodel", "pt" -> "model"
                "pem", "crt", "cer", "der", "p12", "bks", "jks", "keystore" -> "certificate"
                "so" -> "native"
                "dex", "odex", "vdex" -> "dex"
                "jar", "apk", "zip", "aar", "gz", "xz", "7z", "tar" -> "archive"
                "txt", "xml", "properties", "yaml", "yml", "csv", "md", "cfg", "conf", "ini", "toml" -> "text"
                else -> "other"
            }
        val announced = kind == "dex" || kind == "native" || kind == "archive"
        // Many formats are zip containers (office files, Firefox's omni.ja, Lottie, map tiles): not hidden code.
        val zipFormat = magic == "zip" && ext in ZIP_FORMATS
        val text =
            if (kind in TEXT_ASSETS && a.size in 0..MAX_ASSET_TEXT) {
                runCatching { zip.getInputStream(a).use { it.readBytes() } }
                    .getOrNull()
                    ?.let { b ->
                        runCatching {
                            Charsets.UTF_8
                                .newDecoder()
                                .decode(java.nio.ByteBuffer.wrap(b))
                                .toString()
                        }.getOrNull()
                    }
            } else {
                null
            }
        return AssetRecord(a.name, a.size, kind, magic, magic != null && !announced && !zipFormat, text)
    }

    /** The decoded file as readable XML, for the "Source" block. */
    private fun source(root: XmlNode) = ManifestXml.render(root).trimEnd()
}

/** An attribute by local name: `android:x`, `app:x` or a bare `x` (aapt2 resource shrinking can drop namespaces). */
internal fun XmlNode.a(name: String): String? = attributes["android:$name"] ?: attributes["app:$name"] ?: attributes[name]

/** Width and height from image headers (PNG, WebP), without decoding pixels. */
internal object ImageSize {
    fun of(b: ByteArray): Pair<Int?, Int?> {
        fun u16le(i: Int) = (b[i].toInt() and 0xff) or ((b[i + 1].toInt() and 0xff) shl 8)

        fun u24le(i: Int) = u16le(i) or ((b[i + 2].toInt() and 0xff) shl 16)

        fun u32be(i: Int) =
            ((b[i].toInt() and 0xff) shl 24) or ((b[i + 1].toInt() and 0xff) shl 16) or ((b[i + 2].toInt() and 0xff) shl 8) or
                (b[i + 3].toInt() and 0xff)
        if (b.size >= 24 &&
            b[1] == 'P'.code.toByte() &&
            b[2] == 'N'.code.toByte() &&
            b[3] == 'G'.code.toByte()
        ) {
            return u32be(16) to u32be(20)
        }
        if (b.size >= 30 && String(b, 0, 4, Charsets.US_ASCII) == "RIFF" && String(b, 8, 4, Charsets.US_ASCII) == "WEBP") {
            return when (String(b, 12, 4, Charsets.US_ASCII)) {
                "VP8X" -> (u24le(24) + 1) to (u24le(27) + 1)
                "VP8L" -> {
                    val bits =
                        (b[21].toInt() and 0xff) or ((b[22].toInt() and 0xff) shl 8) or ((b[23].toInt() and 0xff) shl 16) or
                            ((b[24].toInt() and 0xff) shl 24)
                    ((bits and 0x3fff) + 1) to (((bits shr 14) and 0x3fff) + 1)
                }
                "VP8 " -> (u16le(26) and 0x3fff) to (u16le(28) and 0x3fff)
                else -> null to null
            }
        }
        return null to null
    }
}

/** The facts that matter for each XML config kind. */
internal object XmlSummaries {
    fun of(
        kind: String,
        root: XmlNode,
    ): Map<String, String> {
        val out = linkedMapOf<String, String>()

        fun all(tag: String): List<XmlNode> {
            val found = mutableListOf<XmlNode>()

            fun w(n: XmlNode) {
                if (n.tag == tag) found += n
                n.children.forEach(::w)
            }
            w(root)
            return found
        }
        when (kind) {
            "network-security-config" -> {
                val base = root.children.firstOrNull { it.tag == "base-config" }
                out["base-cleartext"] = base?.a("cleartextTrafficPermitted") ?: "default"
                val baseAnchors =
                    base
                        ?.children
                        ?.filter { it.tag == "trust-anchors" }
                        ?.flatMap { it.children }
                        ?.mapNotNull {
                            it.a(
                                "src",
                            )
                        }.orEmpty()
                if ("user" in baseAnchors) out["user-certs"] = "trusted for every domain"
                val domains = all("domain-config")
                out["domain-configs"] = domains.size.toString()
                val clear =
                    domains
                        .filter { it.a("cleartextTrafficPermitted") == "true" }
                        .flatMap { d -> d.children.filter { it.tag == "domain" }.mapNotNull { it.attributes["#text"] } }
                if (clear.isNotEmpty()) out["cleartext-domains"] = clear.joinToString()
                val pinned = all("pin-set").size
                if (pinned >
                    0
                ) {
                    out["pinned-domains"] =
                        domains
                            .filter { d ->
                                d.children.any { it.tag == "pin-set" }
                            }.flatMap { d ->
                                d.children.filter { it.tag == "domain" }.mapNotNull { it.attributes["#text"] }
                            }.joinToString()
                }
                out["pin-sets"] = all("pin-set").size.toString()
                val anchors = all("certificates").mapNotNull { it.a("src") }.distinct()
                out["trust-anchors"] = anchors.joinToString().ifEmpty { "default" }
                if ("user" in anchors) out["user-certs"] = "trusted"
                if (root.children.any { it.tag == "debug-overrides" }) out["debug-overrides"] = "yes"
            }
            "file-provider-paths" -> root.children.forEach { c -> out[c.a("name") ?: c.tag] = c.tag + ":" + (c.a("path") ?: ".") }
            "backup-rules", "data-extraction-rules" ->
                all("include").plus(all("exclude")).forEachIndexed { i, r ->
                    out["${r.tag} ${i + 1}"] = (r.a("domain") ?: "?") + ":" + (r.a("path") ?: ".")
                }
            "locale-config" -> out["locales"] = all("locale").mapNotNull { it.a("name") }.joinToString()
            "shortcuts" -> out["shortcuts"] = all("shortcut").size.toString()
            "accessibility-service" ->
                root.attributes
                    .filterKeys {
                        it.endsWith("canRetrieveWindowContent") ||
                            it.endsWith("accessibilityEventTypes") ||
                            it.endsWith("canPerformGestures")
                    }.forEach { (k, v) -> out[k.substringAfter(':')] = v }
            else -> out["root"] = root.tag
        }
        return out
    }
}
