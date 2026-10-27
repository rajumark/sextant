package io.github.rajumark.sextant.core.output

import io.github.rajumark.sextant.core.CodeSink
import io.github.rajumark.sextant.core.SextantException
import io.github.rajumark.sextant.core.model.CallRecord
import io.github.rajumark.sextant.core.model.ClassRecord
import io.github.rajumark.sextant.core.model.FieldRecord
import io.github.rajumark.sextant.core.model.FieldRefRecord
import io.github.rajumark.sextant.core.model.MethodRecord
import io.github.rajumark.sextant.core.model.StringRecord
import io.github.rajumark.sextant.core.model.TypeRefRecord
import kotlinx.serialization.KSerializer
import java.io.BufferedWriter
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** Code output files, relative to the extraction folder. */
object CodeFiles {
    const val DIR = "code"
    const val CLASSES = "code/classes.jsonl"
    const val METHODS = "code/methods.jsonl"
    const val FIELDS = "code/fields.jsonl"
    const val CALLS = "code/calls.jsonl"
    const val STRINGS = "code/strings.jsonl"
    const val FIELD_REFS = "code/field_refs.jsonl"
    const val TYPE_REFS = "code/type_refs.jsonl"
    val ALL = listOf(CLASSES, METHODS, FIELDS, CALLS, STRINGS, FIELD_REFS, TYPE_REFS)
}

/**
 * Streams code records straight to JSONL files so even very large apps need little memory.
 * Files are written to temp names and only moved into place by [commit]; [close] without commit discards them.
 */
class FileCodeSink(
    private val root: Path,
) : CodeSink,
    AutoCloseable {
    private val classes = JsonlFile(root.resolve(CodeFiles.CLASSES), ClassRecord.serializer())
    private val methods = JsonlFile(root.resolve(CodeFiles.METHODS), MethodRecord.serializer())
    private val fields = JsonlFile(root.resolve(CodeFiles.FIELDS), FieldRecord.serializer())
    private val calls = JsonlFile(root.resolve(CodeFiles.CALLS), CallRecord.serializer())
    private val strings = JsonlFile(root.resolve(CodeFiles.STRINGS), StringRecord.serializer())
    private val fieldRefs = JsonlFile(root.resolve(CodeFiles.FIELD_REFS), FieldRefRecord.serializer())
    private val typeRefs = JsonlFile(root.resolve(CodeFiles.TYPE_REFS), TypeRefRecord.serializer())
    private val all = listOf(classes, methods, fields, calls, strings, fieldRefs, typeRefs)
    private var committed = false

    override fun classRecord(record: ClassRecord) = classes.write(record)

    override fun method(record: MethodRecord) = methods.write(record)

    override fun field(record: FieldRecord) = fields.write(record)

    override fun call(record: CallRecord) = calls.write(record)

    override fun string(record: StringRecord) = strings.write(record)

    override fun fieldRef(record: FieldRefRecord) = fieldRefs.write(record)

    override fun typeRef(record: TypeRefRecord) = typeRefs.write(record)

    fun commit(): List<Path> {
        all.forEach { it.commit() }
        committed = true
        return all.map { it.target }
    }

    override fun close() {
        if (!committed) all.forEach { it.discard() }
    }

    private class JsonlFile<T>(
        val target: Path,
        private val serializer: KSerializer<T>,
    ) {
        private val tmp: Path
        private val writer: BufferedWriter

        init {
            try {
                Files.createDirectories(target.parent)
                tmp = Files.createTempFile(target.parent, ".${target.fileName}.", ".tmp")
                writer = Files.newBufferedWriter(tmp)
            } catch (e: IOException) {
                throw SextantException.output("cannot write ${target.parent}: ${e.message}", cause = e)
            }
        }

        fun write(record: T) {
            writer.write(SextantJson.compact.encodeToString(serializer, record).escapeLoneSurrogates())
            writer.write('\n'.code)
        }

        fun commit() {
            try {
                writer.close()
                tmp.makeReadable()
                Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            } catch (e: IOException) {
                throw SextantException.output("cannot write $target: ${e.message}", cause = e)
            }
        }

        fun discard() {
            runCatching { writer.close() }
            runCatching { Files.deleteIfExists(tmp) }
        }
    }
}
