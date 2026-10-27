package io.github.rajumark.sextant.core

import io.kotest.matchers.shouldBe
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class ExportedRulesTest {
    @ParameterizedTest(name = "{0} explicit={1} filters={2} min={3} target={4} -> {5} ({6})")
    @CsvSource(
        nullValues = ["null"],
        value = [
            // explicit always wins
            "activity, true, false, 21, 34, true, explicit",
            "activity, false, true, 21, 34, false, explicit",
            "provider, true, false, 21, 34, true, explicit",
            "provider, false, false, 10, 10, false, explicit",
            // intent filters decide for activities, aliases, services, receivers
            "activity, null, true, 21, 34, true, default",
            "activity, null, false, 21, 34, false, default",
            "activity-alias, null, true, 21, 34, true, default",
            "service, null, false, 21, 34, false, default",
            "receiver, null, true, 21, 34, true, default",
            // providers: exported by default only for old apps
            "provider, null, false, 21, 34, false, default",
            "provider, null, true, 21, 34, false, default",
            "provider, null, false, 16, 34, true, default",
            "provider, null, false, 21, 16, true, default",
            "provider, null, false, 21, 17, false, default",
            "provider, null, false, null, null, true, default",
            "provider, null, false, 21, null, false, default",
        ],
    )
    fun `follows Android's export rules`(
        type: String,
        explicit: Boolean?,
        hasFilters: Boolean,
        minSdk: Int?,
        targetSdk: Int?,
        expected: Boolean,
        source: String,
    ) {
        ExportedRules.resolve(type, explicit, hasFilters, minSdk, targetSdk) shouldBe ExportedRules.Result(expected, source)
    }
}
