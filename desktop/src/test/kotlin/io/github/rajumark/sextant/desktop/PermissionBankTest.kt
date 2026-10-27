package io.github.rajumark.sextant.desktop

import io.github.rajumark.sextant.desktop.data.PermissionBank
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.readLines
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The summary bank covers every platform permission the golden corpus declares. */
class PermissionBankTest {
    private val golden = Path.of(System.getProperty("sextant.corpus") ?: "../fixtures/external").resolveSibling("golden")

    @Test
    fun `every platform permission in the corpus has a summary`() {
        if (!golden.isDirectory()) return println("skipped: no golden folders in $golden")
        val missing = mutableSetOf<String>()
        Files.list(golden).use { dirs ->
            dirs.filter { it.isDirectory() }.forEach { dir ->
                val file = dir.resolve("permissions.jsonl")
                if (!file.exists()) return@forEach
                file.readLines().filter { it.isNotBlank() }.forEach { line ->
                    val name = Regex("\"name\":\"([^\"]+)\"").find(line)!!.groupValues[1]
                    val custom = line.contains("\"custom\":true")
                    if (!custom && PermissionBank.summary(name) == null) missing += name
                }
            }
        }
        assertTrue(missing.isEmpty(), "no summary for: $missing")
    }

    @Test
    fun `known and health permissions get summaries, custom ones do not`() {
        assertNotNull(PermissionBank.summary("android.permission.CAMERA"))
        assertNotNull(PermissionBank.summary("android.permission.health.READ_HEART_RATE"))
        assertNull(PermissionBank.summary("com.example.app.PRIVATE_PERMISSION"))
    }
}
