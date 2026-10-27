package io.github.rajumark.sextant.desktop

import io.github.rajumark.sextant.desktop.data.FakeRepository
import io.github.rajumark.sextant.desktop.data.MemorySettings
import io.github.rajumark.sextant.desktop.data.SampleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecentActionsTest {
    private val sample = SampleData.recent.first()
    private val dir = sample.outputDir

    private fun app(
        repo: FakeRepository,
        settings: MemorySettings,
    ): AppState =
        AppState(repo, CoroutineScope(Dispatchers.Unconfined), Dispatchers.Unconfined, settings = settings).also { it.loadRecent() }

    @Test
    fun `tags are trimmed, de-duplicated, capped and cleared`() {
        val settings = MemorySettings()
        val state = app(FakeRepository(), settings)
        state.setTags(dir, listOf("  review ", "review", "", "exported"))
        assertEquals(listOf("review", "exported"), state.tags[dir])
        assertEquals(listOf("review", "exported"), settings.tags(dir))
        state.setTags(dir, (1..12).map { "t$it" })
        assertEquals(8, state.tags[dir]!!.size)
        state.setTags(dir, emptyList())
        assertTrue(dir !in state.tags, "no entry means no chip")
        assertEquals(emptyList(), settings.tags(dir))
    }

    @Test
    fun `tags come back when recent is loaded again`() {
        val settings = MemorySettings()
        app(FakeRepository(), settings).setTags(dir, listOf("check services"))
        val reopened = app(FakeRepository(), settings)
        assertEquals(listOf("check services"), reopened.tags[dir])
    }

    @Test
    fun `delete removes the folder through the repository, the recent entry and its tags`() {
        val repo = FakeRepository()
        val settings = MemorySettings()
        val state = app(repo, settings)
        state.setTags(dir, listOf("keep"))
        state.deleteAnalysis(sample)
        assertEquals(listOf(Path.of(dir)), repo.deleted)
        assertTrue(state.recent.none { it.outputDir == dir })
        assertTrue(dir !in state.tags)
        assertEquals(emptyList(), settings.tags(dir))
    }
}
