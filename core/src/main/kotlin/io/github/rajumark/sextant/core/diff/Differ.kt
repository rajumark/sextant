package io.github.rajumark.sextant.core.diff

import io.github.rajumark.sextant.core.index.IndexReader
import io.github.rajumark.sextant.core.model.AppMeta
import io.github.rajumark.sextant.core.output.SextantJson
import kotlinx.serialization.Serializable
import java.nio.file.Path

@Serializable
data class DiffReport(
    val old: String,
    val new: String,
    val sections: List<DiffSection>,
)

/** What changed in one area. Lists are sorted and capped at [MAX_ITEMS]; the totals are exact. */
@Serializable
data class DiffSection(
    val name: String,
    val addedTotal: Int,
    val removedTotal: Int,
    val changedTotal: Int,
    val added: List<String>,
    val removed: List<String>,
    val changed: List<Change>,
    val note: String? = null,
) {
    val empty get() = addedTotal == 0 && removedTotal == 0 && changedTotal == 0
}

@Serializable
data class Change(
    val item: String,
    val old: String,
    val new: String,
)

const val MAX_ITEMS = 200

/**
 * Compares two extraction folders (each with index.sqlite). Everything is keyed by stable names, so it works
 * across versions; obfuscated class names (R8) may be renamed between builds, which is called out in the code sections.
 */
object Differ {
    fun diff(
        oldDir: Path,
        newDir: Path,
    ): DiffReport =
        IndexReader.open(oldDir).use { a ->
            IndexReader.open(newDir).use { b ->
                val oldMeta = meta(a)
                val newMeta = meta(b)

                fun label(m: AppMeta) = "${m.`package`} ${m.versionName} (${m.versionCode})"
                DiffReport(
                    old = label(oldMeta),
                    new = label(newMeta),
                    sections =
                        listOf(
                            maps("app", appFacts(oldMeta), appFacts(newMeta)),
                            maps("permissions", a.pairs(PERMISSIONS), b.pairs(PERMISSIONS)),
                            maps("components", a.pairs(COMPONENTS), b.pairs(COMPONENTS)),
                            sets("deep links", a.set(DEEP_LINKS), b.set(DEEP_LINKS)),
                            sets("frameworks", a.set(FRAMEWORKS), b.set(FRAMEWORKS)),
                            sets("sdks", a.set(SDKS), b.set(SDKS)),
                            maps("libraries", a.pairs(LIBRARIES), b.pairs(LIBRARIES)),
                            sets("hosts", a.set(HOSTS), b.set(HOSTS)),
                            sets("urls", a.set(URLS), b.set(URLS)),
                            maps("native libraries", a.pairs(NATIVE), b.pairs(NATIVE), note = "changed = different sha256"),
                            sets("jni methods", a.set(JNI), b.set(JNI)),
                            counts("packages (classes)", a.pairs(PACKAGES), b.pairs(PACKAGES)),
                            sets(
                                "classes",
                                a.set(CLASSES),
                                b.set(CLASSES),
                                note = "obfuscated names (R8) can change between builds, so renamed classes show as removed + added",
                            ),
                            sets("strings", a.set(STRINGS), b.set(STRINGS), note = "distinct string constants loaded by code"),
                            maps("resources", a.pairs(RESOURCES), b.pairs(RESOURCES)),
                        ),
                )
            }
        }

    private fun meta(index: IndexReader): AppMeta {
        val json =
            index
                .query("SELECT value FROM meta WHERE key = 'meta_json'")
                .rows
                .single()
                .single()
                .toString()
        return SextantJson.reader.decodeFromString(AppMeta.serializer(), json)
    }

    private fun appFacts(m: AppMeta): Map<String, String> =
        linkedMapOf(
            "version_name" to m.versionName,
            "version_code" to m.versionCode?.toString(),
            "min_sdk" to m.minSdk?.toString(),
            "target_sdk" to m.targetSdk?.toString(),
            "compile_sdk" to m.compileSdk?.toString(),
            "label" to m.label,
            "application_class" to m.applicationClass,
            "debuggable" to m.flags.debuggable?.toString(),
            "allow_backup" to m.flags.allowBackup?.toString(),
            "uses_cleartext_traffic" to m.flags.usesCleartextTraffic?.toString(),
            "network_security_config" to m.flags.networkSecurityConfig,
            "signing_schemes" to m.signing.schemes.joinToString(),
            "signing_certificates" to m.signing.certificates.joinToString { it.sha256 },
            "dex_files" to m.layout.dexFiles.toString(),
            "native_abis" to m.layout.nativeAbis.joinToString(),
            "android_gradle_plugin" to m.insights?.androidGradlePlugin,
        ).mapValues { it.value ?: "(not set)" }

    private fun IndexReader.set(sql: String): Set<String> = query(sql).rows.mapTo(HashSet()) { it[0].toString() }

    private fun IndexReader.pairs(sql: String): Map<String, String> = query(sql).rows.associate { it[0].toString() to it[1].toString() }

    private fun sets(
        name: String,
        old: Set<String>,
        new: Set<String>,
        note: String? = null,
    ): DiffSection {
        val added = (new - old).sorted()
        val removed = (old - new).sorted()
        return DiffSection(name, added.size, removed.size, 0, added.take(MAX_ITEMS), removed.take(MAX_ITEMS), emptyList(), note)
    }

    private fun maps(
        name: String,
        old: Map<String, String>,
        new: Map<String, String>,
        note: String? = null,
    ): DiffSection {
        val added = (new.keys - old.keys).sorted()
        val removed = (old.keys - new.keys).sorted()
        val changed =
            (old.keys intersect new.keys).filter { old[it] != new[it] }.sorted().map {
                Change(
                    it,
                    old.getValue(it),
                    new.getValue(it),
                )
            }
        return DiffSection(
            name,
            added.size,
            removed.size,
            changed.size,
            added.take(MAX_ITEMS).map { "$it = ${new.getValue(it)}" },
            removed.take(MAX_ITEMS).map { "$it = ${old.getValue(it)}" },
            changed.take(MAX_ITEMS),
            note,
        )
    }

    /** Like [maps] for counts, with changes ordered by size of the change. */
    private fun counts(
        name: String,
        old: Map<String, String>,
        new: Map<String, String>,
    ): DiffSection {
        val base = maps(name, old, new)
        val ordered =
            (old.keys intersect new.keys)
                .filter { old[it] != new[it] }
                .sortedWith(
                    compareByDescending<String> { kotlin.math.abs(new.getValue(it).toInt() - old.getValue(it).toInt()) }.thenBy { it },
                ).map { Change(it, old.getValue(it), new.getValue(it)) }
        return base.copy(changed = ordered.take(MAX_ITEMS))
    }

    private const val PERMISSIONS =
        "SELECT kind || ' ' || name, 'max_sdk=' || coalesce(max_sdk, '-') || ' dangerous=' || dangerous || " +
            "coalesce(' protection=' || protection_level, '') FROM permissions"
    private const val COMPONENTS =
        "SELECT type || ' ' || name, 'exported=' || exported || '(' || exported_source || ') enabled=' || enabled || " +
            "coalesce(' permission=' || permission, '') || ' filters=' || intent_filters FROM components"
    private const val DEEP_LINKS = "SELECT url || '  ← ' || component FROM deep_links"
    private const val FRAMEWORKS = "SELECT name FROM frameworks"
    private const val SDKS = "SELECT name || ' (' || category || ')' FROM sdks"
    private const val LIBRARIES = "SELECT \"group\" || ':' || artifact, version FROM libraries"
    private const val HOSTS = "SELECT DISTINCT host FROM endpoints"
    private const val URLS = "SELECT url FROM endpoints"
    private const val NATIVE = "SELECT path, sha256 FROM native_libraries"
    private const val JNI = "SELECT DISTINCT jni_class || '.' || jni_method FROM native_symbols WHERE jni_class IS NOT NULL"
    private const val PACKAGES = "SELECT coalesce(nullif(package, ''), '(default package)'), count(*) FROM classes GROUP BY package"
    private const val CLASSES = "SELECT name FROM classes"
    private const val STRINGS = "SELECT value FROM strings"
    private const val RESOURCES = "SELECT type || '/' || name, value FROM resources"
}
