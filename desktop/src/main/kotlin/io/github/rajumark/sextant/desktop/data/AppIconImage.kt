package io.github.rajumark.sextant.desktop.data

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.github.rajumark.sextant.desktop.model.IconPick
import org.jetbrains.skia.Image
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/** Reads the app icon out of its APK. Decoded icons are cached by APK and file, so each one is read once. */
object AppIconImage {
    private val cache = ConcurrentHashMap<String, ImageBitmap>()

    /** Null when the APK is missing, the file is not in the zip, or the bytes are not an image. Never throws. */
    fun load(
        apk: Path,
        pick: IconPick,
    ): ImageBitmap? {
        val key = "$apk!${pick.path}"
        cache[key]?.let { return it }
        return runCatching {
            val bytes = IconAssets.bytes(apk, pick.path) ?: return null
            Image.makeFromEncoded(bytes).toComposeImageBitmap()
        }.getOrNull()?.also { cache[key] = it }
    }
}
