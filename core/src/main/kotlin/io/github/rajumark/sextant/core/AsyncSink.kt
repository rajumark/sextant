package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.AnnotationRecord
import io.github.rajumark.sextant.core.model.CallRecord
import io.github.rajumark.sextant.core.model.ClassRecord
import io.github.rajumark.sextant.core.model.FieldRecord
import io.github.rajumark.sextant.core.model.FieldRefRecord
import io.github.rajumark.sextant.core.model.MethodCodeRecord
import io.github.rajumark.sextant.core.model.MethodDetailRecord
import io.github.rajumark.sextant.core.model.MethodRecord
import io.github.rajumark.sextant.core.model.ResourceUseRecord
import io.github.rajumark.sextant.core.model.StringRecord
import io.github.rajumark.sextant.core.model.TypeRefRecord
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Feeds a slow [CodeSink] (the SQLite index) from a background thread, so reading the APK and writing JSONL
 * do not wait for it. Order is preserved. [await] must be called before the delegate is used again; it rethrows
 * any failure from the worker.
 */
class AsyncSink(
    private val delegate: CodeSink,
    capacity: Int = 16_384,
) : CodeSink,
    AutoCloseable {
    private val queue = ArrayBlockingQueue<Any>(capacity)

    @Volatile private var failure: Throwable? = null
    private val worker =
        Thread({ drain() }, "sextant-index").apply {
            isDaemon = true
            start()
        }

    override fun classRecord(record: ClassRecord) = put(record)

    override fun method(record: MethodRecord) = put(record)

    override fun field(record: FieldRecord) = put(record)

    override fun call(record: CallRecord) = put(record)

    override fun string(record: StringRecord) = put(record)

    override fun fieldRef(record: FieldRefRecord) = put(record)

    override fun typeRef(record: TypeRefRecord) = put(record)

    override fun resourceUse(record: ResourceUseRecord) = put(record)

    override fun annotation(record: AnnotationRecord) = put(record)

    override fun methodDetail(record: MethodDetailRecord) = put(record)

    override val wantsInstructions: Boolean get() = delegate.wantsInstructions

    override fun methodCode(record: MethodCodeRecord) = put(record)

    private fun put(record: Any) {
        // Stop feeding a worker that has already failed; the error surfaces in await().
        while (failure == null) {
            if (queue.offer(record, 100, TimeUnit.MILLISECONDS)) return
        }
    }

    private fun drain() {
        try {
            while (true) {
                when (val r = queue.take()) {
                    END -> return
                    is ClassRecord -> delegate.classRecord(r)
                    is MethodRecord -> delegate.method(r)
                    is FieldRecord -> delegate.field(r)
                    is CallRecord -> delegate.call(r)
                    is StringRecord -> delegate.string(r)
                    is FieldRefRecord -> delegate.fieldRef(r)
                    is TypeRefRecord -> delegate.typeRef(r)
                    is ResourceUseRecord -> delegate.resourceUse(r)
                    is AnnotationRecord -> delegate.annotation(r)
                    is MethodDetailRecord -> delegate.methodDetail(r)
                    is MethodCodeRecord -> delegate.methodCode(r)
                }
            }
        } catch (e: Throwable) {
            failure = e
        }
    }

    /** Waits until every record has reached the delegate. */
    fun await() {
        put(END)
        worker.join()
        failure?.let { throw it }
    }

    override fun close() {
        if (worker.isAlive) {
            queue.clear()
            queue.offer(END)
            worker.join(5_000)
        }
    }

    private object END
}
