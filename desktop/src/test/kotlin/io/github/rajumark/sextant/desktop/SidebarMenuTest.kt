package io.github.rajumark.sextant.desktop

import io.github.rajumark.sextant.desktop.ui.nav.SidebarMenu
import io.github.rajumark.sextant.desktop.ui.workspace.Destination
import kotlin.test.Test
import kotlin.test.assertEquals

class SidebarMenuTest {
    @Test
    fun `every screen appears exactly once in the sidebar`() {
        assertEquals(Destination.entries.sorted(), SidebarMenu.flatten().sorted())
    }

    @Test
    fun `app-level pages come first, then components and manifest, then resources`() {
        assertEquals(
            listOf(Destination.Overview, Destination.Application, Destination.Sdks, Destination.Permissions, Destination.IconFiles),
            SidebarMenu[0],
        )
        assertEquals(Destination.Strings, SidebarMenu.last().first())
    }
}
