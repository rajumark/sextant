package io.github.rajumark.sextant.core

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import kotlin.random.Random

class DecimalsTest {
    private val jdk19OrLater = Runtime.version().feature() >= 19

    @Test
    fun `known values print like JDK 19+`() {
        Decimals.format(0.002) shouldBe "0.002"
        Decimals.format(1.0E23) shouldBe "1.0E23"
        Decimals.format(2.0E-3) shouldBe "0.002"
        Decimals.format(1.0) shouldBe "1.0"
        Decimals.format(-0.0) shouldBe "-0.0"
        Decimals.format(123456.789) shouldBe "123456.789"
        Decimals.format(1.0E7) shouldBe "1.0E7"
        Decimals.format(9999999.0) shouldBe "9999999.0"
        Decimals.format(0.001) shouldBe "0.001"
        Decimals.format(9.99E-4) shouldBe "9.99E-4"
        Decimals.format(Double.MAX_VALUE) shouldBe "1.7976931348623157E308"
        Decimals.format(Double.MIN_VALUE) shouldBe "4.9E-324"
        Decimals.format(Double.NaN) shouldBe "NaN"
        Decimals.format(0.1f) shouldBe "0.1"
        Decimals.format(3.4028235E38f) shouldBe "3.4028235E38"
        Decimals.format(1.4E-45f) shouldBe "1.4E-45"
        Decimals.format(-2.5f) shouldBe "-2.5"
    }

    @Test
    fun `matches the JDK's own toString on JDK 19+`() {
        if (!jdk19OrLater) return
        val random = Random(42)
        repeat(200_000) {
            val d = Double.fromBits(random.nextLong())
            Decimals.format(d) shouldBe d.toString()
            val f = Float.fromBits(random.nextInt())
            Decimals.format(f) shouldBe f.toString()
        }
        repeat(50_000) {
            val d = random.nextDouble() * Math.pow(10.0, random.nextInt(-12, 12).toDouble())
            Decimals.format(d) shouldBe d.toString()
            val f = d.toFloat()
            Decimals.format(f) shouldBe f.toString()
        }
    }
}
