package com.example.newtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.newtv.R
import com.example.newtv.ui.NewTvTheme
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaFocus
import com.example.newtv.ui.NostalgiaShapes
import com.example.newtv.ui.osdTextStyle

/**
 * Presentational parental-lock keypad (3x4: 1-9, backspace, 0, OK). No PIN logic.
 * Key "1" requests focus on first composition. While [busy], clicks are ignored (focus is kept).
 */
@Composable
fun ParentalLockPad(
    title: String,
    message: String,
    digits: Int,
    maxDigits: Int,
    error: String?,
    busy: Boolean,
    onDigit: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Waits one frame so the key is attached before focus is requested.
    val firstKey = rememberInitialFocus()
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.width(380.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PadlockIcon()
                Text(text = title.uppercase(), style = osdTextStyle(36))
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = NostalgiaColors.TextMuted
            )
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.height(24.dp)) {
                repeat(maxDigits) { i ->
                    val filled = i < digits
                    Box(
                        Modifier
                            .size(20.dp)
                            .background(if (filled) NostalgiaColors.OsdGreen else Color.Transparent, CircleShape)
                            .border(2.dp, NostalgiaColors.OsdGreen, CircleShape)
                    )
                }
            }
            // Reserve the error line so the layout does not jump.
            Box(Modifier.height(76.dp), contentAlignment = Alignment.TopStart) {
                if (error != null) OsdBanner(text = error, tone = OsdTone.Error)
            }
            NostalgiaButton(text = stringResource(R.string.cancel), onClick = onCancel, secondary = true)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("<", "0", "OK")
            )
            rows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { key ->
                        val label = when {
                            key == "OK" && busy -> "..."
                            key == "<" -> "\u2190"
                            else -> key
                        }
                        PadKey(
                            label = label,
                            emphasized = key == "OK",
                            modifier = if (key == "1") Modifier.focusRequester(firstKey) else Modifier,
                            onClick = {
                                if (!busy) {
                                    when (key) {
                                        "<" -> onBackspace()
                                        "OK" -> onSubmit()
                                        else -> onDigit(key[0])
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PadKey(label: String, emphasized: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = NostalgiaShapes.Medium
    Surface(
        onClick = onClick,
        modifier = modifier
            .width(96.dp)
            .height(60.dp),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (emphasized) NostalgiaColors.GuideBlue else NostalgiaColors.SurfaceRaised,
            contentColor = NostalgiaColors.OsdGreen,
            focusedContainerColor = NostalgiaColors.Focus,
            focusedContentColor = NostalgiaColors.OnFocus,
            pressedContainerColor = NostalgiaColors.FocusGlow,
            pressedContentColor = NostalgiaColors.OnFocus
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = NostalgiaFocus.Scale),
        border = ClickableSurfaceDefaults.border(
            border = Border(BorderStroke(2.dp, NostalgiaColors.GuideLine), 0.dp, shape),
            focusedBorder = Border(BorderStroke(NostalgiaFocus.BorderWidth, NostalgiaColors.Focus), 0.dp, shape)
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(NostalgiaFocus.GlowColor, NostalgiaFocus.GlowElevation)
        )
    ) {
        Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.Unspecified
            )
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun ParentalLockPadPreview() {
    NewTvTheme {
        Box(Modifier.background(NostalgiaColors.Background).fillMaxWidth(), contentAlignment = Alignment.Center) {
            ParentalLockPad(
                title = "Parental lock",
                message = "Enter your PIN to continue",
                digits = 2,
                maxDigits = 4,
                error = "Wrong PIN",
                busy = false,
                onDigit = {},
                onBackspace = {},
                onSubmit = {},
                onCancel = {}
            )
        }
    }
}
