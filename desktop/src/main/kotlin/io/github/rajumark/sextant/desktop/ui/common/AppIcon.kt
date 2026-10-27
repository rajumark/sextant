package io.github.rajumark.sextant.desktop.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.rajumark.sextant.desktop.data.AppIconImage
import io.github.rajumark.sextant.desktop.model.Analysis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** The app's own icon from its APK, or the letter avatar when no icon can be drawn. */
@Composable
fun AppIcon(
    analysis: Analysis,
    size: Dp = 48.dp,
) {
    val m = analysis.meta
    val apk =
        analysis.apkPath?.let {
            java.nio.file.Path
                .of(it)
        }
    val pick = m.icon
    val bitmap by produceState<ImageBitmap?>(null, apk, pick) {
        value = if (apk != null && pick != null) withContext(Dispatchers.IO) { AppIconImage.load(apk, pick) } else null
    }
    val shape = MaterialTheme.shapes.large
    val loaded = bitmap
    if (loaded != null) {
        Image(
            bitmap = loaded,
            contentDescription = "${m.label} icon",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(size).clip(shape),
        )
    } else {
        AppAvatar(m.label, m.packageName, size)
    }
}
