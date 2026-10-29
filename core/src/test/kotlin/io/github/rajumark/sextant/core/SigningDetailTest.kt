package io.github.rajumark.sextant.core

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SigningDetailTest {
    @Test
    fun `manifest digests follow Name blocks and continuation lines`() {
        val mf =
            "Manifest-Version: 1.0\r\nCreated-By: test\r\n\r\n" +
                "Name: res/a_very_long_name_that_wraps_over_the_seventy_two_byte_line_li\r\n mit.png\r\nSHA-256-Digest: abc=\r\n\r\n" +
                "Name: classes.dex\r\nSHA1-Digest: one=\r\nSHA-256-Digest: two=\r\n"
        SigningDetails.manifestDigests(mf.toByteArray()) shouldBe
            listOf(
                Triple("res/a_very_long_name_that_wraps_over_the_seventy_two_byte_line_limit.png", "SHA-256", "abc="),
                Triple("classes.dex", "SHA1", "one="),
                Triple("classes.dex", "SHA-256", "two="),
            )
    }
}
