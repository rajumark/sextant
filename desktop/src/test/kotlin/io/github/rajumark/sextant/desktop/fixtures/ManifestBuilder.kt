package io.github.rajumark.sextant.desktop.data

import io.github.rajumark.sextant.desktop.model.AppComponent
import io.github.rajumark.sextant.desktop.model.AppMeta
import io.github.rajumark.sextant.desktop.model.ExportedSource
import io.github.rajumark.sextant.desktop.model.ManifestNode
import io.github.rajumark.sextant.desktop.model.Permission
import io.github.rajumark.sextant.desktop.model.PermissionKind

/** Builds a decoded-manifest tree that matches the sample permissions and components. */
internal object ManifestBuilder {
    fun build(
        meta: AppMeta,
        permissions: List<Permission>,
        components: List<AppComponent>,
    ): ManifestNode {
        val f = meta.flags
        val appAttrs =
            buildList {
                add("android:name" to (meta.applicationClass ?: ""))
                add("android:label" to "@string/app_name")
                add("android:icon" to "@mipmap/ic_launcher")
                add("android:theme" to "@style/Theme.App")
                f.allowBackup?.let { add("android:allowBackup" to it.toString()) }
                f.debuggable?.takeIf { it }?.let { add("android:debuggable" to "true") }
                f.usesCleartextTraffic?.let { add("android:usesCleartextTraffic" to it.toString()) }
                f.networkSecurityConfig?.let { add("android:networkSecurityConfig" to it) }
                f.extractNativeLibs?.let { add("android:extractNativeLibs" to it.toString()) }
            }
        return ManifestNode(
            tag = "manifest",
            attributes =
                buildList {
                    add("package" to meta.packageName)
                    add("android:versionCode" to meta.versionCode.toString())
                    add("android:versionName" to (meta.versionName ?: ""))
                    meta.compileSdk?.let { add("android:compileSdkVersion" to it.toString()) }
                },
            children =
                buildList {
                    add(
                        ManifestNode(
                            "uses-sdk",
                            listOf(
                                "android:minSdkVersion" to "${meta.minSdk}",
                                "android:targetSdkVersion" to "${meta.targetSdk}",
                            ),
                        ),
                    )
                    permissions.filter { it.kind != PermissionKind.Declared }.forEach { p ->
                        add(
                            ManifestNode(
                                p.kind.wire,
                                buildList {
                                    add("android:name" to p.name)
                                    p.maxSdk?.let { add("android:maxSdkVersion" to it.toString()) }
                                },
                            ),
                        )
                    }
                    permissions.filter { it.kind == PermissionKind.Declared }.forEach { p ->
                        add(
                            ManifestNode(
                                "permission",
                                listOf(
                                    "android:name" to p.name,
                                    "android:protectionLevel" to (p.protectionLevel ?: "normal"),
                                ),
                            ),
                        )
                    }
                    add(ManifestNode("uses-feature", listOf("android:name" to "android.hardware.camera", "android:required" to "false")))
                    add(
                        ManifestNode(
                            "queries",
                            children =
                                listOf(
                                    ManifestNode(
                                        "intent",
                                        children =
                                            listOf(
                                                ManifestNode("action", listOf("android:name" to "android.intent.action.VIEW")),
                                                ManifestNode("data", listOf("android:scheme" to "https")),
                                            ),
                                    ),
                                ),
                        ),
                    )
                    add(ManifestNode("application", appAttrs, components.map(::componentNode)))
                },
        )
    }

    private fun componentNode(c: AppComponent): ManifestNode =
        ManifestNode(
            tag = c.type.wire,
            attributes =
                buildList {
                    add("android:name" to c.name)
                    if (c.exportedSource == ExportedSource.Explicit) add("android:exported" to c.exported.toString())
                    if (!c.enabled) add("android:enabled" to "false")
                    c.permission?.let { add("android:permission" to it) }
                    c.process?.let { add("android:process" to it) }
                    c.targetActivity?.let { add("android:targetActivity" to it) }
                    if (c.authorities.isNotEmpty()) add("android:authorities" to c.authorities.joinToString(";"))
                },
            children =
                c.intentFilters.map { f ->
                    ManifestNode(
                        tag = "intent-filter",
                        attributes =
                            buildList {
                                if (f.autoVerify) add("android:autoVerify" to "true")
                                if (f.priority != 0) add("android:priority" to f.priority.toString())
                            },
                        children =
                            f.actions.map { ManifestNode("action", listOf("android:name" to it)) } +
                                f.categories.map { ManifestNode("category", listOf("android:name" to it)) } +
                                f.data.map { d ->
                                    ManifestNode(
                                        "data",
                                        listOfNotNull(
                                            d.scheme?.let { "android:scheme" to it },
                                            d.host?.let { "android:host" to it },
                                            d.port?.let { "android:port" to it },
                                            d.path?.let { "android:path" to it },
                                            d.pathPrefix?.let { "android:pathPrefix" to it },
                                            d.pathPattern?.let { "android:pathPattern" to it },
                                            d.mimeType?.let { "android:mimeType" to it },
                                        ),
                                    )
                                },
                    )
                },
        )
}
