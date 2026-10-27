package io.github.rajumark.sextant.core

/**
 * Android's rules for whether a component can be started by other apps.
 *
 * - An explicit `android:exported` always wins.
 * - Activities, aliases, services and receivers default to exported when they declare an intent filter.
 * - Providers default to exported only for apps whose minSdk or targetSdk is 16 or lower.
 */
object ExportedRules {
    data class Result(
        val exported: Boolean,
        val source: String,
    )

    const val EXPLICIT = "explicit"
    const val DEFAULT = "default"

    fun resolve(
        type: String,
        explicit: Boolean?,
        hasIntentFilters: Boolean,
        minSdk: Int?,
        targetSdk: Int?,
    ): Result {
        if (explicit != null) return Result(explicit, EXPLICIT)
        val exported =
            when (type) {
                "provider" -> {
                    // targetSdk defaults to minSdk, which defaults to 1.
                    val effectiveMin = minSdk ?: 1
                    val effectiveTarget = targetSdk ?: effectiveMin
                    effectiveMin <= 16 || effectiveTarget <= 16
                }
                else -> hasIntentFilters
            }
        return Result(exported, DEFAULT)
    }
}
