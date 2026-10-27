package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.CallRecord
import io.github.rajumark.sextant.core.model.ClassRecord
import io.github.rajumark.sextant.core.model.FieldRecord
import io.github.rajumark.sextant.core.model.FieldRefRecord
import io.github.rajumark.sextant.core.model.MethodRecord
import io.github.rajumark.sextant.core.model.StringRecord
import io.github.rajumark.sextant.core.model.TypeRefRecord
import io.github.rajumark.sextant.core.output.CodeFiles
import io.github.rajumark.sextant.core.output.SextantJson
import io.github.rajumark.sextant.core.output.escapeLoneSurrogates
import kotlinx.serialization.KSerializer
import java.security.MessageDigest

/**
 * Hashes code records exactly as FileCodeSink would write them (JSON + escapes + "\n", UTF-8), without writing
 * anything. Lets golden tests pin hundreds of MB of code output per APK using no disk.
 */
class CodeDigestSink : CodeSink {
    private class Stream {
        val digest: MessageDigest = MessageDigest.getInstance("SHA-256")
        var lines = 0L
    }

    private val streams = CodeFiles.ALL.associateWith { Stream() }

    private fun <T> add(
        file: String,
        serializer: KSerializer<T>,
        record: T,
    ) {
        val s = streams.getValue(file)
        s.digest.update((SextantJson.compact.encodeToString(serializer, record).escapeLoneSurrogates() + "\n").toByteArray(Charsets.UTF_8))
        s.lines++
    }

    override fun classRecord(record: ClassRecord) = add(CodeFiles.CLASSES, ClassRecord.serializer(), record)

    override fun method(record: MethodRecord) = add(CodeFiles.METHODS, MethodRecord.serializer(), record)

    override fun field(record: FieldRecord) = add(CodeFiles.FIELDS, FieldRecord.serializer(), record)

    override fun call(record: CallRecord) = add(CodeFiles.CALLS, CallRecord.serializer(), record)

    override fun string(record: StringRecord) = add(CodeFiles.STRINGS, StringRecord.serializer(), record)

    override fun fieldRef(record: FieldRefRecord) = add(CodeFiles.FIELD_REFS, FieldRefRecord.serializer(), record)

    override fun typeRef(record: TypeRefRecord) = add(CodeFiles.TYPE_REFS, TypeRefRecord.serializer(), record)

    /** Same line format as before: `<sha256>  <lines>  <file>`, in CodeFiles.ALL order. */
    fun lines(): String =
        streams.entries.joinToString("") { (name, s) ->
            "${s.digest.digest().toHex()}  ${"%9d".format(s.lines)}  $name\n"
        }
}
