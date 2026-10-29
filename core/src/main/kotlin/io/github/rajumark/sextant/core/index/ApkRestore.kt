package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.Bundles
import io.github.rajumark.sextant.core.SextantException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Rebuilds an APK from the bytes stored in a .sqlite file (`file_contents`), so commands that need the code itself
 * (smali, source, grep) work from the file alone. Every entry gets its stored bytes and its original compression; the
 * zip is not re-signed, so it is for reading, not for installing. Cached by the APK's SHA-256, written once.
 */
object ApkRestore {
    fun restore(index: Path): Path =
        IndexReader.open(index).use { reader ->
            val sha = reader.meta("sha256") ?: throw SextantException.input("$index has no sha256 in meta", "re-extract the APK")
            val target = Bundles.cacheRoot().resolve("restored").resolve("${sha.take(24)}.apk")
            if (Files.isRegularFile(target)) return@use target
            Files.createDirectories(target.parent)
            val staging = target.resolveSibling(".${target.fileName}.${ProcessHandle.current().pid()}.building")
            try {
                var written = 0
                ZipOutputStream(Files.newOutputStream(staging)).use { zip ->
                    reader.fileContents { path, stored, bytes ->
                        val entry = ZipEntry(path)
                        if (stored) {
                            entry.method = ZipEntry.STORED
                            entry.size = bytes.size.toLong()
                            entry.compressedSize = bytes.size.toLong()
                            entry.crc = CRC32().also { it.update(bytes) }.value
                        }
                        zip.putNextEntry(entry)
                        zip.write(bytes)
                        zip.closeEntry()
                        written++
                    }
                }
                if (written == 0) {
                    throw SextantException.input(
                        "$index holds no file contents (built by an older Sextant)",
                        "run `sextant extract <app.apk> --force` again, or pass the APK itself",
                    )
                }
                Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                target
            } finally {
                Files.deleteIfExists(staging)
            }
        }
}
