package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.model.AppMeta
import io.github.rajumark.sextant.core.model.LibraryRecord
import io.github.rajumark.sextant.core.model.SdkRecord
import io.github.rajumark.sextant.core.output.OutputFiles
import io.github.rajumark.sextant.core.output.SextantJson
import kotlinx.serialization.Serializable

/** One SDK or library in one app: what it is for, its version when an artifact proves it, and the evidence. */
@Serializable
data class SdkEntry(
    /** SDK name ("Firebase Analytics") or Maven coordinates ("androidx.room:room-runtime"). */
    val name: String,
    /** sdk (recognized product, from package rules) | library (versioned artifact with no SDK rule) */
    val kind: String,
    /** analytics | ads | crash-reporting | auth | database | ui | networking | … ; libraries get one from their group. */
    val category: String,
    val version: String?,
    /** Where the version comes from (a META-INF *.version file, pom.properties, a *.properties file or a code string). */
    val versionEvidence: String?,
    /** Classes in the app's code under the SDK's packages (0 for libraries known only from artifacts). */
    val classes: Int,
    val artifacts: List<String>,
    /** Top packages where its classes live. */
    val packages: List<String> = emptyList(),
)

/** Every SDK and library of one app, grouped by role. The unit `sextant sdks` compares across apps. */
@Serializable
data class SdkProfile(
    val app: String,
    val packageName: String?,
    val versionName: String?,
    val entries: List<SdkEntry>,
    /** Categories present, sorted. */
    val categories: List<String>,
)

object SdkInventory {
    /** SDK name → artifact coordinate prefixes that carry its version. */
    private val ARTIFACTS =
        mapOf(
            "Firebase Analytics" to listOf("com.google.firebase:firebase-analytics", "com.google.android.gms:play-services-measurement"),
            "Firebase Crashlytics" to listOf("com.google.firebase:firebase-crashlytics"),
            "Firebase Cloud Messaging" to listOf("com.google.firebase:firebase-messaging"),
            "Firebase Core" to listOf("com.google.firebase:firebase-common", "com.google.firebase:firebase-components"),
            "Firebase Performance" to listOf("com.google.firebase:firebase-perf"),
            "Firebase Remote Config" to listOf("com.google.firebase:firebase-config"),
            "Firebase Auth" to listOf("com.google.firebase:firebase-auth"),
            "Firebase Firestore" to listOf("com.google.firebase:firebase-firestore"),
            "Google Mobile Ads (AdMob)" to listOf("com.google.android.gms:play-services-ads"),
            "Google Play Services Base" to listOf("com.google.android.gms:play-services-base", "com.google.android.gms:play-services-basement"),
            "Google Maps" to listOf("com.google.android.gms:play-services-maps"),
            "Google Sign-In" to listOf("com.google.android.gms:play-services-auth"),
            "Google Play Billing" to listOf("com.android.billingclient:billing"),
            "Google ML Kit" to listOf("com.google.mlkit:", "com.google.android.gms:play-services-mlkit"),
            "OkHttp" to listOf("com.squareup.okhttp3:okhttp"),
            "Retrofit" to listOf("com.squareup.retrofit2:retrofit"),
            "Gson" to listOf("com.google.code.gson:gson"),
            "Moshi" to listOf("com.squareup.moshi:moshi"),
            "kotlinx.serialization" to listOf("org.jetbrains.kotlinx:kotlinx-serialization", "kotlinx:kotlinx-serialization"),
            "Kotlin Coroutines" to listOf("org.jetbrains.kotlinx:kotlinx-coroutines", "kotlinx:kotlinx-coroutines-core"),
            "Room" to listOf("androidx.room:room-runtime", "androidx.room:"),
            "WorkManager" to listOf("androidx.work:work-runtime", "androidx.work:"),
            "Jetpack Compose" to listOf("androidx.compose.ui:ui", "androidx.compose.runtime:runtime"),
            "Material Components" to listOf("com.google.android.material:material"),
            "Dagger / Hilt" to listOf("com.google.dagger:"),
            "ExoPlayer / Media3" to listOf("androidx.media3:media3-exoplayer", "com.google.android.exoplayer:"),
            "Protocol Buffers" to listOf("com.google.protobuf:"),
            "Glide" to listOf("com.github.bumptech.glide:glide"),
            "Coil" to listOf("io.coil-kt:coil"),
            "Sentry" to listOf("io.sentry:sentry"),
            "Tink" to listOf("com.google.crypto.tink:"),
            "Lottie" to listOf("com.airbnb.android:lottie"),
            "Ktor" to listOf("io.ktor:"),
        )

    /** Library group → category, for artifacts no SDK rule claims. Longest prefix wins. */
    private val GROUP_CATEGORY =
        listOf(
            "androidx.compose" to "ui", "androidx.appcompat" to "ui", "androidx.constraintlayout" to "ui", "androidx.recyclerview" to "ui",
            "com.google.android.material" to "ui", "androidx.fragment" to "ui", "androidx.activity" to "ui", "androidx.navigation" to "ui",
            "androidx.room" to "database", "androidx.sqlite" to "database", "androidx.datastore" to "storage", "androidx.work" to "background-work",
            "androidx.camera" to "media", "androidx.media3" to "media", "androidx.media" to "media", "androidx.biometric" to "auth",
            "androidx.security" to "cryptography", "androidx.credentials" to "auth", "androidx.browser" to "browser",
            "androidx.lifecycle" to "architecture", "androidx.savedstate" to "architecture", "androidx.startup" to "architecture",
            "com.google.firebase" to "firebase", "com.google.android.gms" to "google-play-services", "com.google.mlkit" to "machine-learning",
            "kotlinx" to "kotlin", "org.jetbrains.kotlinx" to "kotlin", "com.squareup.okhttp3" to "networking", "com.squareup.retrofit2" to "networking",
            "com.google.code.gson" to "serialization", "com.google.protobuf" to "serialization", "com.google.dagger" to "dependency-injection",
            "androidx" to "androidx",
        ).sortedByDescending { it.first.length }

    /** Reads an extracted folder's meta.json, insights/sdks.jsonl and insights/libraries.jsonl. */
    fun fromFolder(dir: java.nio.file.Path): SdkProfile {
        val meta = SextantJson.reader.decodeFromString(AppMeta.serializer(), java.nio.file.Files.readString(dir.resolve(OutputFiles.META)))

        fun <T> lines(
            name: String,
            parse: (String) -> T,
        ) = dir.resolve(name).let { f ->
            if (java.nio.file.Files.isRegularFile(f)) java.nio.file.Files.readAllLines(f).filter { it.isNotBlank() }.map(parse) else emptyList()
        }
        return of(
            dir.fileName.toString(),
            meta.`package`,
            meta.versionName,
            lines(OutputFiles.SDKS) { SextantJson.reader.decodeFromString(SdkRecord.serializer(), it) },
            lines(OutputFiles.LIBRARIES) { SextantJson.reader.decodeFromString(LibraryRecord.serializer(), it) },
        )
    }

    fun categoryOfGroup(group: String): String = GROUP_CATEGORY.firstOrNull { group == it.first || group.startsWith(it.first + ".") }?.second ?: "library"

    fun of(
        app: String,
        packageName: String?,
        versionName: String?,
        sdks: List<SdkRecord>,
        libraries: List<LibraryRecord>,
    ): SdkProfile {
        val coords = libraries.associateBy { "${it.group}:${it.artifact}" }
        val claimed = HashSet<String>()
        val entries = mutableListOf<SdkEntry>()
        for (s in sdks) {
            val matches = ARTIFACTS[s.name].orEmpty().flatMap { prefix -> coords.keys.filter { it.startsWith(prefix) } }.distinct().sorted()
            claimed += matches
            val best = matches.firstNotNullOfOrNull { coords[it] }
            entries += SdkEntry(s.name, "sdk", s.category, best?.version, best?.evidence, s.classes, matches, s.packages)
        }
        for ((coord, lib) in coords.toSortedMap()) {
            if (coord in claimed) continue
            entries += SdkEntry(coord, "library", categoryOfGroup(lib.group), lib.version, lib.evidence, 0, listOf(coord))
        }
        return SdkProfile(
            app,
            packageName,
            versionName,
            entries.sortedWith(compareBy({ it.kind }, { it.category }, { it.name })),
            entries.filter { it.kind == "sdk" }.map { it.category }.distinct().sorted(),
        )
    }

    /**
     * Compares dotted versions numerically part by part (`4.9.3` < `4.10.0`); a missing part counts as 0 and a
     * non-numeric suffix (`-rc1`) sorts before the release. Null when either side has no digits.
     */
    fun compareVersions(
        a: String,
        b: String,
    ): Int? {
        fun parts(v: String) = Regex("""\d+""").findAll(v.substringBefore('-')).map { it.value.toBigInteger() }.toList()
        val pa = parts(a)
        val pb = parts(b)
        if (pa.isEmpty() || pb.isEmpty()) return null
        for (i in 0 until maxOf(pa.size, pb.size)) {
            val c = (pa.getOrNull(i) ?: java.math.BigInteger.ZERO).compareTo(pb.getOrNull(i) ?: java.math.BigInteger.ZERO)
            if (c != 0) return c
        }
        val sa = '-' in a
        val sb = '-' in b
        return if (sa == sb) 0 else if (sa) -1 else 1
    }
}
