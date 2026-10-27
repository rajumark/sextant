package io.github.rajumark.sextant.core

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import org.junit.jupiter.api.Test

class SextantTest {
    @Test
    fun `version comes from the build, not a placeholder`() {
        Sextant.version shouldContain Regex("""^\d+\.\d+\.\d+""")
    }

    @Test
    fun `schema version is 7 since all manifest categories are structured`() {
        Sextant.SCHEMA_VERSION shouldBe 7
    }
}
