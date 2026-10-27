package io.github.rajumark.sextant.core.output

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy

/** The one JSON configuration used for every file Sextant writes or reads. */
@OptIn(ExperimentalSerializationApi::class)
object SextantJson {
    /** Single-object files (meta.json, manifest.json). */
    val pretty =
        Json {
            prettyPrint = true
            prettyPrintIndent = "  "
            encodeDefaults = true
            explicitNulls = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        }

    /** One record per line (.jsonl) and `--json` output. */
    val compact =
        Json {
            encodeDefaults = true
            explicitNulls = true
            namingStrategy = JsonNamingStrategy.SnakeCase
            ignoreUnknownKeys = true
        }

    /** Reading files back: tolerant of fields added by newer versions. */
    val reader =
        Json {
            namingStrategy = JsonNamingStrategy.SnakeCase
            ignoreUnknownKeys = true
        }
}

/**
 * DEX strings may contain unpaired UTF-16 surrogates, which UTF-8 cannot encode. JSON can: write them as
 * `\uXXXX` escapes so the exact value survives and the file stays valid UTF-8.
 */
internal fun String.escapeLoneSurrogates(): String {
    var i = indexOfFirst { it.isSurrogate() }
    if (i < 0) return this
    val out = StringBuilder(length + 16).append(this, 0, i)
    while (i < length) {
        val c = this[i]
        when {
            c.isHighSurrogate() && i + 1 < length && this[i + 1].isLowSurrogate() -> {
                out.append(c).append(this[i + 1])
                i++
            }
            c.isSurrogate() -> out.append("\\u").append("%04x".format(c.code))
            else -> out.append(c)
        }
        i++
    }
    return out.toString()
}
