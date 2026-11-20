package com.example.newtv.ui.components

import android.graphics.Bitmap
import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.tv.material3.Text
import com.example.newtv.ui.LocalReducedMotion
import com.example.newtv.ui.NewTvTheme
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.osdTextStyle
import kotlin.random.Random

private const val NOISE_W = 160
private const val NOISE_H = 90
const val STATIC_DURATION_MS = 350

/** One small grayscale noise texture, generated once and re-sampled at random offsets per frame. */
private fun createNoise(): ImageBitmap {
    val rnd = Random(1997)
    val px = IntArray(NOISE_W * NOISE_H) {
        val v = rnd.nextInt(30, 256)
        (0xFF shl 24) or (v shl 16) or (v shl 8) or v
    }
    return Bitmap.createBitmap(px, NOISE_W, NOISE_H, Bitmap.Config.ARGB_8888).asImageBitmap()
}

@Composable
private fun rememberReducedMotion(): Boolean {
    if (LocalReducedMotion.current) return true
    val ctx = LocalContext.current
    return remember(ctx) {
        try {
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (_: Throwable) {
            false
        }
    }
}

/** Draws one frame of TV snow plus a rolling horizontal bar. progress runs 0..1. */
private fun Modifier.staticFrame(noise: ImageBitmap, progress: () -> Float): Modifier = drawBehind {
    val p = progress()
    if (p <= 0f || p >= 1f) return@drawBehind
    // Fade in fast, hold, fade out.
    val alpha = when {
        p < 0.15f -> p / 0.15f
        p > 0.6f -> (1f - p) / 0.4f
        else -> 1f
    }.coerceIn(0f, 1f)
    val frame = (p * 24f).toInt()
    val rnd = Random(frame * 7919)
    val srcW = NOISE_W - 32
    val srcH = NOISE_H - 18
    drawImage(
        image = noise,
        srcOffset = IntOffset(rnd.nextInt(0, NOISE_W - srcW + 1), rnd.nextInt(0, NOISE_H - srcH + 1)),
        srcSize = IntSize(srcW, srcH),
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
        alpha = alpha * 0.55f,
        filterQuality = FilterQuality.None
    )
    // Horizontal roll: a dark band and a bright band sweeping down the screen.
    val bandH = size.height * 0.18f
    val y = (p * 1.3f - 0.15f) * size.height
    drawRect(Color.Black.copy(alpha = 0.55f * alpha), Offset(0f, y), androidx.compose.ui.geometry.Size(size.width, bandH))
    drawRect(Color.White.copy(alpha = 0.18f * alpha), Offset(0f, y + bandH), androidx.compose.ui.geometry.Size(size.width, bandH * 0.12f))
}

/**
 * Plays a ~350 ms static burst whenever [trigger] changes (not on first composition).
 * Place it in a Box on top of the content; it never takes focus or input.
 */
@Composable
fun ChannelChangeOverlay(trigger: Any, modifier: Modifier = Modifier) {
    val noise = remember { createNoise() }
    val progress = remember { Animatable(0f) }
    val reduced = rememberReducedMotion()
    var first by remember { mutableStateOf(true) }
    LaunchedEffect(trigger) {
        if (first) {
            first = false
            return@LaunchedEffect
        }
        if (reduced) return@LaunchedEffect
        progress.snapTo(0.001f)
        progress.animateTo(1f, tween(STATIC_DURATION_MS, easing = LinearEasing))
        progress.snapTo(0f)
    }
    Box(modifier.fillMaxSize().staticFrame(noise) { progress.value })
}

/** Visibility-driven variant: when [visible] becomes true, plays once then calls [onFinished]. */
@Composable
fun StaticTransition(visible: Boolean, onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val noise = remember { createNoise() }
    val progress = remember { Animatable(0f) }
    val reduced = rememberReducedMotion()
    val finished by rememberUpdatedState(onFinished)
    LaunchedEffect(visible) {
        if (!visible) {
            progress.snapTo(0f)
            return@LaunchedEffect
        }
        if (!reduced) {
            progress.snapTo(0.001f)
            progress.animateTo(1f, tween(STATIC_DURATION_MS, easing = LinearEasing))
            progress.snapTo(0f)
        }
        finished()
    }
    Box(modifier.fillMaxSize().staticFrame(noise) { progress.value })
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun StaticPreview() {
    NewTvTheme {
        Box(Modifier.fillMaxSize().background(NostalgiaColors.GuideBlue)) {
            Text("CH 07", style = osdTextStyle(48))
            ChannelChangeOverlay(trigger = 7)
        }
    }
}
