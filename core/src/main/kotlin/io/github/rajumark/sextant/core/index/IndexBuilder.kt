package io.github.rajumark.sextant.core.index

import io.github.rajumark.sextant.core.AsyncSink
import io.github.rajumark.sextant.core.Bundles
import io.github.rajumark.sextant.core.Extraction
import io.github.rajumark.sextant.core.Extractor
import io.github.rajumark.sextant.core.SextantException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.name

/** APK in, SQLite out: one pass over the APK into a single index file, the only output Sextant writes. */
object IndexBuilder {
    /**
     * Builds [target] from [apk]. The file is written under a per-process name and moved into place at the end, so a
     * crash or a parallel run never leaves a half-built file at [target]. [includeCode] = false skips the code tables; [includeContent] = false stores the bytes of the dex files only.
     */
    fun build(
        apk: Path,
        target: Path,
        includeCode: Boolean = true,
        includeContent: Boolean = true,
    ): Extraction {
        // Contents come from the APK Sextant reads: for a split bundle, the merged one (cached by Bundles).
        val contents = IndexWriter.Contents(Bundles.open(apk).apk, includeContent)
        val parent = target.toAbsolutePath().parent
        try {
            Files.createDirectories(parent)
        } catch (e: Exception) {
            throw SextantException.output("cannot create $parent: ${e.message}", cause = e)
        }
        val staging = parent.resolve(".${target.name}.${ProcessHandle.current().pid()}.building")
        try {
            val extraction =
                IndexWriter(staging).use { index ->
                    if (includeCode) {
                        AsyncSink(index).use { feed ->
                            val extraction = Extractor.extract(apk, feed, listFiles = true)
                            feed.await()
                            index.finish(extraction, contents)
                            extraction
                        }
                    } else {
                        Extractor.extract(apk, null, listFiles = true).also { index.finish(it, contents) }
                    }
                }
            try {
                Files.move(staging, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (e: Exception) {
                throw SextantException.output("cannot write $target: ${e.message}", cause = e)
            }
            return extraction
        } finally {
            Files.deleteIfExists(staging)
        }
    }
}
