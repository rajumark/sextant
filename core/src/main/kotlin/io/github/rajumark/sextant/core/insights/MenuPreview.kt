package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.model.LayoutRecord
import io.github.rajumark.sextant.core.model.MenuItem
import io.github.rajumark.sextant.core.model.MenuRecord
import kotlinx.serialization.Serializable

/** One row of a menu as Android shows it: an item, or a sub-menu with its own rows. */
@Serializable
data class MenuEntry(
    /** Resolved title, else the raw `@string/…`, else null (set in code). */
    val title: String?,
    /** The title as written, when it is a reference. */
    val titleRef: String?,
    val id: String?,
    val icon: String?,
    /** never | ifRoom | always (withText / collapseActionView dropped). */
    val placement: String,
    val withText: Boolean,
    /** check (check box) | radio | null */
    val mark: String?,
    val checked: Boolean,
    val enabled: Boolean,
    val visible: Boolean,
    /** Action view (e.g. a SearchView), action layout or action provider standing in for the item. */
    val actionView: String?,
    /** Index of the group the item belongs to, so a preview can draw dividers between groups; null outside groups. */
    val group: Int?,
    val children: List<MenuEntry> = emptyList(),
)

/**
 * A menu laid out the way its host shows it. App bar and toolbar menus: [toolbar] action buttons (at most [slots],
 * one kept for ⋮ when anything overflows), then the [overflow] list. Bottom navigation: every item is a tab in
 * [toolbar]. Navigation drawer: every item is a row in [overflow].
 */
@Serializable
data class MenuPreview(
    /** app-bar (inflated from code: options or context menu) | toolbar | bottom-nav | drawer */
    val host: String,
    /** Layout views that show this menu, e.g. `layout/activity_main  BottomNavigationView`. */
    val hostViews: List<String>,
    val slots: Int,
    val toolbar: List<MenuEntry>,
    val overflow: List<MenuEntry>,
    /** Items with android:visible="false": shown only when code turns them on. */
    val hidden: List<MenuEntry>,
    /** Items with no title in XML: their text is set in code. */
    val untitled: Int,
)

object MenuPreviews {
    /** Action buttons a phone app bar fits in portrait (Android's ActionBarPolicy gives 3 for 360–499dp wide screens). */
    const val PHONE_SLOTS = 3

    fun of(
        menu: MenuRecord,
        layouts: List<LayoutRecord> = emptyList(),
        slots: Int = PHONE_SLOTS,
    ): MenuPreview {
        val ref = "@menu/${menu.name}"
        val views =
            layouts
                .flatMap { l ->
                    l.views.filter { v -> v.attributes.any { it.raw == ref } }.map { "layout/${l.name}  ${it.tag.substringAfterLast('.')}" }
                }.distinct()
        val host =
            when {
                views.any { "BottomNavigationView" in it || "NavigationRailView" in it } -> "bottom-nav"
                views.any { "NavigationView" in it } -> "drawer"
                views.any { "Toolbar" in it || "BottomAppBar" in it || "ActionMenuView" in it } -> "toolbar"
                else -> "app-bar"
            }
        val all = tree(menu.items)
        val (shown, hidden) = all.partition { it.visible }
        val untitled = count(all) { it.title == null }
        return when (host) {
            "bottom-nav" -> MenuPreview(host, views, slots, shown, emptyList(), hidden, untitled)
            "drawer" -> MenuPreview(host, views, slots, emptyList(), shown, hidden, untitled)
            else -> {
                // Android's ActionMenuPresenter: `always` items always go up; `ifRoom` items fill what is left, keeping
                // one slot for the ⋮ button whenever something ends up in the overflow.
                val always = shown.filter { it.placement == "always" }
                val ifRoom = shown.filter { it.placement == "ifRoom" }
                val overflows = shown.size > always.size + ifRoom.size || always.size + ifRoom.size > slots
                val room = (slots - always.size - if (overflows) 1 else 0).coerceAtLeast(0)
                val up = (always + ifRoom.take(room)).toSet()
                MenuPreview(host, views, slots, shown.filter { it in up }, shown.filter { it !in up }, hidden, untitled)
            }
        }
    }

    /** Builds rows from the flat depth-first list: `group` children are flattened in with a group index, `menu` under an item is its sub-menu. */
    private fun tree(items: List<MenuItem>): List<MenuEntry> {
        var groups = 0
        var pos = 0
        val keys = java.util.IdentityHashMap<MenuEntry, Long>()

        fun level(
            from: Int,
            depth: Int,
            group: Int?,
            mark: String?,
        ): Pair<List<MenuEntry>, Int> {
            // (orderInCategory, position) → entry: Android sorts by orderInCategory, keeping XML order for ties.
            val out = mutableListOf<Pair<Long, MenuEntry>>()
            var i = from
            while (i < items.size && items[i].depth >= depth) {
                val it = items[i]
                if (it.depth > depth) {
                    i++
                    continue
                }
                when (it.tag) {
                    "group" -> {
                        val g = groups++
                        val groupMark =
                            when (it.checkableBehavior) {
                                "all" -> "check"
                                "single" -> "radio"
                                else -> null
                            }
                        val (kids, next) = level(i + 1, depth + 1, g, groupMark)
                        // A hidden or disabled group hides or disables everything in it; items keep their own order.
                        kids.forEach { k ->
                            val e = k.copy(visible = k.visible && it.visible != false, enabled = k.enabled && it.enabled != false)
                            out += keys.getValue(k) to e
                        }
                        i = next
                    }
                    "item" -> {
                        val sub = items.getOrNull(i + 1)?.takeIf { s -> s.tag == "menu" && s.depth == depth + 1 }
                        val (kids, next) = if (sub != null) level(i + 2, depth + 2, null, null) else emptyList<MenuEntry>() to i + 1
                        val flags = it.showAsAction.orEmpty().split('|')
                        val e =
                            MenuEntry(
                                title = it.title?.let { t -> t.resolved ?: t.raw.takeIf { r -> r.isNotEmpty() } },
                                titleRef = it.title?.raw?.takeIf { r -> r.startsWith("@") },
                                id = it.id,
                                icon = it.icon,
                                placement = flags.firstOrNull { f -> f in setOf("always", "ifRoom", "never") } ?: "never",
                                withText = "withText" in flags,
                                mark = if (it.checkable == true) "check" else mark,
                                checked = it.checked == true,
                                enabled = it.enabled != false,
                                visible = it.visible != false,
                                actionView = it.actionViewClass ?: it.actionLayout ?: it.actionProviderClass,
                                group = group,
                                children = kids,
                            )
                        val key = (it.orderInCategory ?: 0).toLong() * 1_000_000 + pos++
                        keys[e] = key
                        out += key to e
                        i = next
                    }
                    else -> i++
                }
            }
            return out.sortedBy { it.first }.map { it.second } to i
        }
        return level(0, items.minOfOrNull { it.depth } ?: 1, null, null).first
    }

    private fun count(
        entries: List<MenuEntry>,
        p: (MenuEntry) -> Boolean,
    ): Int = entries.sumOf { (if (p(it)) 1 else 0) + count(it.children, p) }
}
