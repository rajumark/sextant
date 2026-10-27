package io.github.rajumark.sextant.desktop

import io.github.rajumark.sextant.desktop.data.Insights
import io.github.rajumark.sextant.desktop.data.MiniJson
import io.github.rajumark.sextant.desktop.data.SampleData
import io.github.rajumark.sextant.desktop.data.Severity
import io.github.rajumark.sextant.desktop.model.ComponentType
import io.github.rajumark.sextant.desktop.model.ExportedSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SampleDataTest {
    private val apps = SampleData.recent

    @Test
    fun `sample apps are distinct and non-trivial`() {
        assertEquals(4, apps.size)
        assertEquals(apps.size, apps.map { it.meta.packageName }.toSet().size)
        apps.forEach { a ->
            assertTrue(a.permissions.isNotEmpty(), a.meta.label)
            assertTrue(a.components.size > 20, a.meta.label)
            assertTrue(a.launcher != null, "${a.meta.label} has a launcher")
        }
    }

    @Test
    fun `component names are unique and fully qualified`() {
        apps.forEach { a ->
            assertEquals(
                a.components.size,
                a.components
                    .map { it.name }
                    .toSet()
                    .size,
            )
            assertTrue(a.components.all { '.' in it.name && !it.name.startsWith('.') })
        }
    }

    @Test
    fun `default-exported components follow Android's rule`() {
        apps.flatMap { it.components }.filter { it.exportedSource == ExportedSource.Default }.forEach {
            assertEquals(it.intentFilters.isNotEmpty(), it.exported, it.name)
        }
    }

    @Test
    fun `data is deterministic`() {
        val again = SampleData.recent.first()
        assertEquals(
            apps
                .first()
                .meta.file.sha256,
            again.meta.file.sha256,
        )
        assertEquals(64, again.meta.file.sha256.length)
    }

    @Test
    fun `jsonl lines match the CLI schema shape`() {
        val a = apps.first()
        val line = a.permissions.first().jsonLine
        assertTrue(line.startsWith("{\"name\":\""), line)
        assertTrue("\"max_sdk\":null" in line, line)
        val comps = a.outputFiles.first { it.name == "components.jsonl" }
        val lines = comps.load().lines().filter { it.isNotEmpty() }
        assertEquals(a.components.size, lines.size)
        assertTrue(
            lines
                .filter { it.isNotEmpty() }
                .all { it.startsWith("{\"type\":\"") },
        )
    }

    @Test
    fun `pretty json keeps key order and inlines scalar lists`() {
        val json = MiniJson.pretty(linkedMapOf("b" to 1, "a" to listOf("x", "y"), "c" to null))
        assertEquals("{\n  \"b\": 1,\n  \"a\": [\"x\", \"y\"],\n  \"c\": null\n}", json)
    }

    @Test
    fun `summary lists the counts agents need`() {
        val a = apps.first()
        val summary = a.outputFiles.first { it.name == "summary.md" }.load()
        assertTrue(summary.startsWith("# ${a.meta.packageName} ${a.meta.versionName}"))
        assertTrue("${a.countOf(ComponentType.Activity)} activities" in summary)
        assertTrue(summary.lines().size < 60, "summary.md stays under 60 lines")
    }

    @Test
    fun `debuggable sample raises a risk insight`() {
        val lumen = apps.first { it.meta.flags.debuggable == true }
        assertTrue(Insights.of(lumen).any { it.severity == Severity.Risk && "Debuggable" in it.title })
    }
}
