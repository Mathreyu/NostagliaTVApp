package com.example.newtv.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.compose.ui.focus.FocusRequester
import com.example.newtv.ui.NostalgiaColors
import com.example.newtv.ui.NostalgiaFocus
import com.example.newtv.ui.NostalgiaFonts
import com.example.newtv.ui.NostalgiaShapes

/**
 * Single-line text input for TV. It is a focusable row (D-pad friendly, never pops the keyboard
 * by merely passing over it); selecting it switches to a [BasicTextField] with the IME. Done
 * (or leaving the field) returns to the row.
 */
@Composable
fun TvTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = ""
) {
    var editing by remember { mutableStateOf(false) }
    var returnFocus by remember { mutableStateOf(false) }
    var seenFocus by remember { mutableStateOf(false) }
    val rowFocus = remember { FocusRequester() }
    val editFocus = remember { FocusRequester() }
    val shape = NostalgiaShapes.Medium

    LaunchedEffect(editing) {
        withFrameNanos { }
        if (editing) {
            seenFocus = false
            editFocus.tryRequestFocus()
        } else if (returnFocus) {
            returnFocus = false
            rowFocus.tryRequestFocus()
        }
    }

    if (!editing) {
        Surface(
            onClick = { editing = true },
            modifier = modifier
                .fillMaxWidth()
                .focusRequester(rowFocus),
            shape = ClickableSurfaceDefaults.shape(shape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = NostalgiaColors.GuideBlueDeep,
                contentColor = NostalgiaColors.TextPrimary,
                focusedContainerColor = NostalgiaColors.SurfaceRaised,
                focusedContentColor = NostalgiaColors.TextPrimary
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.02f),
            border = ClickableSurfaceDefaults.border(
                border = Border(BorderStroke(2.dp, NostalgiaColors.GuideLine), 0.dp, shape),
                focusedBorder = Border(BorderStroke(NostalgiaFocus.BorderWidth, NostalgiaColors.Focus), 0.dp, shape)
            ),
            glow = ClickableSurfaceDefaults.glow(
                focusedGlow = Glow(NostalgiaFocus.GlowColor, NostalgiaFocus.GlowElevation)
            )
        ) {
            FieldBody(label = label) {
                Text(
                    text = value.ifEmpty { placeholder },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (value.isEmpty()) NostalgiaColors.TextMuted else NostalgiaColors.TextPrimary,
                    maxLines = 1
                )
            }
        }
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .background(NostalgiaColors.GuideBlueDeep, shape)
                .border(NostalgiaFocus.BorderWidth, NostalgiaColors.Focus, shape)
        ) {
            FieldBody(label = label) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = NostalgiaColors.TextPrimary,
                        fontFamily = NostalgiaFonts.Body,
                        fontSize = 21.sp
                    ),
                    cursorBrush = SolidColor(NostalgiaColors.OsdGreen),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        returnFocus = true
                        editing = false
                    }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(editFocus)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                seenFocus = true
                            } else if (seenFocus) {
                                seenFocus = false
                                editing = false
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun FieldBody(label: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = NostalgiaColors.TextMuted
        )
        content()
    }
}
