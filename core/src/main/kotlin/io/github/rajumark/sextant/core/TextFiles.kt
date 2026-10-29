package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.ApkFile
import java.io.StringReader
import java.util.Properties
import java.util.zip.ZipFile

/** One text file of the APK (not a resource, not binary XML): assets, library metadata, licenses, configs. */
class TextFile(
    val path: String,
    val text: String,
    /** Key/value pairs when the file is a Java .properties file. */
    val properties: List<Pair<String, String>>,
    /** For META-INF/services/<interface>: the implementation classes it lists. */
    val services: List<String>,
)

/**
 * Reads the text of every file whose content is text, JSON or textual XML (binary XML is the XML layer's job), so
 * assets, library metadata and configs become searchable rows. Properties files are split into keys and values;
 * service-loader files into the implementations they register.
 */
object TextFiles {
    /** Larger text files (bundled JavaScript, big JSON) are listed and stored as bytes, not as text rows. */
    const val MAX_TEXT_BYTES = 4L * 1024 * 1024

    private val TEXT = setOf("text", "json", "xml")

    fun read(
        zip: ZipFile,
        files: List<ApkFile>,
    ): List<TextFile> =
        files.mapNotNull { f ->
            if (f.magic !in TEXT || f.sizeBytes > MAX_TEXT_BYTES) return@mapNotNull null
            val entry = zip.getEntry(f.path) ?: return@mapNotNull null
            val text =
                runCatching { zip.getInputStream(entry).use { it.readBytes() }.toString(Charsets.UTF_8) }.getOrNull()
                    ?: return@mapNotNull null
            TextFile(f.path, text, if (f.path.endsWith(".properties")) properties(text) else emptyList(), services(f.path, text))
        }

    /** Java properties syntax (escapes, continuation lines), in file order. */
    internal fun properties(text: String): List<Pair<String, String>> {
        val ordered = LinkedHashMap<String, String>()
        object : Properties() {
            override fun put(
                key: Any,
                value: Any,
            ): Any? {
                ordered[key.toString()] = value.toString()
                return super.put(key, value)
            }
        }.load(StringReader(text))
        return ordered.toList()
    }

    /** `META-INF/services/<interface>`: one implementation class per line, `#` starts a comment. */
    internal fun services(
        path: String,
        text: String,
    ): List<String> {
        if (!path.startsWith("META-INF/services/")) return emptyList()
        return text
            .lines()
            .map { it.substringBefore('#').trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }
}
