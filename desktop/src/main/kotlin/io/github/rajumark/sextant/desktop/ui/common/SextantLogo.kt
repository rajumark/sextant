package io.github.rajumark.sextant.desktop.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.Dp
import org.jetbrains.skia.Image as SkiaImage

/** The Sextant app icon (Ocean), bundled as resources in a light and a dark variant. */
object SextantIcon {
    private fun load(name: String): ImageBitmap {
        val bytes =
            SextantIcon::class.java.getResourceAsStream("/icons/$name.png")?.use { it.readBytes() }
                ?: error("missing resource /icons/$name.png")
        return SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
    }

    val light: ImageBitmap by lazy { load("sextant-light") }
    val dark: ImageBitmap by lazy { load("sextant-dark") }

    /** Window and Dock/taskbar icon: the light variant, as on the installers. */
    val windowPainter: BitmapPainter by lazy { BitmapPainter(light) }

    /** The same icon as an AWT image, for the macOS Dock when running from Gradle (installers use the .icns). */
    fun awtImage(): java.awt.Image? =
        SextantIcon::class.java.getResource("/icons/sextant-light.png")?.let { javax.imageio.ImageIO.read(it) }
}

/** The app icon, picking the variant that matches the current theme. */
@Composable
fun SextantLogo(size: Dp) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val bitmap = if (dark) SextantIcon.dark else SextantIcon.light
    Image(remember(bitmap) { BitmapPainter(bitmap) }, contentDescription = "Sextant", modifier = Modifier.size(size))
}
