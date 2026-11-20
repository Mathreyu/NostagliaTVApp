package com.example.newtv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import com.example.newtv.R
import com.example.newtv.resolver.LinkChoice
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.Spacing
import com.example.newtv.ui.components.HelpLine
import com.example.newtv.ui.components.NostalgiaButton
import com.example.newtv.ui.components.NostalgiaWindow
import com.example.newtv.ui.components.OsdBanner
import com.example.newtv.ui.components.OsdTone
import com.example.newtv.ui.components.ScreenScaffold
import com.example.newtv.ui.components.StepperRow
import com.example.newtv.ui.components.TvTextField
import com.example.newtv.ui.components.rememberInitialFocus

/** Cable-box style setup menu for creating, editing or deleting one show of the library. */
@Composable
fun ShowEditorScreen(
    uiState: MainUiState,
    onTitleChange: (String) -> Unit,
    onContentIdChange: (String) -> Unit,
    onStepProvider: (Int) -> Unit,
    onStepSeasons: (Int) -> Unit,
    onStepEpisodes: (Int) -> Unit,
    onStepAgeRating: (Int) -> Unit,
    onStepLink: (Int) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    val editor = uiState.showEditor ?: return
    val firstFocus = rememberInitialFocus()
    ScreenScaffold(kidSafe = uiState.kidSafeEnabled, spacing = Spacing.S) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            NostalgiaWindow(
                title = stringResource(
                    if (editor.isNew) R.string.show_editor_add_title else R.string.show_editor_edit_title
                ),
                modifier = Modifier.width(MenuWidth).verticalScroll(rememberScrollState()),
                contentSpacing = Spacing.S
            ) {
                TvTextField(
                    label = stringResource(R.string.show_title),
                    value = editor.title,
                    onValueChange = onTitleChange,
                    placeholder = stringResource(R.string.show_title_placeholder),
                    modifier = Modifier.focusRequester(firstFocus)
                )
                TvTextField(
                    label = stringResource(R.string.show_content_id),
                    value = editor.contentId,
                    onValueChange = onContentIdChange,
                    placeholder = stringResource(R.string.show_content_id_placeholder)
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.S)) {
                    StepperRow(
                        label = stringResource(R.string.show_provider),
                        valueText = editor.provider.displayName,
                        onDecrement = { onStepProvider(-1) },
                        onIncrement = { onStepProvider(+1) }
                    )
                    StepperRow(
                        label = stringResource(R.string.show_seasons),
                        valueText = editor.seasonCount.toString(),
                        onDecrement = { onStepSeasons(-1) },
                        onIncrement = { onStepSeasons(+1) }
                    )
                    StepperRow(
                        label = stringResource(R.string.show_episodes_per_season),
                        valueText = editor.episodeCount.toString(),
                        onDecrement = { onStepEpisodes(-1) },
                        onIncrement = { onStepEpisodes(+1) }
                    )
                    StepperRow(
                        label = stringResource(R.string.age_rating),
                        valueText = stringResource(R.string.age_rating_value, editor.ageRating),
                        onDecrement = { onStepAgeRating(-1) },
                        onIncrement = { onStepAgeRating(+1) }
                    )
                    StepperRow(
                        label = stringResource(R.string.show_link),
                        valueText = stringResource(
                            when (editor.linkChoice) {
                                LinkChoice.TITLE_PAGE -> R.string.link_choice_title_page
                                LinkChoice.EPISODE_LINK -> R.string.link_choice_episode
                                LinkChoice.CUSTOM -> R.string.link_choice_custom
                            }
                        ),
                        onDecrement = { onStepLink(-1) },
                        onIncrement = { onStepLink(+1) }
                    )
                }
                when {
                    editor.error != null -> OsdBanner(text = editor.error, tone = OsdTone.Error)
                    editor.confirmingDelete ->
                        OsdBanner(text = stringResource(R.string.delete_show_warning), tone = OsdTone.Error)
                    else -> HelpLine(stringResource(R.string.show_editor_help))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.M)) {
                    NostalgiaButton(text = stringResource(R.string.save), onClick = onSave)
                    NostalgiaButton(text = stringResource(R.string.cancel), onClick = onCancel, secondary = true)
                    if (!editor.isNew) {
                        NostalgiaButton(
                            text = stringResource(
                                if (editor.confirmingDelete) R.string.delete_show_confirm else R.string.delete_show
                            ),
                            onClick = onDelete,
                            secondary = true
                        )
                    }
                }
            }
        }
    }
}
