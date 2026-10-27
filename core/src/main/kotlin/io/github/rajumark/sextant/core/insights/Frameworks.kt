package io.github.rajumark.sextant.core.insights

import io.github.rajumark.sextant.core.model.FrameworkRecord

/** Detects app frameworks and engines from files, class names and manifest entries. Each hit lists its evidence. */
object Frameworks {
    private class Rule(
        val name: String,
        val files: List<Regex> = emptyList(),
        val classes: List<String> = emptyList(),
        val manifest: List<String> = emptyList(),
    )

    private fun lib(vararg names: String) = names.map { Regex("""lib/[^/]+/${Regex.escape(it)}""") }

    private fun file(vararg patterns: String) = patterns.map { Regex(it) }

    private val RULES =
        listOf(
            Rule("Flutter", lib("libflutter.so") + file("""assets/flutter_assets/.*"""), listOf("io.flutter.embedding")),
            Rule(
                "React Native",
                lib("libreactnativejni.so", "libreactnative.so") + file("""assets/index\.android\.bundle"""),
                listOf("com.facebook.react"),
            ),
            Rule("Hermes (JS engine)", lib("libhermes.so", "libhermes_executor.so", "libhermesvm.so")),
            Rule("Expo", lib("libexpo-modules-core.so"), listOf("expo.modules")),
            Rule(
                ".NET (Xamarin / MAUI)",
                lib("libmonodroid.so", "libmonosgen-2.0.so", "libxamarin-app.so") + file("""assemblies/assemblies.*\.blob"""),
                listOf("mono.android"),
            ),
            Rule("Capacitor", file("""assets/capacitor\.config\.json"""), listOf("com.getcapacitor")),
            Rule("Cordova", file("""assets/www/cordova\.js"""), listOf("org.apache.cordova")),
            Rule("Unity", lib("libunity.so", "libil2cpp.so"), listOf("com.unity3d.player")),
            Rule("Godot", lib("libgodot_android.so"), listOf("org.godotengine.godot")),
            Rule("libGDX", lib("libgdx.so"), listOf("com.badlogic.gdx")),
            Rule("SDL", lib("libSDL2.so", "libSDL3.so"), listOf("org.libsdl.app")),
            Rule("Qt", file("""lib/[^/]+/libQt[56]Core.*\.so"""), listOf("org.qtproject.qt")),
            Rule("Cocos2d-x", lib("libcocos2dcpp.so", "libcocos2djs.so"), listOf("org.cocos2dx")),
            Rule("Unreal Engine", lib("libUE4.so", "libUnreal.so"), listOf("com.epicgames.ue4", "com.epicgames.unreal")),
            Rule("Jetpack Compose", classes = listOf("androidx.compose.runtime")),
            Rule("NativeActivity (C/C++ app)", manifest = listOf("android.app.NativeActivity")),
        )

    fun detect(
        entries: List<String>,
        classNames: List<String>,
        manifestNames: Set<String>,
        kotlinClasses: Int,
    ): List<FrameworkRecord> {
        val found = mutableListOf<FrameworkRecord>()
        for (rule in RULES) {
            val evidence = mutableListOf<String>()
            rule.files.forEach { re ->
                val hits = entries.filter { re.matches(it) }
                if (hits.isNotEmpty()) evidence += "file ${hits.first()}" + if (hits.size > 1) " (+${hits.size - 1} more)" else ""
            }
            rule.classes.forEach { prefix ->
                val n = classNames.count { it == prefix || it.startsWith("$prefix.") }
                if (n > 0) evidence += "$n classes in $prefix"
            }
            rule.manifest.forEach { name -> if (name in manifestNames) evidence += "manifest $name" }
            if (evidence.isNotEmpty()) found += FrameworkRecord(rule.name, evidence)
        }
        if (kotlinClasses > 0) found += FrameworkRecord("Kotlin", listOf("$kotlinClasses classes with kotlin.Metadata"))
        return found.sortedBy { it.name }
    }
}
