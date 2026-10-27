package io.github.rajumark.sextant.core

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/**
 * `Double.toString` / `Float.toString` as JDK 19+ prints them (the shortest decimal that reads back as the same
 * value), on every JDK. JDK 17's algorithm sometimes prints extra digits (`2.0E-3` vs `0.002`, `1.0E23` vs
 * `9.999999999999999E22`), which made output depend on the Java version.
 */
object Decimals {
    fun format(value: Double): String = special(value) ?: render(shortest(BigDecimal(value)) { it.toDouble() == value })

    fun format(value: Float): String = special(value.toDouble()) ?: render(shortest(BigDecimal(value.toDouble())) { it.toFloat() == value })

    private fun special(d: Double) =
        when {
            d.isNaN() -> "NaN"
            d == Double.POSITIVE_INFINITY -> "Infinity"
            d == Double.NEGATIVE_INFINITY -> "-Infinity"
            d == 0.0 -> if (1.0 / d < 0) "-0.0" else "0.0"
            else -> null
        }

    /**
     * The closest decimal with the fewest significant digits (at least 2, as the JDK does) that reads back as the
     * same value.
     */
    private fun shortest(
        exact: BigDecimal,
        readsBack: (BigDecimal) -> Boolean,
    ): BigDecimal {
        for (digits in 2..17) {
            val candidate = exact.round(MathContext(digits, RoundingMode.HALF_EVEN))
            if (readsBack(candidate)) return candidate.stripTrailingZeros()
        }
        return exact.round(MathContext(17, RoundingMode.HALF_EVEN)).stripTrailingZeros()
    }

    /** Java's layout: plain for 10^-3 ≤ |x| < 10^7, otherwise `d.dddE±n`; always at least one fractional digit. */
    private fun render(d: BigDecimal): String {
        val negative = d.signum() < 0
        val unscaled = d.unscaledValue().abs().toString()
        // value = 0.<digits> × 10^exp10, i.e. the decimal exponent of the first digit is exp10 - 1.
        val exponent = unscaled.length - 1 - d.scale()
        val body =
            if (exponent >= -3 && exponent < 7) {
                val plain = d.abs().toPlainString()
                if ('.' in plain) plain else "$plain.0"
            } else {
                val fraction = unscaled.substring(1).ifEmpty { "0" }
                "${unscaled[0]}.${fraction}E$exponent"
            }
        return if (negative) "-$body" else body
    }
}
