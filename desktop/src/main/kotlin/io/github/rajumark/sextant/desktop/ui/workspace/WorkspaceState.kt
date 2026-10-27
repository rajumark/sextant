package io.github.rajumark.sextant.desktop.ui.workspace

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.automirrored.outlined.MenuOpen
import androidx.compose.material.icons.automirrored.outlined.ViewQuilt
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.foundation.lazy.LazyListState
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
    Application("Application", Icons.Outlined.RocketLaunch),
    Sdks("SDKs & libraries", Icons.Outlined.Extension),
    Permissions("Permissions", Icons.Outlined.Shield),
    Activities("Activities", Icons.Outlined.Smartphone),
    Services("Services", Icons.Outlined.Sync),
    Receivers("Receivers", Icons.Outlined.Sensors),
    Providers("Providers", Icons.Outlined.Storage),
    Aliases("Aliases", Icons.Outlined.Link),
    Manifest("Manifest", Icons.Outlined.AccountTree),
    Signing("Signing", Icons.Outlined.VerifiedUser),
    Strings("Strings", Icons.Outlined.Translate),
    Layouts("Layouts", Icons.AutoMirrored.Outlined.ViewQuilt),
    Menus("Menus", Icons.AutoMirrored.Outlined.MenuOpen),
    Navigation("Navigation", Icons.Outlined.Route),
    Styles("Styles & themes", Icons.Outlined.Palette),
    Drawables("Drawables", Icons.Outlined.Brush),
    ResourceIds("Resource IDs", Icons.Outlined.Tag),
    Assets("Assets", Icons.Outlined.Inventory2),
    XmlConfigs("XML configs", Icons.Outlined.Code),
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

    /** One page per component type, so each keeps its own search, filters, selection and scroll position. */
    private val componentPages = ComponentType.entries.associateWith { ComponentPageState() }

    fun componentPage(type: ComponentType): ComponentPageState = componentPages.getValue(type)

    /** The selected component on the current component page; setting it selects it on its own type's page. */
    var selectedComponent: String?
        get() = destination.componentType?.let { componentPage(it).selected }
        set(value) {
            val type = value?.let { n -> analysis.components.firstOrNull { it.name == n }?.type } ?: destination.componentType
            type?.let { componentPage(it).selected = value }
        }

    /** One page state per resource page (Strings, Layouts, …). */
    private val resourcePages = HashMap<Destination, ResourcePageState>()

    fun resourcePage(d: Destination): ResourcePageState = resourcePages.getOrPut(d) { ResourcePageState() }

    /** Opens a resource page with one entry selected (from "Used by" links and the palette). */
    fun showResource(
        d: Destination,
        key: String,
    ) {
        resourcePage(d).apply {
            query = ""
            filters = emptySet()
            selected = key
        }
        destination = d
    }

    /** Selected startup step on the Application page (its order number). */
    var selectedStartupStep by mutableStateOf<Int?>(null)

    /** Command palette (Cmd/Ctrl+K): jump to any page, component or permission. */
    var paletteOpen by mutableStateOf(false)

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

    /** Jumps to a component from another screen (overview, permissions, palette): opens its type's page, filters cleared. */
    fun showComponent(name: String) {
        val c = analysis.components.firstOrNull { it.name == name } ?: return
        componentPage(c.type).apply {
            clearFilters()
            query = ""
            selected = name
            multi = emptySet()
        }
        destination = c.type.destination
    }

    /** Opens a component page with one verdict filter set (overview tiles). */
    fun showComponentsWithTag(
        type: ComponentType,
        tag: String,
    ) {
        componentPage(type).apply {
            clearFilters()
            tags = setOf(tag)
        }
        destination = type.destination
    }

    fun showPermission(name: String) {
        permissionFilter = PermissionFilter.All
        permissionQuery = ""
        selectedPermission = name
        destination = Destination.Permissions
    }

    /** Jump to the manifest and select the node declaring the given component name. */
    fun showComponentInManifest(componentName: String) {
        val path = findComponentPath(analysis.manifest, componentName)
        if (path != null) {
            manifestQuery = ""
            manifestView = ManifestView.Tree
            expanded = defaultExpanded(analysis.manifest) + allPathsUpTo(path)
            selectedNode = path
            destination = Destination.Manifest
        } else {
            manifestQuery = componentName.substringAfterLast('.')
            manifestView = ManifestView.Tree
            destination = Destination.Manifest
        }
    }

    private fun findComponentPath(
        root: ManifestNode,
        componentName: String,
    ): String? {
        val pkg = analysis.meta.packageName

        fun resolveName(raw: String?): String? {
            if (raw == null) return null
            return when {
                raw.startsWith(".") && pkg.isNotEmpty() -> "$pkg$raw"
                raw == componentName -> raw
                raw.endsWith(".$componentName") -> raw
                componentName.endsWith(raw) && raw.contains('.') -> componentName
                else -> raw
            }
        }

        fun walk(
            n: ManifestNode,
            path: String,
        ): String? {
            if (n.tag in setOf("activity", "activity-alias", "service", "receiver", "provider")) {
                val raw = n.attr("android:name") ?: n.attr("name")
                val resolved = resolveName(raw)
                if (resolved == componentName || raw == componentName) {
                    return path
                }
                if (raw != null && !raw.contains('.') && componentName.endsWith(".$raw")) {
                    return path
                }
                if (raw != null && raw.startsWith(".") && pkg.isNotEmpty() && "$pkg$raw" == componentName) {
                    return path
                }
            }
            n.children.forEachIndexed { i, c ->
                val childPath = if (path.isEmpty()) "$i" else "$path/$i"
                val res = walk(c, childPath)
                if (res != null) return res
            }
            return null
        }
        return walk(root, "")
    }

    private fun allPathsUpTo(target: String): Set<String> {
        if (target.isEmpty()) return setOf("")
        val parts = target.split("/")
        val out = mutableSetOf<String>("")
        var cur = ""
        for (p in parts) {
            cur = if (cur.isEmpty()) p else "$cur/$p"
            out.add(cur)
        }
        return out
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

/** The sidebar page that lists a component type. */
val ComponentType.destination: Destination
    get() =
        when (this) {
            ComponentType.Activity -> Destination.Activities
            ComponentType.ActivityAlias -> Destination.Aliases
            ComponentType.Service -> Destination.Services
            ComponentType.Receiver -> Destination.Receivers
            ComponentType.Provider -> Destination.Providers
        }

/** The component type a page lists, or null for other pages. */
val Destination.componentType: ComponentType?
    get() =
        when (this) {
            Destination.Activities -> ComponentType.Activity
            Destination.Aliases -> ComponentType.ActivityAlias
            Destination.Services -> ComponentType.Service
            Destination.Receivers -> ComponentType.Receiver
            Destination.Providers -> ComponentType.Provider
            else -> null
        }

enum class ComponentSort(
    val label: String,
) {
    Relevance("Relevance"),
    Risk("Risk first"),
    Name("Name"),
    Package("Package"),
    Filters("Most intent filters"),
}

/** View state of one component page (Activities, Services, …). */
class ComponentPageState {
    var query by mutableStateOf("")
    var exported by mutableStateOf<Boolean?>(null)
    var guardedOnly by mutableStateOf(false)
    var disabledOnly by mutableStateOf(false)
    var customLaunchModeOnly by mutableStateOf(false)
    var customProcessOnly by mutableStateOf(false)

    /** Verdict tags that must all be present. */
    var tags by mutableStateOf(emptySet<String>())
    var sort by mutableStateOf(ComponentSort.Relevance)
    var groupByPackage by mutableStateOf(false)
    var collapsed by mutableStateOf(emptySet<String>())
    var selected by mutableStateOf<String?>(null)

    /** Rows picked with Cmd/Ctrl-click or Shift-click, for copying several at once. */
    var multi by mutableStateOf(emptySet<String>())

    /** Raw view in the detail pane: XML or JSON. */
    var rawJson by mutableStateOf(false)
    val listState = LazyListState()

    val filtering: Boolean
        get() = exported != null || guardedOnly || disabledOnly || customLaunchModeOnly || customProcessOnly || tags.isNotEmpty()

    fun clearFilters() {
        exported = null
        guardedOnly = false
        disabledOnly = false
        customLaunchModeOnly = false
        customProcessOnly = false
        tags = emptySet()
    }
}

/** View state of one resource page. */
class ResourcePageState {
    var query by mutableStateOf("")
    var filters by mutableStateOf(emptySet<String>())
    var selected by mutableStateOf<String?>(null)

    /** Strings page: the locale shown in the list (null = default). */
    var locale by mutableStateOf<String?>(null)
    val listState = LazyListState()
}
