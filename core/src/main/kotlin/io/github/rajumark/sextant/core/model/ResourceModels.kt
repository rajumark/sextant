package io.github.rajumark.sextant.core.model

import kotlinx.serialization.Serializable

/** Everything harvested from res/ and assets/ (schema 8). Written as one JSON-lines file per kind under resources/. */
@Serializable
data class ResourceHarvest(
    val ids: List<ResourceIdRecord> = emptyList(),
    val strings: List<StringResource> = emptyList(),
    val styles: List<StyleRecord> = emptyList(),
    val drawables: List<DrawableRecord> = emptyList(),
    val layouts: List<LayoutRecord> = emptyList(),
    val menus: List<MenuRecord> = emptyList(),
    val navigation: List<NavigationRecord> = emptyList(),
    val assets: List<AssetRecord> = emptyList(),
    val xml: List<XmlConfigRecord> = emptyList(),
    /** Who points at which resource: layouts, menus, navigation graphs, XML configs, styles and the manifest. */
    val refs: List<ResourceRef> = emptyList(),
)

/** One resource id from resources.arsc. */
@Serializable
data class ResourceIdRecord(
    /** `0x7f0e001c` */
    val id: String,
    val type: String,
    val name: String,
    /** Configurations that define it (`""` is the default). */
    val configs: List<String>,
)

/** One string, plural quantity or string-array item in one configuration. */
@Serializable
data class StringResource(
    val name: String,
    /** string | plurals | array */
    val kind: String,
    /** Qualifier as in the folder name without `values-` (`""` default, `fr`, `pt-rBR`, `night`). */
    val config: String,
    /** BCP-47 locale of [config], or null for the default/non-locale configs. */
    val locale: String?,
    /** Plural quantity (`one`, `other`) or array index; null for plain strings. */
    val item: String?,
    val value: String,
)

/** A raw attribute value and, when it is a resource reference, what it resolves to in the default configuration. */
@Serializable
data class ResValue(
    val name: String,
    val raw: String,
    val resolved: String? = null,
)

@Serializable
data class StyleRecord(
    val name: String,
    val config: String,
    val parent: String?,
    val items: List<ResValue>,
    /** Used as android:theme in the manifest (application or an activity). */
    val usedBy: List<String>,
)

@Serializable
data class DrawableRecord(
    /** drawable | mipmap */
    val type: String,
    val name: String,
    val config: String,
    val path: String,
    /** png | webp | jpg | gif | 9-patch | vector | shape | selector | layer-list | adaptive-icon | bitmap | xml-other | other */
    val format: String,
    val bytes: Long,
    val width: Int? = null,
    val height: Int? = null,
    /** Readable XML of the decoded file for XML drawables (vector, shape, selector, …); empty for images. */
    val xml: String = "",
)

@Serializable
data class LayoutView(
    val depth: Int,
    /** View class or tag as written: TextView, com.app.ui.Card, include, fragment, merge, … */
    val tag: String,
    val id: String?,
    /** Attributes worth reading: text, hint, contentDescription, src, onClick, layout, name/class, style. */
    val attributes: List<ResValue>,
)

@Serializable
data class LayoutRecord(
    val name: String,
    val config: String,
    val path: String,
    val root: String?,
    val viewCount: Int,
    val ids: List<String>,
    /** View classes that are not android.* / androidx.* framework widgets. */
    val customViews: List<String>,
    /** Fragment classes declared in the layout. */
    val fragments: List<String>,
    /** Layouts included with <include layout="…">. */
    val includes: List<String>,
    val views: List<LayoutView>,
    /** Readable XML of the decoded file, as the developer wrote it (attribute values as compiled). */
    val xml: String = "",
)

@Serializable
data class MenuItem(
    val depth: Int,
    /** item | group | menu (sub-menu) */
    val tag: String,
    val id: String?,
    val title: ResValue?,
    val icon: String?,
    /** never | ifRoom | always, plus withText / collapseActionView, joined with `|` (decoded from the compiled flags). */
    val showAsAction: String?,
    val actionViewClass: String?,
    val orderInCategory: Int? = null,
    val visible: Boolean? = null,
    val enabled: Boolean? = null,
    val checkable: Boolean? = null,
    val checked: Boolean? = null,
    /** Groups only: none | all (check boxes) | single (radio buttons). */
    val checkableBehavior: String? = null,
    /** container | system | secondary | alternative */
    val menuCategory: String? = null,
    val contentDescription: ResValue? = null,
    val tooltipText: ResValue? = null,
    val actionLayout: String? = null,
    val actionProviderClass: String? = null,
    val alphabeticShortcut: String? = null,
)

@Serializable
data class MenuRecord(
    val name: String,
    val config: String,
    val path: String,
    val items: List<MenuItem>,
    /** Readable XML of the decoded file, as the developer wrote it (attribute values as compiled). */
    val xml: String = "",
)

@Serializable
data class NavAction(
    val id: String?,
    val destination: String?,
    val popUpTo: String?,
)

@Serializable
data class NavDestination(
    val id: String?,
    /** fragment | activity | dialog | navigation | include | custom tag */
    val kind: String,
    /** android:name: the Fragment/Activity class. */
    val className: String?,
    val label: ResValue?,
    val actions: List<NavAction>,
    val deepLinks: List<String>,
    val arguments: List<String>,
)

@Serializable
data class NavigationRecord(
    val name: String,
    val path: String,
    val startDestination: String?,
    val destinations: List<NavDestination>,
    /** Readable XML of the decoded file, as the developer wrote it (attribute values as compiled). */
    val xml: String = "",
)

@Serializable
data class AssetRecord(
    val path: String,
    val bytes: Long,
    /** By extension: js, json, html, font, image, audio, video, db, model, certificate, native, dex, archive, text, other. */
    val kind: String,
    /** What the first bytes say, when it is executable or an archive: dex | elf | zip | null. */
    val magic: String?,
    /** Executable code or an archive whose name hides it (e.g. a .dat that is a DEX): worth a look. */
    val hiddenCode: Boolean,
    /** The file's text when it is a small UTF-8 text asset (json, xml, html, txt, …), else null. */
    val text: String? = null,
)

@Serializable
data class XmlConfigRecord(
    val name: String,
    val config: String,
    val path: String,
    val root: String?,
    /** network-security-config | backup-rules | data-extraction-rules | file-provider-paths | locale-config | shortcuts | … */
    val kind: String,
    /** The few facts that matter for this kind, e.g. cleartext=permitted, pins=2, user-certs=trusted. */
    val summary: Map<String, String>,
    /** Readable XML of the decoded file. */
    val xml: String,
)

@Serializable
data class ResourceRef(
    /** `@string/app_name` */
    val ref: String,
    /** layout | menu | navigation | xml | style | manifest */
    val fromKind: String,
    /** The file or element that holds it, e.g. `layout/activity_main` or `activity com.app.Main`. */
    val from: String,
    /** The attribute that holds it, e.g. `android:text`. */
    val attribute: String,
)
