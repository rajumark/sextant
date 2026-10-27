package io.github.rajumark.sextant.desktop

import androidx.compose.ui.graphics.Color
import io.github.rajumark.sextant.desktop.data.MemorySettings
import io.github.rajumark.sextant.desktop.data.parseHexColor
import io.github.rajumark.sextant.desktop.data.toHex
import io.github.rajumark.sextant.desktop.theme.DefaultSeed
import io.github.rajumark.sextant.desktop.theme.SeedPresets
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SeedColorTest {
    @Test
    fun `hex parses with or without hash and rejects junk`() {
        assertEquals("3F51B5", parseHexColor("#3f51b5")!!.toHex())
        assertEquals("3F51B5", parseHexColor("3F51B5")!!.toHex())
        assertNull(parseHexColor("3F51B"))
        assertNull(parseHexColor("zzzzzz"))
        assertNull(parseHexColor(""))
    }

    @Test
    fun `presets are distinct and the default is one of them`() {
        assertEquals(SeedPresets.size, SeedPresets.map { it.second }.toSet().size)
        assertEquals(DefaultSeed, SeedPresets.first().second)
    }

    @Test
    fun `changing the seed updates the state and is remembered`() {
        val settings = MemorySettings()
        val app =
            io.github.rajumark.sextant.desktop.AppState(
                repo =
                    io.github.rajumark.sextant.desktop.data
                        .FakeRepository(),
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
                settings = settings,
            )
        assertEquals(DefaultSeed, app.seed)
        val indigo = Color(0xFF3F51B5)
        app.changeSeed(indigo)
        assertEquals(indigo, app.seed)
        assertEquals("3F51B5", settings.seedHex())
        app.changeSeed(null)
        assertEquals(DefaultSeed, app.seed)
        assertNull(settings.seedHex())
    }
}
