package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.model.LayoutRecord
import io.github.rajumark.sextant.core.model.LayoutView
import io.github.rajumark.sextant.core.model.MenuItem
import io.github.rajumark.sextant.core.model.MenuRecord
import io.github.rajumark.sextant.core.model.ResValue
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MenuPreviewTest {
    private fun item(
        id: String,
        show: String? = null,
        depth: Int = 1,
        order: Int? = null,
        visible: Boolean? = null,
        checkable: Boolean? = null,
        checked: Boolean? = null,
    ) = MenuItem(
        depth,
        "item",
        "id/$id",
        ResValue("android:title", "@string/$id", id.uppercase()),
        null,
        show,
        null,
        orderInCategory = order,
        visible = visible,
        checkable = checkable,
        checked = checked,
    )

    private fun menu(vararg items: MenuItem) = MenuRecord("main", "", "res/a.xml", items.toList())

    private fun ids(entries: List<MenuEntry>) = entries.map { it.id!!.removePrefix("id/") }

    @Test
    fun `showAsAction flags decode to names`() {
        ResourceHarvester.showAsAction("0x0") shouldBe "never"
        ResourceHarvester.showAsAction("0x1") shouldBe "ifRoom"
        ResourceHarvester.showAsAction("0x2") shouldBe "always"
        ResourceHarvester.showAsAction("0x5") shouldBe "ifRoom|withText"
        ResourceHarvester.showAsAction("0xa") shouldBe "always|collapseActionView"
        ResourceHarvester.showAsAction("ifRoom") shouldBe "ifRoom"
    }

    @Test
    fun `ifRoom items fill the app bar but keep a slot for the overflow button`() {
        val p = MenuPreviews.of(menu(item("a", "ifRoom"), item("b", "ifRoom"), item("c", "ifRoom"), item("d", "never")))
        ids(p.toolbar) shouldBe listOf("a", "b")
        ids(p.overflow) shouldBe listOf("c", "d")
    }

    @Test
    fun `without an overflow every slot is used, and always items always go up`() {
        ids(MenuPreviews.of(menu(item("a", "ifRoom"), item("b", "ifRoom"), item("c", "ifRoom"))).toolbar) shouldBe listOf("a", "b", "c")
        val p = MenuPreviews.of(menu(item("a", "always"), item("b", "always"), item("c", "always"), item("d", "always"), item("e")))
        ids(p.toolbar) shouldBe listOf("a", "b", "c", "d")
        ids(p.overflow) shouldBe listOf("e")
    }

    @Test
    fun `orderInCategory sorts items, ties keep XML order, hidden items are apart`() {
        val p = MenuPreviews.of(menu(item("a", order = 20), item("b", order = 10), item("c", order = 10), item("d", visible = false)))
        ids(p.overflow) shouldBe listOf("b", "c", "a")
        ids(p.hidden) shouldBe listOf("d")
    }

    @Test
    fun `groups give radio marks and sub-menus nest`() {
        val items =
            listOf(
                MenuItem(1, "group", "id/sort", null, null, null, null, checkableBehavior = "single"),
                item("by_name", depth = 2, checked = true),
                item("by_date", depth = 2),
                item("more"),
                MenuItem(2, "menu", null, null, null, null, null),
                item("about", depth = 3),
            )
        val p = MenuPreviews.of(MenuRecord("main", "", "res/a.xml", items))
        p.overflow.map { it.mark } shouldBe listOf("radio", "radio", null)
        p.overflow[0].checked shouldBe true
        p.overflow[0].group shouldBe p.overflow[1].group
        ids(p.overflow[2].children) shouldBe listOf("about")
    }

    @Test
    fun `a BottomNavigationView in a layout makes every item a tab`() {
        val layout =
            LayoutRecord(
                "activity_main",
                "",
                "res/b.xml",
                "LinearLayout",
                1,
                emptyList(),
                emptyList(),
                emptyList(),
                emptyList(),
                listOf(
                    LayoutView(
                        1,
                        "com.google.android.material.bottomnavigation.BottomNavigationView",
                        null,
                        listOf(ResValue("app:menu", "@menu/main")),
                    ),
                ),
            )
        val p = MenuPreviews.of(menu(item("a"), item("b"), item("c"), item("d")), listOf(layout))
        p.host shouldBe "bottom-nav"
        ids(p.toolbar) shouldBe listOf("a", "b", "c", "d")
        p.hostViews shouldBe listOf("layout/activity_main  BottomNavigationView")
    }
}
