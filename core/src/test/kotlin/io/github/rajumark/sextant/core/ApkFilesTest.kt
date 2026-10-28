package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.ApkFile
import io.github.rajumark.sextant.core.model.Signing
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ApkFilesTest {
    @Test
    fun `types come from the path`() {
        ApkFiles.type("AndroidManifest.xml") shouldBe "manifest"
        ApkFiles.type("resources.arsc") shouldBe "resource-table"
        ApkFiles.type("classes.dex") shouldBe "dex"
        ApkFiles.type("classes12.dex") shouldBe "dex"
        ApkFiles.type("res/BW.xml") shouldBe "resource"
        ApkFiles.type("lib/arm64-v8a/libfoo.so") shouldBe "native-library"
        ApkFiles.type("assets/dexopt/baseline.prof") shouldBe "asset"
        ApkFiles.type("META-INF/MANIFEST.MF") shouldBe "signature"
        ApkFiles.type("META-INF/CERT.RSA") shouldBe "signature"
        ApkFiles.type("META-INF/BNDLTOOL.SF") shouldBe "signature"
        ApkFiles.type("stamp-cert-sha256") shouldBe "signature"
        ApkFiles.type("META-INF/androidx.core_core.version") shouldBe "library-version"
        ApkFiles.type("META-INF/services/kotlinx.coroutines.CoroutineExceptionHandler") shouldBe "service-loader"
        ApkFiles.type("kotlin/collections/collections.kotlin_builtins") shouldBe "kotlin-metadata"
        ApkFiles.type("META-INF/app_release.kotlin_module") shouldBe "kotlin-metadata"
        ApkFiles.type("META-INF/version-control-info.textproto") shouldBe "meta-inf"
        ApkFiles.type("countrycodes.csv") shouldBe "other"
    }

    @Test
    fun `content comes from the first bytes`() {
        fun magic(vararg bytes: Int) =
            ApkFiles.magic(ByteArray(16).also { b -> bytes.forEachIndexed { i, v -> b[i] = v.toByte() } }, bytes.size)

        fun magic(text: String) = text.toByteArray().let { ApkFiles.magic(it.copyOf(16), minOf(it.size, 16)) }
        magic("dex\n035\u0000") shouldBe "dex"
        magic(0x7f, 'E'.code, 'L'.code, 'F'.code) shouldBe "elf"
        magic('P'.code, 'K'.code, 3, 4) shouldBe "zip"
        magic(0x89, 'P'.code, 'N'.code, 'G'.code) shouldBe "png"
        magic("RIFF\u0000\u0000\u0000\u0000WEBP") shouldBe "webp"
        magic(0xff, 0xd8, 0xff, 0xe0) shouldBe "jpeg"
        magic(3, 0, 8, 0) shouldBe "binary-xml"
        magic(2, 0, 12, 0) shouldBe "resource-table"
        magic(0, 1, 0, 0) shouldBe "font"
        magic("<?xml version") shouldBe "xml"
        magic("  {\"a\": 1}") shouldBe "json"
        magic("de,Germany") shouldBe "text"
        magic(0, 0, 0, 7, 1, 2) shouldBe null
        ApkFiles.magic(ByteArray(16), 0) shouldBe null
    }

    @Test
    fun `decoders mark only what they produced, and signature files only for v1`() {
        fun file(
            path: String,
            type: String,
        ) = ApkFile(path, type, 1, 1, "deflated", "00000000", null, null)
        val files =
            listOf(
                file("AndroidManifest.xml", "manifest"),
                file("META-INF/CERT.RSA", "signature"),
                file("kotlin/a.kotlin_builtins", "kotlin-metadata"),
                file("stamp-cert-sha256", "signature"),
            )
        val decoders =
            ApkFiles.decoders(
                null,
                false,
                null,
                io.github.rajumark.sextant.core.model
                    .ResourceHarvest(),
                null,
            )
        ApkFiles.markDecoded(files, decoders, Signing(true, listOf("v2"), emptyList())).map { it.decodedBy } shouldBe
            listOf("manifest", null, null, null)
        ApkFiles.markDecoded(files, decoders, Signing(true, listOf("v1", "v2"), emptyList())).map { it.decodedBy } shouldBe
            listOf("manifest", "signing", null, null)
    }
}
