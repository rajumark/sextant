package io.github.rajumark.sextant.core

import com.android.tools.smali.dexlib2.Opcodes
import com.android.tools.smali.dexlib2.dexbacked.DexBackedDexFile
import io.github.rajumark.sextant.core.model.ApkFile
import java.io.ByteArrayInputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/** One entry of an archive inside the APK (a jar, zip or APK in assets or elsewhere). */
class ArchiveEntry(
    val archive: String,
    val path: String,
    val sizeBytes: Long,
    val crc32: String,
    val sha256: String,
    val magic: String?,
)

/** One class of dex code that is not one of the APK's own classesN.dex: inside a nested archive, or a dex file in assets. */
class HiddenClass(
    /** The file inside the APK that holds it. */
    val container: String,
    /** For dex inside an archive: the entry (classes.dex); null when the container is the dex file itself. */
    val entry: String?,
    val descriptor: String,
    val superclass: String?,
    val methods: Int,
)

class NestedCode(
    val entries: List<ArchiveEntry> = emptyList(),
    val classes: List<HiddenClass> = emptyList(),
)

/**
 * Looks inside archives and dex files the APK carries besides its own code: every entry of a nested zip/jar/APK
 * (one level deep), and every class of dex code found in them or loose in the APK. This is code the app can load at
 * run time, so it matters for review even though Android does not load it at install.
 */
object NestedArchives {
    private const val MAX_ARCHIVE_BYTES = 256L * 1024 * 1024
    private const val MAX_ENTRY_BYTES = 64L * 1024 * 1024
    private val ROOT_DEX = Regex("""classes\d*\.dex""")

    fun read(
        zip: ZipFile,
        files: List<ApkFile>,
        warnings: MutableList<String>,
    ): NestedCode {
        val entries = mutableListOf<ArchiveEntry>()
        val classes = mutableListOf<HiddenClass>()
        var failed = 0
        for (f in files) {
            if (f.sizeBytes > MAX_ARCHIVE_BYTES) continue
            val nestedArchive = f.magic == "zip"
            val looseDex = f.magic == "dex" && !ROOT_DEX.matches(f.path)
            if (!nestedArchive && !looseDex) continue
            val bytes =
                runCatching { zip.getEntry(f.path)?.let { e -> zip.getInputStream(e).use { it.readBytes() } } }.getOrNull() ?: continue
            try {
                if (looseDex) {
                    classes += dexClasses(f.path, null, bytes)
                } else {
                    ZipInputStream(ByteArrayInputStream(bytes)).use { input ->
                        while (true) {
                            val e = input.nextEntry ?: break
                            if (e.isDirectory) continue
                            val data = input.readNBytes(MAX_ENTRY_BYTES.toInt())
                            val crc = CRC32().also { it.update(data) }.value
                            val head = data.copyOf(minOf(16, data.size))
                            entries +=
                                ArchiveEntry(
                                    archive = f.path,
                                    path = e.name,
                                    sizeBytes = data.size.toLong(),
                                    crc32 = "%08x".format(crc),
                                    sha256 = MessageDigest.getInstance("SHA-256").digest(data).toHex(),
                                    magic = ApkFiles.magic(head.copyOf(16), head.size),
                                )
                            if (head.size >= 4 &&
                                String(head, 0, 4, Charsets.ISO_8859_1) == "dex\n"
                            ) {
                                classes += dexClasses(f.path, e.name, data)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                failed++
            }
        }
        if (failed > 0) warnings += "$failed nested archive${if (failed == 1) "" else "s"} could not be read"
        return NestedCode(entries.sortedWith(compareBy({ it.archive }, { it.path })), classes)
    }

    private fun dexClasses(
        container: String,
        entry: String?,
        bytes: ByteArray,
    ): List<HiddenClass> =
        DexBackedDexFile(Opcodes.getDefault(), bytes)
            .classes
            .map { c ->
                HiddenClass(container, entry, c.type, c.superclass, c.methods.count())
            }.sortedBy { it.descriptor }
}
