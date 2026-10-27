package io.github.rajumark.sextant.desktop

import io.github.rajumark.sextant.core.output.OutputFiles
import io.github.rajumark.sextant.desktop.data.AnalysisFailure
import io.github.rajumark.sextant.desktop.data.CoreRepository
import io.github.rajumark.sextant.desktop.data.MemoryStore
import io.github.rajumark.sextant.desktop.model.ComponentType
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.exists
import kotlin.io.path.readLines
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/** Runs the real core library on downloaded corpus APKs and checks the desktop mapping against what it wrote. */
class CoreRepositoryTest {
    private val corpus = Path.of(System.getProperty("sextant.corpus") ?: "../fixtures/external")

    /** Analyses write large folders (APK copy, index). Each test's folders are removed when it ends. */
    private val temps = mutableListOf<Path>()

    @AfterTest
    fun removeTempFolders() {
        temps.forEach { it.toFile().deleteRecursively() }
        temps.clear()
    }

    /** Copies a corpus APK into a fresh temp folder, so output lands beside it and not in fixtures/. */
    private fun apk(id: String): Path? {
        val source = corpus.resolve("$id.apk")
        if (!source.exists()) {
            println("skipped: $source missing (run scripts/fetch-apks.sh)")
            return null
        }
        val dir = Files.createTempDirectory("sextant-desktop-test").also { temps.add(it) }
        return source.copyTo(dir.resolve("$id.apk"))
    }

    @Test
    fun `analyze writes the CLI folder and maps it faithfully`() {
        for (id in listOf("tuner", "newpipe", "obtainium")) {
            val apk = apk(id) ?: continue
            val store = MemoryStore()
            val repo = CoreRepository(store, apk.parent.resolve("fallback"))
            val a = repo.analyze(apk)
            val dir = Path.of(a.outputDir)
            assertEquals(apk.parent.resolve("$id.sextant"), dir, id)
            OutputFiles.ALL.forEach { assertTrue(dir.resolve(it).exists(), "$id: $it written") }
            assertTrue(dir.resolve(OutputFiles.INDEX).exists(), "$id: index written")

            // Every permission and component line matches the file byte for byte.
            assertEquals(dir.resolve(OutputFiles.PERMISSIONS).readLines(), a.permissions.map { it.jsonLine }, id)
            assertEquals(dir.resolve(OutputFiles.COMPONENTS).readLines(), a.components.map { it.jsonLine }, id)
            assertNotNull(a.meta.code, "$id: code stats")
            assertNotNull(a.launcher, "$id: launcher")
            assertEquals(apk.toString(), a.apkPath)
            assertTrue(a.outputFiles.any { it.name == OutputFiles.SUMMARY }, id)
            assertEquals(listOf(dir), store.all(), "$id: remembered")

            // Reading the folder back (like `sextant info`) gives the same facts.
            val reopened = repo.open(dir)
            assertEquals(a.meta.packageName, reopened.meta.packageName)
            assertEquals(a.components.size, reopened.components.size)
            assertEquals(a.countOf(ComponentType.Activity), reopened.countOf(ComponentType.Activity))
            assertEquals(apk.toString(), reopened.apkPath, "$id: finds the APK next to its folder")
        }
    }

    @Test
    fun `analyzing twice reuses the same folder`() {
        val apk = apk("tuner") ?: return
        val repo = CoreRepository(MemoryStore(), apk.parent.resolve("fallback"))
        val first = repo.analyze(apk)
        val second = repo.analyze(apk)
        assertEquals(first.outputDir, second.outputDir)
        assertEquals(first.meta.file.sha256, second.meta.file.sha256)
    }

    @Test
    fun `a foreign folder in the way sends output to the fallback`() {
        val apk = apk("tuner") ?: return
        val blocker = apk.parent.resolve("tuner.sextant")
        Files.createDirectories(blocker)
        Files.writeString(blocker.resolve("notes.txt"), "not ours")
        val repo = CoreRepository(MemoryStore(), apk.parent.resolve("fallback"))
        assertEquals(apk.parent.resolve("fallback/tuner.sextant"), repo.outputDirFor(apk))
        assertEquals("not ours", Files.readString(blocker.resolve("notes.txt")))
    }

    @Test
    fun `bad input becomes a readable failure`() {
        val dir = Files.createTempDirectory("sextant-desktop-test").also { temps.add(it) }
        val notApk = dir.resolve("broken.apk").also { Files.writeString(it, "this is not a zip") }
        val repo = CoreRepository(MemoryStore(), dir.resolve("fallback"))
        val e = assertFailsWith<AnalysisFailure> { repo.analyze(notApk) }
        assertTrue(e.message!!.isNotBlank())
        assertFailsWith<AnalysisFailure> { repo.open(dir) }
    }
}
