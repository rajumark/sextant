package io.github.rajumark.sextant.core.output

import io.github.rajumark.sextant.core.Bundles
import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.model.AppMeta
import io.github.rajumark.sextant.core.model.Component
import io.github.rajumark.sextant.core.model.IntentFilter
import io.github.rajumark.sextant.core.model.obfuscationText
import kotlinx.serialization.Serializable

/** Compact overview of an extraction: what `info --json` prints and what summary.md is built from. */
@Serializable
data class InfoReport(
    val meta: AppMeta,
    val counts: Counts,
    val launcher: List<String>,
    val exported: List<ExportedComponent>,
)

@Serializable
data class Counts(
    val permissionsRequested: Int,
    val permissionsDangerous: Int,
    val permissionsDeclared: Int,
    val components: Map<String, Int>,
    val exported: Int,
    val withDeepLinks: Int,
)

@Serializable
data class ExportedComponent(
    val type: String,
    val name: String,
    val exportedSource: String,
    val permission: String?,
    val deepLinks: List<String>,
)

object InfoReports {
    fun of(extraction: Extraction): InfoReport {
        val perms = extraction.permissions
        val requested = perms.filter { it.kind != "declared" }
        val comps = extraction.components
        val exported = comps.filter { it.exported && it.enabled }
        return InfoReport(
            meta = extraction.meta,
            counts =
                Counts(
                    permissionsRequested = requested.map { it.name }.distinct().size,
                    permissionsDangerous =
                        requested
                            .filter { it.dangerous }
                            .map { it.name }
                            .distinct()
                            .size,
                    permissionsDeclared = perms.count { it.kind == "declared" },
                    components = TYPES.associateWith { type -> comps.count { it.type == type } },
                    exported = exported.size,
                    withDeepLinks = exported.count { deepLinks(it).isNotEmpty() },
                ),
            launcher = comps.filter { it.launcher && it.enabled }.map { it.name },
            exported = exported.map { ExportedComponent(it.type, it.name, it.exportedSource, it.permission, deepLinks(it)) },
        )
    }

    /** URL-like patterns a component answers to: `scheme://host/path*`. */
    fun deepLinks(component: Component): List<String> =
        component.intentFilters
            .flatMap(::deepLinks)
            .distinct()
            .sorted()

    /** URL-like patterns of one intent filter; empty unless it handles VIEW. */
    fun deepLinks(filter: IntentFilter): List<String> {
        if ("android.intent.action.VIEW" !in filter.actions) return emptyList()
        // Android combines every scheme/host/path declared in one filter.
        val schemes = filter.data.mapNotNull { it.scheme }.distinct()
        val hosts =
            filter.data
                .mapNotNull { it.host }
                .distinct()
                .ifEmpty { listOf(null) }
        val paths =
            filter.data
                .mapNotNull { d ->
                    d.path ?: d.pathPrefix?.let { "$it*" } ?: d.pathPattern ?: d.pathSuffix?.let { "*$it" } ?: d.pathAdvancedPattern
                }.distinct()
                .ifEmpty { listOf("") }
        return schemes.flatMap { s -> hosts.flatMap { h -> paths.map { p -> if (h == null) "$s:$p" else "$s://$h$p" } } }.distinct()
    }

    val TYPES = listOf("activity", "activity-alias", "service", "receiver", "provider")
}

/** summary.md: the first file an AI agent should read. Kept short on purpose. */
object SummaryWriter {
    private const val MAX_LISTED = 25
    private const val MAX_FINDINGS = 12

    /** [sqlite]: the app lives in one .sqlite file (the CLI), so point at tables and commands instead of files. */
    fun render(
        extraction: Extraction,
        hasIndex: Boolean = false,
        sqlite: Boolean = false,
    ): String {
        val r = InfoReports.of(extraction)
        val m = r.meta
        val c = r.counts
        return buildString {
            appendLine("# ${m.`package` ?: "(unknown package)"} ${m.versionName ?: "?"} (${m.versionCode ?: "?"})")
            appendLine("Extracted by ${m.tool.name} ${m.tool.version} · schema ${m.schemaVersion} · sha256 ${m.file.sha256}")
            appendLine()
            appendLine("## App")
            m.label?.let { appendLine("- Label: $it") }
            appendLine("- min SDK ${m.minSdk ?: "?"} · target SDK ${m.targetSdk ?: "?"} · compile SDK ${m.compileSdk ?: "?"}")
            appendLine(
                "- debuggable: ${yesNo(m.flags.debuggable)} · allow_backup: ${yesNo(m.flags.allowBackup)} · " +
                    "cleartext traffic: ${yesNo(m.flags.usesCleartextTraffic)}",
            )
            appendLine("- Launcher: ${r.launcher.joinToString().ifEmpty { "none" }}")
            m.flags.sharedUserId?.let { appendLine("- sharedUserId: $it") }
            if (m.flags.testOnly == true) appendLine("- testOnly: yes (debug/test build)")
            appendLine(
                "- Signing: ${m.signing.schemes.joinToString().ifEmpty { "none" }}" +
                    (
                        m.signing.certificates
                            .firstOrNull()
                            ?.let { " · cert sha256 ${it.sha256}" } ?: ""
                    ),
            )
            m.icons?.icon?.let { appendLine("- Icon: ${IconText.describe(it)}") }
            appendLine("- Layout: ${m.layout.dexFiles} dex · ABIs: ${m.layout.nativeAbis.joinToString().ifEmpty { "none" }}")
            m.bundle?.let { appendLine("- Bundle: ${Bundles.describe(it)}") }
            m.code?.let { code ->
                appendLine()
                appendLine("## Code")
                appendLine(
                    "- ${code.classes} classes · ${code.methods} methods · ${code.fields} fields · " +
                        "${code.kotlinClasses} Kotlin classes (${percent(code.kotlinClasses, code.classes)})",
                )
                appendLine(
                    "- ${code.callEdges} call edges (${code.callSites} call sites) · ${code.distinctStrings} distinct string constants",
                )
                code.obfuscationText()?.let { appendLine("- Obfuscation: $it") }
                appendLine(
                    "- Biggest packages: " +
                        code.topPackages.take(8).joinToString { "${it.`package`.ifEmpty { "(default)" }} (${it.classes})" },
                )
            }
            m.insights?.let { ins ->
                appendLine()
                appendLine("## Insights")
                appendLine("- Frameworks: ${ins.frameworks.joinToString().ifEmpty { "none detected" }}")
                val sdkSummary =
                    extraction.insights?.sdks?.joinToString { it.name }
                        ?: "${ins.sdks} (see ${if (sqlite) "table sdks" else "insights/sdks.jsonl"})"
                appendLine("- SDKs (${ins.sdks}): ${sdkSummary.ifEmpty { "none detected" }}")
                appendLine(
                    "- Libraries with exact versions: ${ins.libraries}${ins.androidGradlePlugin?.let { " · built with AGP $it" } ?: ""}",
                )
                appendLine(
                    "- Endpoints: ${ins.endpoints} URLs on ${ins.hosts} hosts; top: " +
                        ins.topHosts.take(6).joinToString { "${it.host} (${it.urls})" },
                )
                appendLine("- Native: ${ins.nativeLibraries} libraries, ${ins.jniMethods} JNI methods · Locales: ${ins.locales.size}")
            }
            extraction.insights?.findings?.takeIf { it.isNotEmpty() }?.let { findings ->
                appendLine()
                appendLine("## Review findings (static observations to check, not proven issues)")
                for (f in findings.take(MAX_FINDINGS)) {
                    val first = f.evidence.firstOrNull()?.let { " — e.g. ${it.where}" } ?: ""
                    appendLine("- [${f.severity}] ${f.title} (${f.count})$first")
                }
                if (findings.size >
                    MAX_FINDINGS
                ) {
                    appendLine(
                        "- … ${findings.size - MAX_FINDINGS} more in ${if (sqlite) "`sextant findings`" else "insights/findings.jsonl"}",
                    )
                }
            }
            appendLine()
            appendLine("## Counts")
            appendLine(
                "- Permissions: ${c.permissionsRequested} requested (${c.permissionsDangerous} dangerous), ${c.permissionsDeclared} declared",
            )
            appendLine("- Components: " + c.components.entries.joinToString { "${it.value} ${it.key}" })
            appendLine("- Exported: ${c.exported} (${c.withDeepLinks} with deep links)")
            if (extraction.features.isNotEmpty() || extraction.queries.isNotEmpty() || extraction.metadata.isNotEmpty()) {
                val f = extraction.features
                appendLine(
                    "- Features: ${f.count { it.kind == "feature" && it.required != false }} required, " +
                        "${f.count { it.kind == "feature" && it.required == false }} optional · " +
                        "libraries: ${f.filter { it.kind != "feature" }.joinToString { it.name }.ifEmpty { "none" }}",
                )
                appendLine(
                    "- Queries (package visibility): ${extraction.queries.count { it.kind == "package" }} packages, " +
                        "${extraction.queries.count {
                            it.kind == "intent"
                        }} intents, ${extraction.queries.count { it.kind == "provider" }} providers" +
                        " · Metadata: ${extraction.metadata.size} entries",
                )
            }
            appendLine()
            appendLine("## Exported components")
            if (r.exported.isEmpty()) appendLine("- none")
            for (e in r.exported.take(MAX_LISTED)) {
                val links = if (e.deepLinks.isEmpty()) "" else " — " + e.deepLinks.take(3).joinToString()
                val perm = e.permission?.let { " [requires $it]" } ?: ""
                appendLine("- ${e.type} ${e.name}$perm$links")
            }
            if (r.exported.size >
                MAX_LISTED
            ) {
                appendLine("- … ${r.exported.size - MAX_LISTED} more in ${if (sqlite) "`sextant components`" else "components.jsonl"}")
            }
            if (m.warnings.isNotEmpty()) {
                appendLine()
                appendLine("## Warnings")
                m.warnings.forEach { appendLine("- $it") }
            }
            appendLine()
            if (sqlite) {
                appendLine("## Query it")
                appendLine("Everything is in one SQLite file; every command takes it (or the APK):")
                appendLine(
                    "- `sextant find <app> <text>` substring search over names, strings, components, permissions, deep links, resources",
                )
                appendLine("- `sextant class <app> <name>` one class: hierarchy, fields, methods, callers, strings")
                appendLine("- `sextant xref <app> <class|field>` where it is used; `sextant reach <app> <from> <to>` call paths")
                appendLine("- `sextant res <app> <kind>`, `sextant components <app>`, `sextant files <app>` resources, components, files")
                appendLine("- `sextant sql <app> \"<query>\"` read-only SQL; `sextant sql <app> --schema` lists tables and views")
                return@buildString
            }
            appendLine("## Files")
            appendLine("- meta.json — identity, signing, flags, layout, warnings")
            appendLine("- manifest.json — full decoded manifest tree")
            appendLine("- permissions.jsonl — one permission per line")
            appendLine("- components.jsonl — one component per line (grep '\"exported\":true')")
            appendLine("- features.jsonl — uses-feature / uses-library / uses-native-library")
            appendLine("- metadata.jsonl — meta-data and property entries (application and components)")
            appendLine("- queries.jsonl — package visibility: packages, intents, providers the app can see")
            if (m.code != null) {
                appendLine("- code/classes.jsonl — one class per line (id, super, interfaces, source file, Kotlin)")
                appendLine("- code/methods.jsonl — one method per line (id like Lcom/a/B;->m(I)V)")
                appendLine("- code/fields.jsonl — one field per line, with constant initial values")
                appendLine("- code/calls.jsonl — caller → callee edges with bytecode offsets")
                appendLine("- code/strings.jsonl — string constants and the methods that load them")
                appendLine("- code/field_refs.jsonl, code/type_refs.jsonl — field reads/writes, type uses (new, cast, instanceof)")
            }
            if (m.insights != null) {
                appendLine("- insights/frameworks.jsonl, sdks.jsonl, libraries.jsonl — what the app is built with, with evidence")
                appendLine("- insights/endpoints.jsonl — every URL, with the methods/resources it comes from")
                appendLine("- insights/findings.jsonl — review findings with evidence (manifest, code, secrets)")
                appendLine("- resources/values.jsonl — default strings, bools, integers, colors, dimens")
                appendLine("- native/libraries.jsonl, native/symbols.jsonl — .so files and exported symbols (JNI mapped to Java)")
            }
            if (hasIndex) {
                appendLine("- index.sqlite — everything above, queryable:")
                appendLine("  - `sextant find <dir> <text>` substring search over names, strings, components, permissions, deep links")
                appendLine("  - `sextant class <dir> <name>` one class: hierarchy, fields, methods, callers, strings")
                appendLine("  - `sextant xref <dir> <class|field>` where it is used; `sextant reach <dir> <from> <to>` call paths")
                appendLine("  - `sextant sql <dir> \"<query>\"` read-only SQL; `sextant sql <dir> --schema` lists tables and views")
            }
        }
    }

    private fun percent(
        part: Int,
        whole: Int,
    ) = if (whole == 0) "0%" else "${part * 100 / whole}%"

    private fun yesNo(value: Boolean?) =
        when (value) {
            true -> "yes"
            false -> "no"
            null -> "not set"
        }
}

/** One-line description of an icon for summary.md and `info`. */
object IconText {
    fun describe(ref: io.github.rajumark.sextant.core.insights.IconRef): String {
        val best = ref.files.firstOrNull { it.path == ref.best }
        val size =
            best?.let {
                if (it.width !=
                    null
                ) {
                    " (${it.width}×${it.height}${it.config.takeIf { c -> c.isNotEmpty() }?.let { c -> " $c" } ?: ""})"
                } else {
                    ""
                }
            }
                ?: ""
        val adaptive =
            ref.adaptive?.let { a ->
                val fg = a.foreground?.best?.let { " fg $it" } ?: ""
                val bg = a.background?.let { l -> l.best ?: l.color }?.let { " bg $it" } ?: ""
                " · adaptive ${a.path}$fg$bg"
            } ?: ""
        return "${ref.best ?: "not found"}$size · ${ref.resource}$adaptive"
    }
}
