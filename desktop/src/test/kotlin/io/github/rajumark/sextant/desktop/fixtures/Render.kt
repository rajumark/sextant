package io.github.rajumark.sextant.desktop.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.jetbrains.skia.EncodedImageFormat

/**
 * Renders [content] off-screen and returns PNG bytes. Effects run on this thread's event loop (like the real
 * app's single UI thread), between frames, so layout is never touched from two threads.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun renderPng(
    width: Int = 1360,
    height: Int = 880,
    frames: Int = 60,
    frameNanos: Long = 33_000_000L,
    content: @Composable () -> Unit,
): ByteArray =
    runBlocking {
        val scene = ImageComposeScene(width, height, Density(1f), coroutineContext = coroutineContext, content = content)
        try {
            var image = scene.render(0)
            for (frame in 1..frames) {
                yield()
                image = scene.render(frame * frameNanos)
            }
            image.encodeToData(EncodedImageFormat.PNG)!!.bytes
        } finally {
            scene.close()
        }
    }
