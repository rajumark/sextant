package io.github.rajumark.sextant.desktop.data

import io.github.rajumark.sextant.desktop.model.Analysis
import io.github.rajumark.sextant.desktop.model.AppComponent
import io.github.rajumark.sextant.desktop.model.ComponentType
import io.github.rajumark.sextant.desktop.model.OutputFile
import io.github.rajumark.sextant.desktop.model.OutputFormat
import io.github.rajumark.sextant.desktop.model.Permission

/** Renders the five base files `sextant extract` writes, for the sample analyses. */
object SampleOutput {
    private fun text(
        name: String,
        description: String,
        format: OutputFormat,
        content: String,
    ) = OutputFile(name, description, format, content.encodeToByteArray().size.toLong()) { content }

    fun files(a: Analysis): List<OutputFile> =
        listOf(
            text("summary.md", "Overview for people and agents. Read this first.", OutputFormat.Markdown, summary(a)),
            text("meta.json", "Identity, signing, flags and APK layout.", OutputFormat.Json, MiniJson.pretty(meta(a)) + "\n"),
            text(
                "manifest.json",
                "The full decoded manifest tree.",
                OutputFormat.Json,
                ManifestFormat.json(a.manifest) + "\n",
            ),
            text(
                "permissions.jsonl",
                "One permission per line.",
                OutputFormat.JsonLines,
                a.permissions.joinToString("") {
                    permissionLine(it) +
                        "\n"
                },
            ),
            text(
                "components.jsonl",
                "One component per line.",
                OutputFormat.JsonLines,
                a.components.joinToString("") {
                    componentLine(it) +
                        "\n"
                },
            ),
        )

    fun permissionLine(p: Permission): String =
        MiniJson.compact(
            linkedMapOf(
                "name" to p.name,
                "kind" to p.kind.wire,
                "max_sdk" to p.maxSdk,
                "dangerous" to p.dangerous,
                "protection_level" to p.protectionLevel,
                "custom" to p.custom,
            ),
        )

    fun componentLine(c: AppComponent): String =
        MiniJson.compact(
            linkedMapOf(
                "type" to c.type.wire,
                "name" to c.name,
                "exported" to c.exported,
                "exported_source" to c.exportedSource.wire,
                "enabled" to c.enabled,
                "permission" to c.permission,
                "process" to c.process,
                "launcher" to c.launcher,
                "target_activity" to c.targetActivity,
                "authorities" to c.authorities,
                "intent_filters" to
                    c.intentFilters.map { f ->
                        linkedMapOf(
                            "actions" to f.actions,
                            "categories" to f.categories,
                            "data" to
                                f.data.map { d ->
                                    linkedMapOf(
                                        "scheme" to d.scheme,
                                        "host" to d.host,
                                        "port" to d.port,
                                        "path" to d.path,
                                        "path_prefix" to d.pathPrefix,
                                        "path_pattern" to d.pathPattern,
                                        "mime_type" to d.mimeType,
                                    )
                                },
                            "priority" to f.priority,
                            "auto_verify" to f.autoVerify,
                        )
                    },
            ),
        )

    private fun meta(a: Analysis): Map<String, Any?> {
        val m = a.meta
        return linkedMapOf(
            "schema_version" to m.schemaVersion,
            "tool" to linkedMapOf("name" to "sextant", "version" to m.toolVersion),
            "file" to linkedMapOf("name" to m.file.name, "size_bytes" to m.file.sizeBytes, "sha256" to m.file.sha256),
            "package" to m.packageName,
            "version_name" to m.versionName,
            "version_code" to m.versionCode,
            "min_sdk" to m.minSdk,
            "target_sdk" to m.targetSdk,
            "compile_sdk" to m.compileSdk,
            "label" to m.label,
            "application_class" to m.applicationClass,
            "flags" to
                linkedMapOf(
                    "debuggable" to m.flags.debuggable,
                    "allow_backup" to m.flags.allowBackup,
                    "uses_cleartext_traffic" to m.flags.usesCleartextTraffic,
                    "network_security_config" to m.flags.networkSecurityConfig,
                    "extract_native_libs" to m.flags.extractNativeLibs,
                ),
            "signing" to
                linkedMapOf(
                    "schemes" to m.signing.schemes,
                    "certificates" to
                        m.signing.certificates.map {
                            linkedMapOf(
                                "sha256" to it.sha256,
                                "sha1" to it.sha1,
                                "subject" to it.subject,
                                "issuer" to it.issuer,
                                "not_before" to it.notBefore,
                                "not_after" to it.notAfter,
                            )
                        },
                ),
            "layout" to linkedMapOf("dex_files" to m.layout.dexFiles, "native_abis" to m.layout.nativeAbis),
            "warnings" to m.warnings,
        )
    }

    private fun summary(a: Analysis): String {
        val m = a.meta

        fun yesNo(b: Boolean?) =
            when (b) {
                true -> "yes"
                false -> "no"
                null -> "not set"
            }
        val exportedLinks = a.components.filter { it.exported && it.deepLinks.isNotEmpty() }
        return buildString {
            appendLine("# ${m.packageName} ${m.versionName} (${m.versionCode})")
            appendLine(
                "Extracted by sextant ${m.toolVersion} · schema ${m.schemaVersion} · sha256 ${m.file.sha256.take(
                    4,
                )}…${m.file.sha256.takeLast(4)}",
            )
            appendLine()
            appendLine("## App")
            appendLine(
                "- min SDK ${m.minSdk} · target SDK ${m.targetSdk} · debuggable: ${yesNo(
                    m.flags.debuggable,
                )} · allow_backup: ${yesNo(m.flags.allowBackup)}",
            )
            appendLine("- Launcher: ${a.launcher?.name ?: "none"}")
            appendLine(
                "- Signing: ${m.signing.schemes.joinToString(
                    ", ",
                )} · cert sha256 ${m.signing.certificates
                    .firstOrNull()
                    ?.sha256
                    ?.let { it.take(5) + "…" + it.takeLast(5) } ?: "none"}",
            )
            appendLine()
            appendLine("## Counts")
            appendLine("- Permissions: ${a.requestedCount} requested (${a.dangerousCount} dangerous), ${a.declaredCount} declared")
            appendLine(
                "- Components: ${a.countOf(ComponentType.Activity)} activities, ${a.countOf(ComponentType.Service)} services, " +
                    "${a.countOf(ComponentType.Receiver)} receivers, ${a.countOf(ComponentType.Provider)} providers",
            )
            appendLine("- Exported: ${a.exportedCount} (${exportedLinks.size} with deep links)")
            appendLine()
            appendLine("## Exported components")
            a.components.filter { it.exported }.take(8).forEach { c ->
                val link = c.deepLinks.firstOrNull()?.let { " — $it" } ?: ""
                appendLine("- ${c.type.wire} ${c.name}$link")
            }
            if (a.exportedCount > 8) appendLine("- … (full list in components.jsonl)")
            appendLine()
            appendLine("## Files")
            appendLine("- meta.json — identity, signing, flags, layout")
            appendLine("- manifest.json — full decoded manifest")
            appendLine("- permissions.jsonl — one permission per line")
            appendLine("- components.jsonl — one component per line (grep \"exported\":true)")
        }
    }
}
