package io.github.rajumark.sextant.core.insights

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Findings as SARIF 2.1.0, the format GitHub code scanning, GitLab and most security dashboards import.
 * One result per finding; each evidence item becomes a location. An APK has no source files, so locations point at
 * the part of the APK the evidence lives in (AndroidManifest.xml, resources.arsc, or the class as a smali path, the
 * layout `sextant smali` and baksmali use), with the exact component, method or resource as a logical location.
 */
object Sarif {
    const val SCHEMA = "https://json.schemastore.org/sarif-2.1.0.json"
    const val RULES_URI = "https://rajumark.github.io/sextant/cli/reference/findings/"

    /** SARIF level per severity. */
    fun level(severity: String) =
        when (severity) {
            "high" -> "error"
            "medium" -> "warning"
            else -> "note"
        }

    /** GitHub's numeric `security-severity` (≥ 7 high, ≥ 4 medium, > 0 low). */
    fun securitySeverity(severity: String) =
        when (severity) {
            "high" -> "8.0"
            "medium" -> "5.0"
            "low" -> "3.0"
            else -> "1.0"
        }

    fun build(
        findings: List<Finding>,
        toolVersion: String,
        apkName: String?,
    ): JsonObject =
        buildJsonObject {
            put("\$schema", SCHEMA)
            put("version", "2.1.0")
            putJsonArray("runs") {
                addJsonObject {
                    putJsonObject("tool") {
                        putJsonObject("driver") {
                            put("name", "Sextant")
                            put("version", toolVersion)
                            put("semanticVersion", toolVersion)
                            put("informationUri", "https://rajumark.github.io/sextant/cli/")
                            put("rules", rules(findings))
                        }
                    }
                    if (apkName != null) {
                        putJsonArray("artifacts") { addJsonObject { putJsonObject("location") { put("uri", apkName) } } }
                    }
                    put("results", results(findings))
                }
            }
        }

    fun encode(sarif: JsonObject): String = PRETTY.encodeToString(JsonObject.serializer(), sarif)

    private fun rules(findings: List<Finding>): JsonArray =
        buildJsonArray {
            findings.distinctBy { it.id }.sortedBy { it.id }.forEach { f ->
                addJsonObject {
                    put("id", f.id)
                    put("name", f.id.split('-').joinToString("") { it.replaceFirstChar(Char::uppercase) })
                    putJsonObject("shortDescription") { put("text", f.title) }
                    putJsonObject("fullDescription") { put("text", f.detail) }
                    put("helpUri", RULES_URI)
                    putJsonObject("help") { put("text", "${f.detail} Static observation to review, not a proven vulnerability.") }
                    putJsonObject("defaultConfiguration") { put("level", level(f.severity)) }
                    putJsonObject("properties") {
                        putJsonArray("tags") {
                            add("security")
                            add("android")
                        }
                        put("security-severity", securitySeverity(f.severity))
                        put("sextant-severity", f.severity)
                    }
                }
            }
        }

    private fun results(findings: List<Finding>): JsonArray {
        val ruleIndex =
            findings
                .map { it.id }
                .distinct()
                .sorted()
                .withIndex()
                .associate { it.value to it.index }
        return buildJsonArray {
            for (f in findings) {
                addJsonObject {
                    put("ruleId", f.id)
                    put("ruleIndex", ruleIndex.getValue(f.id))
                    put("level", level(f.severity))
                    putJsonObject("message") {
                        val places = if (f.count == 1) "1 place" else "${f.count} places"
                        put("text", "${f.title} ($places). ${f.detail}")
                    }
                    // Code scanning needs at least one location; a finding without evidence points at the manifest.
                    val evidence = f.evidence.ifEmpty { listOf(FindingEvidence("manifest", "application")) }
                    putJsonArray("locations") { evidence.forEach { add(location(it)) } }
                    putJsonObject("partialFingerprints") {
                        put("sextant/v1", "${f.id}:${evidence.first().kind}:${evidence.first().where}")
                    }
                    putJsonObject("properties") {
                        put("count", f.count)
                        put("severity", f.severity)
                    }
                }
            }
        }
    }

    private fun location(e: FindingEvidence): JsonObject =
        buildJsonObject {
            putJsonObject("physicalLocation") {
                putJsonObject("artifactLocation") { put("uri", artifact(e)) }
            }
            putJsonArray("logicalLocations") {
                addJsonObject {
                    put("fullyQualifiedName", e.where)
                    put("kind", logicalKind(e))
                }
            }
            if (e.value != null) {
                putJsonObject("message") { put("text", e.value) }
            }
        }

    /** Where in the APK the evidence lives, as a relative URI. */
    fun artifact(e: FindingEvidence): String =
        when {
            e.kind == "manifest" -> "AndroidManifest.xml"
            e.kind == "resource" -> "resources.arsc"
            e.where.startsWith("L") && ';' in e.where -> "smali/" + e.where.substring(1, e.where.indexOf(';')) + ".smali"
            else -> "classes.dex"
        }

    private fun logicalKind(e: FindingEvidence) =
        when {
            e.kind == "manifest" -> "element"
            e.kind == "resource" -> "resource"
            "->" in e.where -> "function"
            else -> "type"
        }

    private val PRETTY =
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
        }
}
