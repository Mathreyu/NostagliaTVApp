package com.example.newtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.Text as TvText
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaWindow

@Composable
fun ChannelEditorScreen(
    uiState: MainUiState,
    onNameChange: (String) -> Unit,
    onAgeRatingChange: (String) -> Unit,
    onToggleKidSafe: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val editor = uiState.channelEditor ?: return
    NostalgiaWindow(
        title = if (editor.isNew) "Add channel" else "Edit channel",
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        OutlinedTextField(
            value = editor.name,
            onValueChange = onNameChange,
            label = { Text(text = "Channel name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = editor.ageRating,
            onValueChange = onAgeRatingChange,
            label = { Text(text = "Age rating (0-21)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NostalgiaButton(
                text = if (editor.kidSafeOnly) "Kid-safe only: ON" else "Kid-safe only: OFF",
                onClick = { onToggleKidSafe(!editor.kidSafeOnly) }
            )
        }
        if (editor.error != null) {
            TvText(text = editor.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NostalgiaButton(text = "Save", onClick = onSave)
            NostalgiaButton(text = "Cancel", onClick = onCancel)
        }
    }
}
