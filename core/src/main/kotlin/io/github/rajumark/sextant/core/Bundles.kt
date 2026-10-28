package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.BundleInfo
import io.github.rajumark.sextant.core.model.BundleSplit
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * What Sextant reads for one input file. For a plain APK all three paths are the file itself. For a split bundle
 * (.apks, .xapk, .apkm) [apk] is one APK merged from the base and its splits (code, native libraries, assets and
 * res files of every split), and [signed] is the base APK as shipped, whose signature is checked.
 */
class ApkInput(
    val original: Path,
    val apk: Path,
    val signed: Path,
    val bundle: BundleInfo?,
)

/**
 * Split bundles: a zip of APKs, as written by bundletool and SAI (.apks), APKPure (.xapk) and APKMirror (.apkm).
 * Nothing is uploaded or installed: the APKs are copied out to a local cache folder, keyed by the bundle's hash, and
 * merged once. resources.arsc comes from the base only, so resources that exist only in a density or language split
 * are listed as files but not decoded.
 */
object Bundles {
    private val DEX = Regex("""classes(\d*)\.dex""")
    private val ABIS = setOf("armeabi", "armeabi_v7a", "arm64_v8a", "x86", "x86_64", "mips", "mips64")
    private val DENSITIES = setOf("ldpi", "mdpi", "tvdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi", "nodpi")

    // Zip-bomb guard: no single APK inside a bundle may inflate past this.
    private const val MAX_SPLIT_BYTES = 4L * 1024 * 1024 * 1024

    /** True when [path] is a zip that holds APKs but no AndroidManifest.xml of its own. */
    fun isBundle(path: Path): Boolean =
        Files.isRegularFile(path) &&
            runCatching {
                ZipFile(path.toFile()).use { z ->
                    z.getEntry("AndroidManifest.xml") == null && z.entries().asSequence().any { isApkEntry(it) }
                }
            }.getOrDefault(false)

    /** Resolves any input: a plain APK is used as is; a bundle is copied out and merged (cached). */
    fun open(path: Path): ApkInput {
        if (!isBundle(path)) return ApkInput(path, path, path, null)
        val hash = ApkReader.sha256(path)
        val dir = cacheRoot().resolve("bundles").resolve(hash.take(24))
        Files.createDirectories(dir)
        ZipFile(path.toFile()).use { zip ->
            val apks =
                zip
                    .entries()
                    .asSequence()
                    .filter { isApkEntry(it) }
                    .toList()
            val baseEntry = pickBase(zip, apks)
            val splits = apks.filter { it != baseEntry }.sortedBy { it.name }
            val info =
                BundleInfo(
                    format = formatOf(path, zip),
                    base = baseEntry.name,
                    splits = splits.map { BundleSplit(it.name, kindOf(it.name), configOf(it.name), it.size) },
                )
            val base = dir.resolve("base.apk")
            val merged = dir.resolve("merged.apk")
            if (!Files.isRegularFile(merged)) {
                copyOut(zip, baseEntry, base)
                val splitFiles =
                    splits.mapIndexed { i, e -> dir.resolve("split-$i.apk").also { copyOut(zip, e, it) } }
                val tmp = dir.resolve("merged.apk.tmp")
                merge(base, splitFiles, tmp)
                Files.move(tmp, merged, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                splitFiles.forEach { Files.deleteIfExists(it) }
            } else if (!Files.isRegularFile(base)) {
                copyOut(zip, baseEntry, base)
            }
            return ApkInput(path, merged, base, info)
        }
    }

    /** "apkm · base base.apk · 5 splits (4 abi, 1 feature)". */
    fun describe(b: BundleInfo): String =
        "${b.format} · base ${b.base} · ${b.splits.size} splits" +
            if (b.splits.isEmpty()) {
                ""
            } else {
                b.splits
                    .groupingBy {
                        it.kind
                    }.eachCount()
                    .entries
                    .joinToString(", ", " (", ")") { "${it.value} ${it.key}" }
            }

    /** Sextant's local cache: `-Dsextant.cache=…`, else the system temp folder. Nothing in it leaves the machine. */
    fun cacheRoot(): Path =
        System.getProperty("sextant.cache")?.let { Path.of(it) }
            ?: Path.of(System.getProperty("java.io.tmpdir"), "sextant-cache")

    private fun isApkEntry(e: ZipEntry) = !e.isDirectory && e.name.endsWith(".apk", ignoreCase = true) && !e.name.contains("..")

    /** The base APK: by name first (base.apk, base-master.apk, <package>.apk in an .xapk), else the one with classes.dex. */
    private fun pickBase(
        zip: ZipFile,
        apks: List<ZipEntry>,
    ): ZipEntry {
        fun file(e: ZipEntry) = e.name.substringAfterLast('/').lowercase()
        apks.firstOrNull { file(it) == "base.apk" || file(it) == "base-master.apk" }?.let { return it }
        val xapkPackage =
            zip.getEntry("manifest.json")?.let { m ->
                zip
                    .getInputStream(m)
                    .use { String(it.readNBytes(1 shl 20)) }
                    .let { Regex(""""package_name"\s*:\s*"([^"]+)"""").find(it)?.groupValues?.get(1) }
            }
        xapkPackage?.let { p -> apks.firstOrNull { file(it) == "$p.apk".lowercase() }?.let { return it } }
        apks
            .filter { !file(it).startsWith("config.") && !file(it).startsWith("split_") && "-" !in file(it) }
            .singleOrNull()
            ?.let { return it }
        return apks.maxBy { it.size }
    }

    private fun formatOf(
        path: Path,
        zip: ZipFile,
    ): String =
        when {
            path.fileName.toString().endsWith(".xapk", true) || zip.getEntry("manifest.json") != null -> "xapk"
            path.fileName.toString().endsWith(".apkm", true) || zip.getEntry("info.json") != null -> "apkm"
            else -> "apks"
        }

    /** "arm64_v8a" from config.arm64_v8a.apk, split_config.arm64_v8a.apk or splits/base-arm64_v8a.apk. */
    internal fun configOf(name: String): String? {
        val file = name.substringAfterLast('/').removeSuffix(".apk").removeSuffix(".APK")
        return when {
            file.startsWith("split_config.") -> file.removePrefix("split_config.")
            file.startsWith("config.") -> file.removePrefix("config.")
            file.startsWith("base-") && file != "base-master" -> file.removePrefix("base-")
            else -> null
        }
    }

    /** abi, density, language or feature (a dynamic feature module, which can carry its own code). */
    internal fun kindOf(name: String): String {
        val c = configOf(name)?.replace('-', '_')
        return when {
            c == null -> "feature"
            c in ABIS -> "abi"
            c in DENSITIES -> "density"
            Regex("[a-z]{2,3}(_r?[a-zA-Z]{2})?").matches(c) -> "language"
            else -> "feature"
        }
    }

    private fun copyOut(
        zip: ZipFile,
        entry: ZipEntry,
        to: Path,
    ) {
        try {
            zip.getInputStream(entry).use { input -> Files.newOutputStream(to).use { copyCapped(input, it, entry.name) } }
        } catch (e: IOException) {
            throw SextantException.input("cannot read ${entry.name} from the bundle: ${e.message}", cause = e)
        }
    }

    private fun copyCapped(
        input: InputStream,
        out: java.io.OutputStream,
        name: String,
    ) {
        val buf = ByteArray(1 shl 16)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) return
            total += n
            if (total > MAX_SPLIT_BYTES) throw SextantException.input("$name is larger than 4 GB after decompression (possible zip bomb)")
            out.write(buf, 0, n)
        }
    }

    /**
     * Base entries first, unchanged. Then each split adds what the base lacks: its dex files under the next free
     * classesN.dex names, and lib/, assets/ and res/ files. Split manifests, resource tables and signatures are left out.
     */
    private fun merge(
        base: Path,
        splits: List<Path>,
        to: Path,
    ) {
        val seen = HashSet<String>()
        var dexCount = 0
        ZipOutputStream(Files.newOutputStream(to)).use { out ->
            fun add(
                name: String,
                input: InputStream,
            ) {
                if (!seen.add(name)) return
                out.putNextEntry(ZipEntry(name))
                copyCapped(input, out, name)
                out.closeEntry()
            }
            ZipFile(base.toFile()).use { z ->
                z.entries().asSequence().filter { !it.isDirectory }.forEach { e ->
                    if (DEX.matches(e.name)) dexCount++
                    z.getInputStream(e).use { add(e.name, it) }
                }
            }
            for (split in splits) {
                ZipFile(split.toFile()).use { z ->
                    z.entries().asSequence().filter { !it.isDirectory }.sortedBy { it.name }.forEach { e ->
                        val n = e.name
                        when {
                            DEX.matches(n) -> {
                                dexCount++
                                z.getInputStream(e).use { add(if (dexCount == 1) "classes.dex" else "classes$dexCount.dex", it) }
                            }
                            n.startsWith("lib/") || n.startsWith("assets/") || n.startsWith("res/") ->
                                z.getInputStream(e).use { add(n, it) }
                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}
