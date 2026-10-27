package io.github.rajumark.sextant.core.output

import io.kotest.matchers.shouldBe
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test

class SurrogateTest {
    @Test
    fun `plain text and valid surrogate pairs are untouched`() {
        "hello".escapeLoneSurrogates() shouldBe "hello"
        "emoji 😀 ok".escapeLoneSurrogates() shouldBe "emoji 😀 ok"
    }

    @Test
    fun `lone surrogates become JSON escapes that decode back to the same string`() {
        val raw = "a\uD800b\uDC00"
        val json = SextantJson.compact.encodeToString(String.serializer(), raw).escapeLoneSurrogates()
        json shouldBe "\"a\\ud800b\\udc00\""
        SextantJson.reader.decodeFromString(String.serializer(), json) shouldBe raw
    }
}
