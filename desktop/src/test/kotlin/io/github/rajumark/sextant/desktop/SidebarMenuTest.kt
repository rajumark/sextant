package io.github.rajumark.sextant.desktop

import io.github.rajumark.sextant.desktop.ui.nav.SidebarMenu
import io.github.rajumark.sextant.desktop.ui.workspace.Destination
import kotlin.test.Test
import kotlin.test.assertEquals

class SidebarMenuTest {
    @Test
    fun `every screen appears exactly once in the sidebar`() {
        assertEquals(Destination.entries.sorted(), SidebarMenu.flatMap { it.items }.sorted())
    }

    @Test
    fun `overview stands alone, then named sections in review order`() {
        assertEquals(listOf(Destination.Overview), SidebarMenu.first().items)
        assertEquals(null, SidebarMenu.first().title)
        assertEquals(listOf("Review", "Components", "Resources", "Files"), SidebarMenu.drop(1).map { it.title })
    }
}
