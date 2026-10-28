package io.github.rajumark.sextant.core

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.nio.file.Files

/** `grep` over decompiled code, ProGuard/R8 mappings and the obfuscation score, on real corpus APKs. */
class CodeSearchTest {
    private val cache = Files.createTempDirectory("sextant-code-cache")

    @AfterEach
    fun cleanUp() {
        System.clearProperty("sextant.cache")
        cache.toFile().deleteRecursively()
    }

    // BinaryEye is R8-obfuscated: yi0 is kotlinx.serialization's JsonTreeDecoder, qi0 JsonObject, m91 SerialDescriptor.
    private val mapping =
        ProguardMapping.parse(
            """
            # compiler: R8
            kotlinx.serialization.json.internal.JsonTreeDecoder -> yi0:
                kotlinx.serialization.json.JsonObject value -> k
                int position -> m
                1:3:java.lang.String elementName(kotlinx.serialization.descriptors.SerialDescriptor,int):10:12 -> S
                2:2:void kotlinx.serialization.Inlined.call():5:5 -> S
            kotlinx.serialization.json.JsonObject -> qi0:
            kotlinx.serialization.descriptors.SerialDescriptor -> m91:
            """.trimIndent().lines(),
        )

    @Test
    fun `mapping members are keyed by obfuscated dex signatures`() {
        mapping.original("yi0") shouldBe "kotlinx.serialization.json.internal.JsonTreeDecoder"
        mapping.obfuscated("kotlinx.serialization.json.JsonObject") shouldBe "qi0"
        mapping.fields.getValue("yi0") shouldBe mapOf("k:Lqi0;" to "value", "m:I" to "position")
        // Inlined frames (dotted names) are not members of the class.
        mapping.methods.getValue("yi0") shouldBe mapOf("S(Lm91;I)Ljava/lang/String;" to "elementName")
        mapping.lookup("JsonTree").first() shouldBe "kotlinx.serialization.json.internal.JsonTreeDecoder -> yi0"
    }

    @Test
    fun `grep finds code that the index can't, with method and line, and the cache gives the same answer`() {
        System.setProperty("sextant.cache", cache.toString())
        val apk = CorpusApks.require("binaryeye")
        val first = CodeGrep.search(apk, Regex(Regex.escape("V().X")))
        first.hits.map { it.method } shouldContainExactly
            listOf("Lyi0;->S(Lm91;I)Ljava/lang/String;", "Lyi0;->S(Lm91;I)Ljava/lang/String;", "Lyi0;->c(Lm91;)V")
        first.truncated shouldBe false
        Files.isRegularFile(
            Bundles
                .cacheRoot()
                .resolve("code")
                .toFile()
                .listFiles()!!
                .single()
                .toPath()
                .resolve("classes.txt"),
        ) shouldBe
            true
        val again = CodeGrep.search(apk, Regex(Regex.escape("V().X")))
        again.hits shouldBe first.hits
        again.classesSearched shouldBe first.classesSearched
        // --package and --limit on the cached code.
        CodeGrep.search(apk, Regex("getSharedPreferences"), packagePrefix = "de.markusfisch").hits.all {
            it.classDescriptor.startsWith("Lde/markusfisch/")
        } shouldBe true
        CodeGrep.search(apk, Regex("return"), limit = 5).let {
            it.hits.size shouldBe 5
            it.truncated shouldBe true
        }
    }

    @Test
    fun `a mapping gives source and grep the original names`() {
        System.setProperty("sextant.cache", cache.toString())
        val apk = CorpusApks.require("binaryeye")
        val src = SourceDecompiler.decompile(apk, "kotlinx.serialization.json.internal.JsonTreeDecoder", mapping).code
        src shouldContain "public class JsonTreeDecoder"
        src shouldContain "public String elementName(SerialDescriptor serialDescriptor, int i)"
        src shouldContain "public final JsonObject value;"
        SourceDecompiler.decompile(apk, "yi0").code shouldNotContain "JsonTreeDecoder"
        val hits =
            CodeGrep
                .search(
                    apk,
                    Regex("JsonTreeDecoder"),
                    packagePrefix = "kotlinx.serialization.json.internal",
                    mapping = mapping,
                ).hits
        hits.any { "public class JsonTreeDecoder" in it.text } shouldBe true
    }

    @Test
    fun `obfuscation score separates renamed apps from apps that keep their names`() {
        Extractor
            .extract(CorpusApks.require("binaryeye"))
            .meta.code!!
            .obfuscation!!
            .level shouldBe "heavy"
        Extractor
            .extract(CorpusApks.require("duckduckgo"))
            .meta.code!!
            .obfuscation!!
            .level shouldBe "none"
        CodeExtractor.looksRenamed("yi0") shouldBe true
        CodeExtractor.looksRenamed("a.b.c\$d") shouldBe true
        CodeExtractor.looksRenamed("com.app.MainActivity") shouldBe false
    }
}
