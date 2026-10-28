package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.insights.Insights
import io.github.rajumark.sextant.core.model.ApkFile
import io.github.rajumark.sextant.core.model.CodeStats
import io.github.rajumark.sextant.core.model.ResourceHarvest
import io.github.rajumark.sextant.core.model.Signing
import java.io.IOException
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Lists every zip entry of an APK with its size, hashes, content type and the resource it is. [decoders] then says
 * which Sextant decoder read each file, taken from what the decoders actually produced, so "listed only" is honest.
 */
object ApkFiles {
    // Zip-bomb guard: entries that inflate past this are listed without a hash.
    private const val MAX_HASH_BYTES = 1L shl 30

    private val DEX = Regex("""classes\d*\.dex""")
    private val SIGNATURE = Regex("""META-INF/(MANIFEST\.MF|[^/]+\.(SF|RSA|DSA|EC))|stamp-cert-sha256""", RegexOption.IGNORE_CASE)

    fun list(
        apk: Path,
        table: List<RawResource>,
        warnings: MutableList<String>,
    ): List<ApkFile> {
        // Shrunk apps rename res/ files, so the resource table is the only way back to `@layout/x`.
        val resources = HashMap<String, Pair<String, String>>()
        for (r in table.sortedWith(compareBy({ it.type }, { it.name }, { it.config }))) {
            val path = r.value ?: continue
            if (path.startsWith("res/")) resources.putIfAbsent(path, "@${r.type}/${r.name}" to r.config)
        }
        var unhashed = 0
        val files =
            ZipFile(apk.toFile()).use { zip ->
                zip
                    .entries()
                    .asSequence()
                    .filterNot { it.isDirectory }
                    .map { entry ->
                        val read = read(zip, entry)
                        if (read.sha256 == null) unhashed++
                        val res = resources[entry.name]
                        ApkFile(
                            path = entry.name,
                            type = type(entry.name),
                            sizeBytes = read.size,
                            compressedBytes = entry.compressedSize,
                            method = if (entry.method == ZipEntry.STORED) "stored" else "deflated",
                            crc32 = "%08x".format(entry.crc),
                            sha256 = read.sha256,
                            magic = read.magic,
                            resource = res?.first,
                            config = res?.second,
                        )
                    }.sortedBy { it.path }
                    .toList()
            }
        if (unhashed > 0) warnings += "$unhashed zip entr${if (unhashed == 1) "y" else "ies"} could not be hashed (unreadable or over 1 GB)"
        return files
    }

    /** Path → decoder, from what each decoder produced. The first decoder named for a path wins. */
    fun decoders(
        code: CodeStats?,
        resourceTable: Boolean,
        signing: Signing?,
        resources: ResourceHarvest,
        insights: Insights?,
    ): Map<String, String> {
        val map = LinkedHashMap<String, String>()
        map["AndroidManifest.xml"] = "manifest"
        if (resourceTable) map["resources.arsc"] = "resource-table"
        code?.dexFiles?.forEach { map.putIfAbsent(it, "code") }
        resources.layouts.forEach { map.putIfAbsent(it.path, "layouts") }
        resources.menus.forEach { map.putIfAbsent(it.path, "menus") }
        resources.navigation.forEach { map.putIfAbsent(it.path, "navigation") }
        resources.xml.forEach { map.putIfAbsent(it.path, "xml-configs") }
        resources.drawables.forEach { map.putIfAbsent(it.path, "drawables") }
        resources.assets.forEach { map.putIfAbsent(it.path, "assets") }
        insights?.nativeLibraries?.forEach { map.putIfAbsent(it.path, "native") }
        insights?.libraries?.forEach { map.putIfAbsent(it.evidence, "libraries") }
        return map
    }

    /** Sets [ApkFile.decodedBy]; META-INF signature files count as read when the v1 scheme was verified. */
    fun markDecoded(
        files: List<ApkFile>,
        decoders: Map<String, String>,
        signing: Signing?,
    ): List<ApkFile> {
        val v1 = signing?.schemes?.contains("v1") == true
        return files.map { f ->
            val by = decoders[f.path] ?: if (v1 && f.type == "signature" && f.path.startsWith("META-INF/")) "signing" else null
            if (by == null) f else f.copy(decodedBy = by)
        }
    }

    internal fun type(path: String): String =
        when {
            path == "AndroidManifest.xml" -> "manifest"
            path == "resources.arsc" -> "resource-table"
            DEX.matches(path) -> "dex"
            path.startsWith("res/") -> "resource"
            path.startsWith("lib/") && path.endsWith(".so") -> "native-library"
            path.startsWith("assets/") -> "asset"
            SIGNATURE.matches(path) -> "signature"
            path.startsWith("META-INF/") && path.endsWith(".version") -> "library-version"
            path.startsWith("META-INF/services/") -> "service-loader"
            path.endsWith(".kotlin_module") || path.endsWith(".kotlin_builtins") || path.startsWith("kotlin/") -> "kotlin-metadata"
            path.startsWith("META-INF/") -> "meta-inf"
            else -> "other"
        }

    private class Read(
        val size: Long,
        val sha256: String?,
        val magic: String?,
    )

    private fun read(
        zip: ZipFile,
        entry: ZipEntry,
    ): Read =
        try {
            zip.getInputStream(entry).use { input ->
                val digest = MessageDigest.getInstance("SHA-256")
                val buffer = ByteArray(1 shl 16)
                val head = ByteArray(16)
                var headSize = 0
                var total = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    if (headSize < head.size) {
                        val take = minOf(n, head.size - headSize)
                        System.arraycopy(buffer, 0, head, headSize, take)
                        headSize += take
                    }
                    total += n
                    if (total > MAX_HASH_BYTES) return Read(entry.size, null, magic(head, headSize))
                    digest.update(buffer, 0, n)
                }
                Read(total, digest.digest().toHex(), magic(head, headSize))
            }
        } catch (e: IOException) {
            Read(entry.size, null, null)
        } catch (e: RuntimeException) {
            Read(entry.size, null, null)
        }

    internal fun magic(
        head: ByteArray,
        size: Int,
    ): String? {
        fun at(
            i: Int,
            vararg bytes: Int,
        ) = size >= i + bytes.size && bytes.indices.all { head[i + it] == bytes[it].toByte() }

        fun ascii(
            i: Int,
            s: String,
        ) = at(i, *s.map { it.code }.toIntArray())
        return when {
            ascii(0, "dex\n") -> "dex"
            at(0, 0x7f, 'E'.code, 'L'.code, 'F'.code) -> "elf"
            at(0, 'P'.code, 'K'.code, 3, 4) || at(0, 'P'.code, 'K'.code, 5, 6) -> "zip"
            at(0, 0x89, 'P'.code, 'N'.code, 'G'.code) -> "png"
            ascii(0, "RIFF") && ascii(8, "WEBP") -> "webp"
            at(0, 0xff, 0xd8, 0xff) -> "jpeg"
            ascii(0, "GIF8") -> "gif"
            at(0, 3, 0, 8, 0) -> "binary-xml"
            at(0, 2, 0, 12, 0) -> "resource-table"
            at(0, 0, 1, 0, 0) || ascii(0, "OTTO") || ascii(0, "true") || ascii(0, "ttcf") || ascii(0, "wOFF") || ascii(0, "wOF2") -> "font"
            size == 0 -> null
            else -> text(head, size)
        }
    }

    /** `xml`, `json` or `text` for printable content; null for other binary data. */
    private fun text(
        head: ByteArray,
        size: Int,
    ): String? {
        val bytes = head.copyOf(size)
        if (bytes.any(::isControl)) return null
        val s = String(bytes, Charsets.UTF_8).trimStart('\uFEFF', ' ', '\n', '\r', '\t')
        return when {
            s.startsWith("<") -> "xml"
            s.startsWith("{") || s.startsWith("[") -> "json"
            else -> "text"
        }
    }

    private fun isControl(byte: Byte): Boolean {
        val b = byte.toInt() and 0xff
        return b < 0x09 || b in 0x0e..0x1f
    }
}
