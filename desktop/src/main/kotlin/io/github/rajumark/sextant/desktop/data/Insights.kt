package io.github.rajumark.sextant.desktop.data

import io.github.rajumark.sextant.desktop.model.Analysis

enum class Severity { Info, Notice, Risk }

data class Insight(
    val severity: Severity,
    val title: String,
    val detail: String,
)

/** Facts worth a second look, derived only from what the analysis contains. */
object Insights {
    fun of(a: Analysis): List<Insight> =
        buildList {
            val f = a.meta.flags
            if (f.debuggable == true) {
                add(Insight(Severity.Risk, "Debuggable build", "android:debuggable is true. Any user can attach a debugger to this app."))
            }
            val open = a.components.filter { it.exported && it.permission == null && !it.launcher }
            if (open.isNotEmpty()) {
                add(
                    Insight(
                        Severity.Notice,
                        "${open.size} exported components without a permission",
                        "Other apps can start these directly: " + open.take(3).joinToString { it.simpleName } +
                            if (open.size > 3) "…" else "",
                    ),
                )
            }
            val byDefault = a.components.count { it.exported && it.exportedSource.name == "Default" }
            if (byDefault > 0) {
                add(
                    Insight(
                        Severity.Notice,
                        "$byDefault exported only by Android's default rule",
                        "android:exported is missing; the component is exported because it declares an intent filter.",
                    ),
                )
            }
            if (f.allowBackup == true) {
                add(Insight(Severity.Notice, "Backups allowed", "android:allowBackup is true, so app data can be copied with adb backup."))
            }
            when (f.usesCleartextTraffic) {
                true ->
                    add(
                        Insight(
                            Severity.Risk,
                            "Cleartext traffic allowed",
                            "usesCleartextTraffic is true. Plain HTTP connections are permitted.",
                        ),
                    )
                null ->
                    if (f.networkSecurityConfig == null && (a.meta.targetSdk ?: 0) >= 28) {
                        add(
                            Insight(
                                Severity.Info,
                                "Cleartext blocked by default",
                                "Not set in the manifest; Android blocks it for targetSdk 28 and higher.",
                            ),
                        )
                    }
                false -> Unit
            }
            if (a.meta.signing.schemes == listOf("v1") ||
                (
                    a.meta.signing.schemes
                        .firstOrNull() == "v1" &&
                        (a.meta.minSdk ?: 1) < 24
                )
            ) {
                add(
                    Insight(
                        Severity.Notice,
                        "Signed with the v1 scheme",
                        "JAR signing is kept for old devices. v1 alone is vulnerable to Janus on Android 7.0 and lower.",
                    ),
                )
            }
            if (a.meta.signing.certificates
                    .any { it.subject.startsWith("CN=Android Debug") }
            ) {
                add(
                    Insight(
                        Severity.Risk,
                        "Signed with a debug certificate",
                        "The signer is the default Android debug key, not a release key.",
                    ),
                )
            }
            val verified =
                a.components
                    .flatMap { it.intentFilters }
                    .filter { it.autoVerify }
                    .flatMap { it.data }
                    .mapNotNull { it.host }
                    .distinct()
            if (verified.isNotEmpty()) {
                add(Insight(Severity.Info, "Verified App Links", "autoVerify is set for " + verified.joinToString()))
            }
            if (a.dangerousCount > 0) {
                add(Insight(Severity.Info, "${a.dangerousCount} dangerous permissions", "These need runtime approval from the user."))
            }
        }
}
