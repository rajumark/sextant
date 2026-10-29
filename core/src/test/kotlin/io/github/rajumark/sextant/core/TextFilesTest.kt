package io.github.rajumark.sextant.core

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TextFilesTest {
    @Test
    fun `properties keep file order, escapes and continuation lines`() {
        TextFiles.properties("# comment\nversion=1.2.3\nname = Lib\\u0020X\nlong=a \\\n  b\n") shouldBe
            listOf("version" to "1.2.3", "name" to "Lib X", "long" to "a b")
    }

    @Test
    fun `service files list implementations, without comments or duplicates`() {
        TextFiles.services("META-INF/services/com.a.Plugin", "# header\ncom.a.One\n\ncom.a.Two # note\ncom.a.One\n") shouldBe
            listOf("com.a.One", "com.a.Two")
        TextFiles.services("META-INF/other.txt", "com.a.One") shouldBe emptyList()
    }
}
