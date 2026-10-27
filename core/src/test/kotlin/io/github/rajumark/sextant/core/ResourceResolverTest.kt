package io.github.rajumark.sextant.core

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ResourceResolverTest {
    private val resolver =
        ResourceResolver(
            mapOf(
                "@string/app_name" to "@string/app_name_prod",
                "@string/app_name_prod" to "Wikipedia",
                "@string/loop_a" to "@string/loop_b",
                "@string/loop_b" to "@string/loop_a",
            ),
        )

    @Test
    fun `plain values and unknown references pass through`() {
        resolver.resolve("Hello") shouldBe "Hello"
        resolver.resolve("@string/missing") shouldBe "@string/missing"
        resolver.resolve(null) shouldBe null
    }

    @Test
    fun `reference chains are followed`() {
        resolver.resolve("@string/app_name") shouldBe "Wikipedia"
    }

    @Test
    fun `reference cycles stop instead of looping forever`() {
        resolver.resolve("@string/loop_a")!!.startsWith("@string/loop_") shouldBe true
    }
}
