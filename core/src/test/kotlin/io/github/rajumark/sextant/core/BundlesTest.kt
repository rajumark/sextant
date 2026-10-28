package io.github.rajumark.sextant.core

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Split bundles in the three common layouts. No store bundle is in the corpus, so each bundle here packs real corpus
 * APKs: the base is a real APK as downloaded (signature intact), and the splits carry real files from real APKs
 * (native libraries, dex) the way bundletool, APKPure and APKMirror split them.
 */
class BundlesTest {
    private val temps = mutableListOf<Path>()

    @AfterEach
    fun cleanUp() {
        temps.forEach { it.toFile().deleteRecursively() }
    }

    private fun tempDir() = Files.createTempDirectory("sextant-bundle-test").also { temps.add(it) }

    /** A zip holding the given entries, each copied from (zip, entry name) or raw bytes. */
    private fun zip(
        to: Path,
        entries: List<Pair<String, ByteArray>>,
    ): Path {
        ZipOutputStream(Files.newOutputStream(to)).use { out ->
            entries.forEach { (name, bytes) ->
                out.putNextEntry(ZipEntry(name))
                out.write(bytes)
                out.closeEntry()
            }
        }
        return to
    }

    private fun entries(
        apk: Path,
        keep: (String) -> Boolean,
    ): List<Pair<String, ByteArray>> =
        ZipFile(apk.toFile()).use { z ->
            z
                .entries()
                .asSequence()
                .filter {
                    !it.isDirectory &&
                        keep(
                            it.name,
                        )
                }.map { it.name to z.getInputStream(it).readAllBytes() }
                .toList()
        }

    @Test
    fun `split names are classified`() {
        assertEquals("abi", Bundles.kindOf("config.arm64_v8a.apk"))
        assertEquals("abi", Bundles.kindOf("split_config.armeabi_v7a.apk"))
        assertEquals("density", Bundles.kindOf("splits/base-xxhdpi.apk"))
        assertEquals("language", Bundles.kindOf("config.fr.apk"))
        assertEquals("feature", Bundles.kindOf("splits/camera-master.apk"))
        assertNull(Bundles.configOf("base.apk"))
    }

    @Test
    fun `a plain APK is used as is`() {
        val apk = CorpusApks.require("binaryeye")
        val input = Bundles.open(apk)
        assertEquals(apk, input.apk)
        assertNull(input.bundle)
    }

    @Test
    fun `apks, xapk and apkm bundles read like the APK they were split from`() {
        val termux = CorpusApks.require("termux")
        val plain = Extractor.extract(termux)
        val abis = plain.meta.layout.nativeAbis
        assertTrue(abis.size > 1, "termux ships several ABIs")
        val dir = tempDir()
        // Base without native code; one split per ABI holding that ABI's lib/ files, as bundletool splits them.
        val base = zip(dir.resolve("base.apk"), entries(termux) { !it.startsWith("lib/") })
        val abiSplits = abis.associateWith { abi -> entries(termux) { it.startsWith("lib/$abi/") } }
        val baseBytes = Files.readAllBytes(base)

        fun cfg(abi: String) = abi.replace('-', '_')
        val layouts =
            mapOf(
                "apks" to listOf("splits/base-master.apk" to baseBytes) +
                    abiSplits.map { (abi, files) -> "splits/base-${cfg(abi)}.apk" to zipBytes(dir, files) },
                "xapk" to listOf("manifest.json" to """{"package_name":"com.termux"}""".toByteArray(), "com.termux.apk" to baseBytes) +
                    abiSplits.map { (abi, files) -> "config.${cfg(abi)}.apk" to zipBytes(dir, files) },
                "apkm" to listOf("info.json" to "{}".toByteArray(), "base.apk" to baseBytes) +
                    abiSplits.map { (abi, files) -> "split_config.${cfg(abi)}.apk" to zipBytes(dir, files) },
            )
        val cache = tempDir()
        System.setProperty("sextant.cache", cache.toString())
        try {
            for ((format, files) in layouts) {
                val bundle = zip(dir.resolve("termux.$format"), files)
                val e = Extractor.extract(bundle)
                val info = e.meta.bundle!!.also { assertNotNull(it, format) }
                assertEquals(format, info.format)
                assertEquals(abis.size, info.splits.size, format)
                assertTrue(info.splits.all { it.kind == "abi" }, format)
                assertEquals("termux.$format", e.meta.file.name)
                assertEquals(plain.meta.`package`, e.meta.`package`, format)
                assertEquals(abis, e.meta.layout.nativeAbis, format)
                assertEquals(plain.meta.layout.dexFiles, e.meta.layout.dexFiles, format)
                assertEquals(plain.components.map { it.name }, e.components.map { it.name }, format)
                assertEquals(plain.permissions.map { it.name }, e.permissions.map { it.name }, format)
                assertEquals(plain.meta.code?.classes, e.meta.code?.classes, format)
                // Second open hits the cache and gives the same merged file.
                assertEquals(Bundles.open(bundle).apk, Bundles.open(bundle).apk)
            }
        } finally {
            System.clearProperty("sextant.cache")
        }
    }

    @Test
    fun `feature split code is merged and the base signature is checked as shipped`() {
        val signal = CorpusApks.require("signal")
        val feature = CorpusApks.require("binaryeye")
        val dir = tempDir()
        val featureSplit = zipBytes(dir, entries(feature) { it == "classes.dex" })
        val bundle =
            zip(
                dir.resolve("signal.apks"),
                listOf("base.apk" to Files.readAllBytes(signal), "splits/scanner-master.apk" to featureSplit),
            )
        System.setProperty("sextant.cache", tempDir().toString())
        try {
            val plain = Extractor.extract(signal)
            val e = Extractor.extract(bundle)
            assertEquals(
                "feature",
                e.meta.bundle!!
                    .splits
                    .single()
                    .kind,
            )
            assertEquals(plain.meta.layout.dexFiles + 1, e.meta.layout.dexFiles)
            assertTrue(e.meta.code!!.classes > plain.meta.code!!.classes, "feature classes are counted")
            assertEquals(plain.meta.signing, e.meta.signing, "signature read from the untouched base APK")
            assertTrue(SourceDecompiler.decompile(bundle, "Lyi0;").code.contains("class yi0"), "source reads feature code")
        } finally {
            System.clearProperty("sextant.cache")
        }
    }

    private fun zipBytes(
        dir: Path,
        files: List<Pair<String, ByteArray>>,
    ): ByteArray {
        val f = Files.createTempFile(dir, "split", ".apk")
        zip(f, files)
        return Files.readAllBytes(f)
    }
}
