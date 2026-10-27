package io.github.rajumark.sextant.desktop.data

import androidx.compose.ui.graphics.Color
import java.security.MessageDigest
import java.util.prefs.Preferences

/** User choices that survive a restart. */
interface AppSettings {
    /** The seed color as `RRGGBB`, or null when the default is in use. */
    fun seedHex(): String?

    fun saveSeedHex(hex: String?)

    /** Light, dark or system, as the user last chose. Null means never chosen: follow the system. */
    fun themeMode(): io.github.rajumark.sextant.desktop.theme.ThemeMode?

    fun saveThemeMode(mode: io.github.rajumark.sextant.desktop.theme.ThemeMode)

    /** Tags the user added to one analysis, keyed by its output folder. */
    fun tags(dir: String): List<String>

    fun saveTags(
        dir: String,
        tags: List<String>,
    )
}

class PreferencesSettings(
    private val prefs: Preferences = Preferences.userRoot().node("io/github/rajumark/sextant/desktop"),
) : AppSettings {
    override fun seedHex(): String? = prefs.get(SEED, null)

    override fun saveSeedHex(hex: String?) {
        if (hex == null) prefs.remove(SEED) else prefs.put(SEED, hex)
        runCatching { prefs.flush() }
    }

    override fun themeMode(): io.github.rajumark.sextant.desktop.theme.ThemeMode? =
        prefs.get(THEME, null)?.let { name ->
            io.github.rajumark.sextant.desktop.theme.ThemeMode.entries
                .firstOrNull { it.name == name }
        }

    override fun saveThemeMode(mode: io.github.rajumark.sextant.desktop.theme.ThemeMode) {
        prefs.put(THEME, mode.name)
        runCatching { prefs.flush() }
    }

    override fun tags(dir: String): List<String> = prefs.get(tagKey(dir), "").split(TAG_SEPARATOR).filter { it.isNotEmpty() }

    override fun saveTags(
        dir: String,
        tags: List<String>,
    ) {
        if (tags.isEmpty()) prefs.remove(tagKey(dir)) else prefs.put(tagKey(dir), tags.joinToString(TAG_SEPARATOR))
        runCatching { prefs.flush() }
    }

    // Preference keys are short and must not contain '/', so the folder path is hashed.
    private fun tagKey(dir: String): String =
        "tags." +
            MessageDigest
                .getInstance("SHA-256")
                .digest(dir.toByteArray())
                .joinToString("") { "%02x".format(it) }
                .take(32)

    private companion object {
        const val SEED = "seed"
        const val THEME = "theme"
        const val TAG_SEPARATOR = "\u001F"
    }
}

/** In-memory settings for tests, so they never touch the user's preferences. */
class MemorySettings : AppSettings {
    private var seed: String? = null
    private var theme: io.github.rajumark.sextant.desktop.theme.ThemeMode? = null

    override fun themeMode() = theme

    override fun saveThemeMode(mode: io.github.rajumark.sextant.desktop.theme.ThemeMode) {
        theme = mode
    }

    private val tags = mutableMapOf<String, List<String>>()

    override fun seedHex(): String? = seed

    override fun saveSeedHex(hex: String?) {
        seed = hex
    }

    override fun tags(dir: String): List<String> = tags[dir].orEmpty()

    override fun saveTags(
        dir: String,
        tags: List<String>,
    ) {
        if (tags.isEmpty()) this.tags.remove(dir) else this.tags[dir] = tags
    }
}

/** Parses `RRGGBB` or `#RRGGBB`. Returns null for anything else. */
fun parseHexColor(text: String): Color? {
    val hex = text.trim().removePrefix("#")
    if (!Regex("[0-9a-fA-F]{6}").matches(hex)) return null
    return Color(hex.toLong(16) or 0xFF000000L)
}

/** `RRGGBB` for a color, without alpha. */
fun Color.toHex(): String = "%06X".format(toArgb() and 0xFFFFFF)

private fun Color.toArgb(): Int =
    (alpha * 255).toInt() shl 24 or ((red * 255).toInt() shl 16) or ((green * 255).toInt() shl 8) or (blue * 255).toInt()
