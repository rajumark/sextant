package io.github.rajumark.sextant.core.insights

import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class SarifTest {
    private val findings =
        listOf(
            Finding("debuggable", "high", "App is debuggable", "detail", listOf(FindingEvidence("manifest", "application")), 1),
            Finding(
                "weak-hash",
                "low",
                "Weak hash algorithm",
                "detail",
                listOf(FindingEvidence("method", "Lcom/a/B;->h([B)V", "MD5"), FindingEvidence("method", "Lcom/a/C;->h()V", "SHA-1")),
                2,
            ),
            Finding(
                "google-api-key",
                "info",
                "Google API key",
                "detail",
                listOf(FindingEvidence("resource", "@string/k", "AIzaSy…1234")),
                1,
            ),
            Finding("allow-backup", "info", "Backup", "detail", emptyList(), 1),
        )

    private val run =
        Json
            .parseToJsonElement(Sarif.encode(Sarif.build(findings, "1.2.3", "app.apk")))
            .jsonObject["runs"]!!
            .jsonArray[0]
            .jsonObject

    @Test
    fun `rules are sorted and carry level and security severity`() {
        val rules =
            run["tool"]!!
                .jsonObject["driver"]!!
                .jsonObject["rules"]!!
                .jsonArray
                .map { it.jsonObject }
        rules.map { it["id"]!!.jsonPrimitive.content } shouldBe listOf("allow-backup", "debuggable", "google-api-key", "weak-hash")
        val debuggable = rules[1]
        debuggable["defaultConfiguration"]!!.jsonObject["level"]!!.jsonPrimitive.content shouldBe "error"
        debuggable["properties"]!!.jsonObject["security-severity"]!!.jsonPrimitive.content shouldBe "8.0"
    }

    @Test
    fun `results point at the right part of the APK`() {
        val results = run["results"]!!.jsonArray.map { it.jsonObject }
        results.size shouldBe 4
        val weak = results.first { it["ruleId"]!!.jsonPrimitive.content == "weak-hash" }
        weak["ruleIndex"]!!.jsonPrimitive.int shouldBe 3
        weak["level"]!!.jsonPrimitive.content shouldBe "note"
        val uris =
            weak["locations"]!!.jsonArray.map {
                it.jsonObject["physicalLocation"]!!
                    .jsonObject["artifactLocation"]!!
                    .jsonObject["uri"]!!
                    .jsonPrimitive.content
            }
        uris shouldBe listOf("smali/com/a/B.smali", "smali/com/a/C.smali")
        // A finding without evidence still gets a location: code scanning rejects results without one.
        results.first { it["ruleId"]!!.jsonPrimitive.content == "allow-backup" }["locations"]!!.jsonArray.size shouldBe 1
        run["artifacts"]!!
            .jsonArray[0]
            .jsonObject["location"]!!
            .jsonObject["uri"]!!
            .jsonPrimitive.content shouldBe "app.apk"
    }

    @Test
    fun `artifact paths`() {
        Sarif.artifact(FindingEvidence("manifest", "com.a.Main")) shouldBe "AndroidManifest.xml"
        Sarif.artifact(FindingEvidence("resource", "@string/k")) shouldBe "resources.arsc"
        Sarif.artifact(FindingEvidence("class", "Lcom/a/B\$1;")) shouldBe "smali/com/a/B\$1.smali"
        Sarif.artifact(FindingEvidence("string", "weird")) shouldBe "classes.dex"
    }
}
