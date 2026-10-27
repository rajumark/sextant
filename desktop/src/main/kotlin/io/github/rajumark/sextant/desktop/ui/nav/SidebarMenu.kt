package io.github.rajumark.sextant.desktop.ui.nav

import io.github.rajumark.sextant.desktop.ui.workspace.Destination

/**
 * The order of the sidebar's analysis menu. To reorder or regroup items, edit only this list.
 * Each inner list is one group; a divider is drawn between groups. Every [Destination] should appear exactly once
 * (a test checks this).
 */
val SidebarMenu: List<List<Destination>> =
    listOf(
        listOf(Destination.Overview, Destination.Application, Destination.Sdks, Destination.Permissions, Destination.IconFiles),
        listOf(
            Destination.Activities,
            Destination.Services,
            Destination.Receivers,
            Destination.Providers,
            Destination.Aliases,
            Destination.Manifest,
            Destination.Signing,
            Destination.Output,
        ),
        listOf(
            Destination.Strings,
            Destination.Layouts,
            Destination.Menus,
            Destination.Navigation,
            Destination.Styles,
            Destination.Drawables,
            Destination.ResourceIds,
            Destination.Assets,
            Destination.XmlConfigs,
        ),
    )
