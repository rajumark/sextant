package io.github.rajumark.sextant.core

import io.github.rajumark.sextant.core.model.CallRecord
import io.github.rajumark.sextant.core.model.ClassRecord
import io.github.rajumark.sextant.core.model.MethodRecord
import io.github.rajumark.sextant.core.model.StringRecord
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

/** Facts about a real, R8-obfuscated corpus APK (Binary Eye), checked by hand with dexdump. */
class CodeExtractorTest {
    private val classes = mutableListOf<ClassRecord>()
    private val methods = mutableListOf<MethodRecord>()
    private val calls = mutableListOf<CallRecord>()
    private val strings = mutableListOf<StringRecord>()
    private val warnings = mutableListOf<String>()

    private val stats =
        CodeExtractor.extract(
            CorpusApks.require("binaryeye"),
            object : CodeSink {
                override fun classRecord(record: ClassRecord) {
                    classes += record
                }

                override fun method(record: MethodRecord) {
                    methods += record
                }

                override fun call(record: CallRecord) {
                    calls += record
                }

                override fun string(record: StringRecord) {
                    strings += record
                }
            },
            warnings,
        )

    private val onResume = "Lde/markusfisch/android/binaryeye/activity/CameraActivity;->onResume()V"

    @Test
    fun `counts match dexdump`() {
        stats.classes shouldBe 2936
        stats.callSites shouldBe 89433
        stats.stringRefs shouldBe 11297
        stats.dexFiles shouldBe listOf("classes.dex")
        warnings shouldBe emptyList()
    }

    @Test
    fun `classes stream in sorted id order`() {
        classes.map { it.id } shouldBeSortedWith naturalOrder()
    }

    @Test
    fun `class record uses smali ids and dotted names`() {
        val camera = classes.single { it.id == "Lde/markusfisch/android/binaryeye/activity/CameraActivity;" }
        camera.name shouldBe "de.markusfisch.android.binaryeye.activity.CameraActivity"
        camera.`package` shouldBe "de.markusfisch.android.binaryeye.activity"
        camera.methods shouldBe 23
    }

    @Test
    fun `method, call edge and string constant are linked by the same method id`() {
        methods.map { it.id } shouldContain onResume
        calls shouldContain CallRecord(onResume, "Landroid/app/Activity;->getIntent()Landroid/content/Intent;", "virtual", 1, listOf(84))
        strings.single { it.value == "https://markusfisch.de/BinaryEye" }.method shouldBe onResume
    }
}
