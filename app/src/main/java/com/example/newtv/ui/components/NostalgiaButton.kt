package com.example.newtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.example.newtv.ui.NewTvTheme
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaFocus
import com.example.newtv.ui.NostalgiaShapes

/**
 * Primary (pink channel accent) or [secondary] (raised panel) button.
 * Focus is always white with an ice-blue glow, never an accent colour.
 */
@Composable
fun NostalgiaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: Boolean = false
) {
    val shape = NostalgiaShapes.Medium
    val focusBorder = Border(BorderStroke(NostalgiaFocus.BorderWidth, NostalgiaColors.Focus), 0.dp, shape)
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = ButtonDefaults.shape(shape = shape, focusedShape = shape, pressedShape = shape),
        scale = ButtonDefaults.scale(focusedScale = NostalgiaFocus.Scale),
        glow = ButtonDefaults.glow(
            focusedGlow = Glow(NostalgiaFocus.GlowColor, NostalgiaFocus.GlowElevation)
        ),
        border = ButtonDefaults.border(
            border = if (secondary) {
                Border(BorderStroke(2.dp, NostalgiaColors.GuideLine), 0.dp, shape)
            } else {
                Border.None
            },
            focusedBorder = focusBorder,
            pressedBorder = focusBorder
        ),
        colors = if (secondary) {
            ButtonDefaults.colors(
                containerColor = NostalgiaColors.SurfaceRaised,
                contentColor = NostalgiaColors.TextPrimary,
                focusedContainerColor = NostalgiaColors.Focus,
                focusedContentColor = NostalgiaColors.OnFocus,
                pressedContainerColor = NostalgiaColors.FocusGlow,
                pressedContentColor = NostalgiaColors.OnFocus
            )
        } else {
            ButtonDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                focusedContainerColor = NostalgiaColors.Focus,
                focusedContentColor = NostalgiaColors.OnFocus,
                pressedContainerColor = NostalgiaColors.FocusGlow,
                pressedContentColor = NostalgiaColors.OnFocus
            )
        }
    ) {
        Text(text = text, style = MaterialTheme.typography.titleMedium)
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun ButtonPreview() {
    NewTvTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(48.dp)
        ) {
            NostalgiaButton(text = "Watch now", onClick = {})
            NostalgiaButton(text = "Cancel", onClick = {}, secondary = true)
        }
    }
}

