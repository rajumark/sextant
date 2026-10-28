package io.github.rajumark.sextant.core

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class RawXmlTest {
    @Test
    fun `references are recognized and normalized, plain values are not`() {
        RawXml.ref("@string/title") shouldBe "@string/title"
        RawXml.ref("@+id/toolbar") shouldBe "@id/toolbar"
        RawXml.ref("?attr/colorPrimary") shouldBe "?attr/colorPrimary"
        RawXml.ref("@android:color/white") shouldBe "@android:color/white"
        RawXml.ref("16.0dip") shouldBe null
        RawXml.ref("#ff000000") shouldBe null
        RawXml.ref("match_parent") shouldBe null
        RawXml.ref("@null") shouldBe null
    }
}
