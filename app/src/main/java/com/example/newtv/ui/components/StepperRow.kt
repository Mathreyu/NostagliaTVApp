package com.example.newtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.example.newtv.ui.NewTvTheme
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaFocus
import com.example.newtv.ui.NostalgiaShapes
import com.example.newtv.ui.osdTextStyle

/**
 * D-pad friendly value picker: label, a focusable "-" key, the current value and a focusable "+" key.
 * Focus lands on the first key when the row is targeted by a FocusRequester in [modifier].
 */
@Composable
fun StepperRow(
    label: String,
    valueText: String,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .focusGroup(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = NostalgiaColors.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        StepKey(label = "−", onClick = onDecrement)
        Box(
            modifier = Modifier
                .width(260.dp)
                .height(52.dp)
                .background(NostalgiaColors.GuideBlueDeep, NostalgiaShapes.Small),
            contentAlignment = Alignment.Center
        ) {
            Text(text = valueText, style = osdTextStyle(30), maxLines = 1)
        }
        StepKey(label = "+", onClick = onIncrement)
    }
}

@Composable
private fun StepKey(label: String, onClick: () -> Unit) {
    val shape = NostalgiaShapes.Medium
    Surface(
        onClick = onClick,
        modifier = Modifier
            .width(64.dp)
            .height(52.dp),
        shape = ClickableSurfaceDefaults.shape(shape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = NostalgiaColors.SurfaceRaised,
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
        Box(Modifier.fillMaxWidth().height(52.dp), contentAlignment = Alignment.Center) {
            Text(text = label, style = MaterialTheme.typography.headlineMedium, color = Color.Unspecified)
        }
    }
}

@Preview(widthDp = 960, heightDp = 540)
@Composable
private fun StepperRowPreview() {
    NewTvTheme {
        Column(
            Modifier.background(NostalgiaColors.Background).padding(48.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StepperRow("Start time", "6:00 AM", {}, {})
            StepperRow("Slot length", "30 min", {}, {})
        }
    }
}
