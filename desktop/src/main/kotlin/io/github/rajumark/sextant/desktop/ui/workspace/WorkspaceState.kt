package io.github.rajumark.sextant.desktop.ui.workspace

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.rajumark.sextant.desktop.model.Analysis
import io.github.rajumark.sextant.desktop.model.ComponentType
import io.github.rajumark.sextant.desktop.model.ManifestNode

enum class Destination(
    val label: String,
    val icon: ImageVector,
) {
    Overview("Overview", Icons.Outlined.Dashboard),
    Permissions("Permissions", Icons.Outlined.Shield),
    Activities("Activities", Icons.Outlined.Smartphone),
    Services("Services", Icons.Outlined.Sync),
    Receivers("Receivers", Icons.Outlined.Sensors),
    Aliases("Aliases", Icons.Outlined.Link),
    Components("Components", Icons.Outlined.Widgets),
    Manifest("Manifest", Icons.Outlined.AccountTree),
    Signing("Signing", Icons.Outlined.VerifiedUser),
    Output("Output", Icons.Outlined.FolderOpen),
    IconFiles("Icons", Icons.Outlined.Image),
}

enum class PermissionFilter(
    val label: String,
) {
    All("All"),
    Dangerous("Dangerous"),
    Normal("Normal"),
    Declared("Declared"),
    Custom("Custom"),
}

enum class ComponentGroup(
    val label: String,
    val types: Set<ComponentType>,
) {
    All("All", ComponentType.entries.toSet()),
    Activities("Activities", setOf(ComponentType.Activity, ComponentType.ActivityAlias)),
    Services("Services", setOf(ComponentType.Service)),
    Receivers("Receivers", setOf(ComponentType.Receiver)),
    Providers("Providers", setOf(ComponentType.Provider)),
}

enum class ManifestView(
    val label: String,
) {
    Tree("Tree"),
    Xml("XML"),
    Json("JSON"),
}

/** Per-analysis view state, kept while switching destinations. */
class WorkspaceState(
    val analysis: Analysis,
) {
    var destination by mutableStateOf(Destination.Overview)

    var permissionQuery by mutableStateOf("")
    var permissionFilter by mutableStateOf(PermissionFilter.All)
    var selectedPermission by mutableStateOf<String?>(null)

    var componentQuery by mutableStateOf("")
    var componentGroup by mutableStateOf(ComponentGroup.All)
    var exportedOnly by mutableStateOf(false)
    var deepLinksOnly by mutableStateOf(false)
    var selectedComponent by mutableStateOf<String?>(null)

    var manifestView by mutableStateOf(ManifestView.Tree)
    var manifestQuery by mutableStateOf("")
    var expanded by mutableStateOf(defaultExpanded(analysis.manifest))
    var selectedNode by mutableStateOf<String?>(null)

    var selectedFile by mutableStateOf("summary.md")

    /** Text search over the icon list (file path, density, resource). */
    var iconQuery by mutableStateOf("")

    /** The icon folder kind to show ("All", "mipmap", "drawable", ...). */
    var iconFilter by mutableStateOf("All")

    /** The selected icon file, as "role|path"; null when nothing is selected. */
    var selectedIcon by mutableStateOf<String?>(null)

    /** Jumps to a component from another screen (e.g. the overview's deep-link list). */
    fun showComponent(name: String) {
        componentGroup = ComponentGroup.All
        exportedOnly = false
        deepLinksOnly = false
        componentQuery = ""
        selectedComponent = name
        destination = Destination.Components
    }

    fun showPermission(name: String) {
        permissionFilter = PermissionFilter.All
        permissionQuery = ""
        selectedPermission = name
        destination = Destination.Permissions
    }

    companion object {
        /** Tree paths are child indexes joined by '/', the root is "". */
        fun defaultExpanded(root: ManifestNode): Set<String> {
            val app = root.children.indexOfFirst { it.tag == "application" }
            return if (app >= 0) setOf("", "$app") else setOf("")
        }

        fun allPaths(root: ManifestNode): Set<String> {
            val out = mutableSetOf<String>()

            fun walk(
                n: ManifestNode,
                path: String,
            ) {
                if (n.children.isNotEmpty()) out += path
                n.children.forEachIndexed { i, c -> walk(c, if (path.isEmpty()) "$i" else "$path/$i") }
            }
            walk(root, "")
            return out
        }
    }
}
