package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.FileInfo
import io.github.rajumark.sextant.core.model.Layout
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.util.zip.ZipException
import java.util.zip.ZipFile

/** The raw pieces of an APK that Version 1 needs. Nothing is extracted to disk. */
class ApkContents(
    val path: Path,
    val file: FileInfo,
    val layout: Layout,
    val manifest: ByteArray,
    val resourceTable: ByteArray?,
)

object ApkReader {
    // Hard caps on bytes we are willing to inflate, whatever the zip headers claim (zip-bomb guard).
    private const val MAX_MANIFEST_BYTES = 32L * 1024 * 1024
    private const val MAX_RESOURCE_TABLE_BYTES = 512L * 1024 * 1024

    private val DEX_ENTRY = Regex("""classes\d*\.dex""")
    private val NATIVE_LIB_ENTRY = Regex("""lib/([^/]+)/[^/]+\.so""")

    fun read(path: Path): ApkContents {
        if (!Files.exists(path)) throw SextantException.input("file not found: $path")
        if (Files.isDirectory(path)) throw SextantException.input("$path is a directory, not an APK file")
        if (!Files.isReadable(path)) throw SextantException.input("cannot read $path", "check the file permissions")

        val zip =
            try {
                ZipFile(path.toFile())
            } catch (e: ZipException) {
                throw SextantException.input("$path is not a valid APK (not a zip archive)", cause = e)
            } catch (e: IOException) {
                throw SextantException.input("cannot open $path: ${e.message}", cause = e)
            }

        return zip.use {
            val names =
                zip
                    .entries()
                    .asSequence()
                    .map { it.name }
                    .toList()
            val manifestEntry =
                zip.getEntry("AndroidManifest.xml")
                    ?: throw SextantException.input(
                        "$path has no AndroidManifest.xml",
                        "this looks like a zip file but not an APK; split bundles (.apks/.xapk/.aab) are not supported yet",
                    )
            val manifest = zip.getInputStream(manifestEntry).use { readCapped(it, MAX_MANIFEST_BYTES, "AndroidManifest.xml") }
            val resourceTable =
                zip.getEntry("resources.arsc")?.let { entry ->
                    zip.getInputStream(entry).use { readCapped(it, MAX_RESOURCE_TABLE_BYTES, "resources.arsc") }
                }
            ApkContents(
                path = path,
                file = FileInfo(name = path.fileName.toString(), sizeBytes = Files.size(path), sha256 = sha256(path)),
                layout =
                    Layout(
                        dexFiles = names.count { DEX_ENTRY.matches(it) },
                        nativeAbis = names.mapNotNull { NATIVE_LIB_ENTRY.matchEntire(it)?.groupValues?.get(1) }.distinct().sorted(),
                    ),
                manifest = manifest,
                resourceTable = resourceTable,
            )
        }
    }

    private fun readCapped(
        input: InputStream,
        max: Long,
        name: String,
    ): ByteArray {
        val bytes =
            try {
                input.readNBytes((max + 1).toInt().coerceAtLeast(0))
            } catch (e: IOException) {
                throw SextantException.input("cannot read $name from the APK: ${e.message}", cause = e)
            }
        if (bytes.size > max) {
            throw SextantException.input("$name is larger than ${max / 1024 / 1024} MB after decompression (possible zip bomb)")
        }
        return bytes
    }

    fun sha256(path: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(path).use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().toHex()
    }
}

internal fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
