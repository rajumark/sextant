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
    fun `overview, permissions and icons come first, then a divider, then the rest`() {
        assertEquals(listOf(Destination.Overview, Destination.Permissions, Destination.IconFiles), SidebarMenu[0])
    }
}
