package com.example.newtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.tv.material3.Text as TvText
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaWindow

@Composable
fun RuleEditorScreen(
    uiState: MainUiState,
    onStartMinuteChange: (String) -> Unit,
    onEndMinuteChange: (String) -> Unit,
    onNoRepeatChange: (String) -> Unit,
    onSlotMinutesChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val editor = uiState.ruleEditor ?: return
    NostalgiaWindow(
        title = "Edit schedule rules",
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        OutlinedTextField(
            value = editor.startMinute,
            onValueChange = onStartMinuteChange,
            label = { Text(text = "Start minute (0-1440)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = editor.endMinute,
            onValueChange = onEndMinuteChange,
            label = { Text(text = "End minute (0-1440)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = editor.noRepeatWindow,
            onValueChange = onNoRepeatChange,
            label = { Text(text = "No-repeat window") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = editor.slotMinutes,
            onValueChange = onSlotMinutesChange,
            label = { Text(text = "Slot minutes") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        if (editor.error != null) {
            TvText(text = editor.error)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            NostalgiaButton(text = "Save", onClick = onSave)
            NostalgiaButton(text = "Cancel", onClick = onCancel)
        }
    }
}
