package io.github.rajumark.sextant.desktop.ui.nav

import io.github.rajumark.sextant.desktop.ui.workspace.Destination

/** One named, foldable part of the sidebar. A null title is drawn without a header and never folds. */
class MenuSection(
    val title: String?,
    val items: List<Destination>,
)

/**
 * The sidebar's analysis menu, in the order a review usually goes: the overview, the app-level review pages, the
 * components, the resources, then the raw files. To reorder or regroup items, edit only this list. Every
 * [Destination] should appear exactly once (a test checks this).
 */
val SidebarMenu: List<MenuSection> =
    listOf(
        MenuSection(null, listOf(Destination.Overview)),
        MenuSection(
            "Review",
            listOf(Destination.Application, Destination.Permissions, Destination.Sdks, Destination.Signing),
        ),
        MenuSection(
            "Components",
            listOf(
                Destination.Activities,
                Destination.Services,
                Destination.Receivers,
                Destination.Providers,
                Destination.Aliases,
            ),
        ),
        MenuSection(
            "Resources",
            listOf(
                Destination.Strings,
                Destination.Layouts,
                Destination.Menus,
                Destination.Navigation,
                Destination.Styles,
                Destination.Drawables,
                Destination.IconFiles,
                Destination.ResourceIds,
                Destination.Assets,
                Destination.XmlConfigs,
            ),
        ),
        MenuSection("Files", listOf(Destination.Manifest, Destination.Output)),
    )
