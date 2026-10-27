package io.github.rajumark.sextant.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.CoreCliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.clikt.parameters.types.path
import io.github.rajumark.sextant.core.Extractor
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.insights.AppIcons
import io.github.rajumark.sextant.core.insights.IconFile
import io.github.rajumark.sextant.core.insights.IconRef
import io.github.rajumark.sextant.core.model.AppMeta
import io.github.rajumark.sextant.core.output.OutputFiles
import io.github.rajumark.sextant.core.output.SextantJson
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

/** `sextant icon <apk|dir>`: the app's launcher icon files, the best one, and optionally save it. */
class IconCommand : CoreCliktCommand(name = "icon") {
    private val target by argument(help = "An APK (fast: manifest + resources only) or an extracted folder").path()
    private val round by option("--round", help = "Use android:roundIcon instead of android:icon").flag()
    private val layer by option(
        "--layer",
        help = "For adaptive icons: pick a layer instead of the full icon",
    ).choice("foreground", "background", "monochrome")
    private val out by option("-o", "--out", help = "Save the chosen file here (needs the APK)").path()
    private val json by option("--json", help = "All icon data as JSON").flag()

    override fun help(context: Context) =
        "Find the app icon: every file behind android:icon (or --round) per density, adaptive icon layers, pixel sizes, " +
            "and the best file to use. -o saves it."

    override fun run() =
        guarded {
            val isApk = Files.isRegularFile(target)
            val icons =
                if (isApk) {
                    Extractor.icons(target)
                } else {
                    val metaFile = target.resolve(OutputFiles.META)
                    if (!Files.isRegularFile(metaFile)) throw SextantException.input("$target is not an APK or an extraction folder")
                    SextantJson.reader.decodeFromString(AppMeta.serializer(), Files.readString(metaFile)).icons
                        ?: throw SextantException.input("no icon data in $target", "re-extract with sextant 0.9.0 or newer")
                }
            if (json) {
                println(SextantJson.compact.encodeToString(AppIcons.serializer(), icons))
                return@guarded
            }
            val ref =
                (if (round) icons.roundIcon else icons.icon)
                    ?: throw SextantException.input("the app declares no ${if (round) "android:roundIcon" else "android:icon"}")
            val (label, files, best) = chosen(ref)
            println(label)
            for (f in files) {
                val size = if (f.width != null) "${f.width}×${f.height}" else "-"
                val mark = if (f.path == best) "  ← best" else ""
                println(
                    "  %-14s %-5s %-10s %8d B  %s%s".format(f.config.ifEmpty { "(default)" }, f.format, size, f.sizeBytes, f.path, mark),
                )
            }
            ref.adaptive?.takeIf { layer == null }?.let { a ->
                println("adaptive icon  ${a.path}")
                for ((name, l) in listOf("background" to a.background, "foreground" to a.foreground, "monochrome" to a.monochrome)) {
                    if (l != null) println("  %-11s %s  %s".format(name, l.resource, l.best ?: l.color ?: "(not found)"))
                }
            }
            val dest = out
            if (dest != null) save(best, files, dest)
        }

    /** The icon itself, or one adaptive layer when --layer is given. */
    private fun chosen(ref: IconRef): Triple<String, List<IconFile>, String?> {
        val l = layer ?: return Triple("${if (round) "round icon" else "icon"}  ${ref.resource}", ref.files, ref.best)
        val a = ref.adaptive ?: throw SextantException.input("${ref.resource} is not an adaptive icon, so it has no $l layer")
        val picked =
            when (l) {
                "foreground" -> a.foreground
                "background" -> a.background
                else -> a.monochrome
            } ?: throw SextantException.input("the adaptive icon has no $l layer")
        if (picked.files.isEmpty() && picked.color != null) {
            println("$l layer is a solid color: ${picked.color}")
        }
        return Triple("$l layer  ${picked.resource}", picked.files, picked.best)
    }

    private fun save(
        best: String?,
        files: List<IconFile>,
        dest: Path,
    ) {
        if (!Files.isRegularFile(
                target,
            )
        ) {
            throw SextantException.input("-o needs the APK, not an extraction folder", "run `sextant icon app.apk -o icon.png`")
        }
        val path = best ?: throw SextantException.input("no icon file to save")
        if (files.firstOrNull { it.path == path }?.format == "xml") {
            throw SextantException.input(
                "the best file is a vector/XML drawable ($path), not a bitmap",
                "try --layer foreground, or --round; vector drawables are compiled binary XML",
            )
        }
        ZipFile(target.toFile()).use { zip ->
            val entry = zip.getEntry(path) ?: throw SextantException.input("$path is not in the APK")
            zip.getInputStream(entry).use { input -> Files.newOutputStream(dest).use { input.copyTo(it) } }
        }
        System.err.println("Saved $path → $dest")
    }
}
