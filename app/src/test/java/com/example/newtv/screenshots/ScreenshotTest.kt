package com.example.newtv.screenshots

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.newtv.R
import com.example.newtv.data.Provider
import com.example.newtv.data.ShowEntity
import com.example.newtv.resolver.LinkChoice
import com.example.newtv.resolver.LinkKind
import com.example.newtv.ui.ChannelEditorState
import com.example.newtv.ui.LineupEditorState
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.NewTvTheme
import com.example.newtv.ui.PinDialogMode
import com.example.newtv.ui.PinDialogState
import com.example.newtv.ui.PinOverlay
import com.example.newtv.ui.RuleEditorState
import com.example.newtv.ui.Screen
import com.example.newtv.ui.ShowEditorState
import com.example.newtv.ui.components.LocalFixedClockTime
import com.example.newtv.ui.components.NostalgiaBackground
import com.example.newtv.ui.screens.ChannelEditorScreen
import com.example.newtv.ui.screens.ChannelScreen
import com.example.newtv.ui.screens.HomeScreen
import com.example.newtv.ui.screens.LineupEditorScreen
import com.example.newtv.ui.screens.RuleEditorScreen
import com.example.newtv.ui.screens.ScheduleScreen
import com.example.newtv.ui.screens.ShowEditorScreen
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Renders every screen of the cable-box UI to a PNG in src/test/screenshots.
 * Images are only written/compared when run through the Roborazzi tasks
 * (recordRoborazziDebug / verifyRoborazziDebug); plain testDebugUnitTest just checks that
 * everything composes.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w960dp-h540dp-xhdpi")
class ScreenshotTest {
    @get:Rule
    val rule = createComposeRule()

    private fun shot(name: String, content: @Composable () -> Unit) {
        rule.setContent {
            CompositionLocalProvider(LocalFixedClockTime provides Fixtures.clock) {
                NewTvTheme { NostalgiaBackground { content() } }
            }
        }
        rule.waitForIdle()
    }

    /** Assets are rendered without the CRT backdrop so they are seen as shipped. */
    private fun rawShot(content: @Composable () -> Unit) {
        rule.setContent { NewTvTheme { content() } }
        rule.waitForIdle()
    }

    private fun capture(name: String) = rule.onRoot().captureRoboImage("src/test/screenshots/$name.png")

    @Composable
    private fun home(state: MainUiState) = HomeScreen(
        uiState = state, onChannelSelected = {}, onChannelFocused = {}, onEnableKidSafe = {},
        onExitKidSafe = {}, onAddChannel = {}, onParentSettings = {}, zone = Fixtures.zone
    )

    @Composable
    private fun channel(state: MainUiState) = ChannelScreen(
        uiState = state, channelId = (state.screen as Screen.Channel).channelId, onBack = {},
        onOpenSchedule = {}, onPlayNow = { _, _ -> }, onParentSettings = {}, onEditRules = {},
        onEditChannel = {}, onEditLineup = {}, onInstallProvider = {}, onDismissMessage = {},
        onChannelUp = {}, onChannelDown = {}, zone = Fixtures.zone
    )

    @Test fun home() { shot("home") { home(Fixtures.home()) }; capture("home") }

    @Test fun homeKidSafeEmpty() {
        shot("home_kidsafe_empty") { home(Fixtures.homeKidSafeEmpty()) }; capture("home_kidsafe_empty")
    }

    @Test fun homeFocus() {
        shot("home_focus") { home(Fixtures.home()) }
        rule.onAllNodesWithText("Rugrats")[0].requestFocus()
        rule.waitForIdle()
        capture("home_focus")
    }

    @Test fun channel() { shot("channel") { channel(Fixtures.channelScreen()) }; capture("channel") }

    @Test fun channelProviderMissing() {
        shot("channel_provider_missing") { channel(Fixtures.channelScreen(providerAvailable = false)) }
        capture("channel_provider_missing")
    }

    @Test fun channelTitlePageHint() {
        shot("channel_title_page_hint") { channel(Fixtures.channelScreen(LinkKind.TITLE_PAGE)) }
        capture("channel_title_page_hint")
    }

    @Test fun schedule() {
        shot("schedule") {
            ScheduleScreen(Fixtures.scheduleScreen(), Fixtures.channels[1].id, onBack = {}, zone = Fixtures.zone)
        }
        capture("schedule")
    }

    @Test fun ruleEditor() {
        val state = Fixtures.home().copy(
            backStack = listOf(Screen.Home, Screen.Channel("nick"), Screen.RuleEditor("nick")),
            ruleEditor = RuleEditorState("nick", startMinute = 360, endMinute = 1380, noRepeatWindow = 12, slotMinutes = 30)
        )
        shot("rule_editor") { RuleEditorScreen(state, onStep = { _, _ -> }, onSave = {}, onCancel = {}) }
        capture("rule_editor")
    }

    @Test fun channelEditor() {
        val state = Fixtures.home().copy(
            backStack = listOf(Screen.Home, Screen.ChannelEditor),
            channelEditor = ChannelEditorState(
                channelId = "nick", name = "Classic Nickelodeon", ageRating = 10, kidSafeOnly = false,
                isNew = false, number = 7, showCount = 5
            )
        )
        shot("channel_editor") {
            ChannelEditorScreen(state, {}, {}, {}, {}, {}, {}, {})
        }
        capture("channel_editor")
    }

    @Test fun lineupEditor() {
        val shows = Fixtures.guide.values.flatten().map { it.show }.distinctBy { it.title }.take(9)
        val state = Fixtures.home().copy(
            backStack = listOf(Screen.Home, Screen.Channel("nick"), Screen.LineupEditor("nick")),
            lineupEditor = LineupEditorState("nick", shows, shows.take(4).map { it.id }.toSet())
        )
        shot("lineup_editor") { LineupEditorScreen(state, {}, {}, {}, {}, {}) }
        capture("lineup_editor")
    }

    @Test fun showEditor() {
        val state = Fixtures.home().copy(
            backStack = listOf(Screen.Home, Screen.ShowEditor("x")),
            showEditor = ShowEditorState(
                showId = "x", title = "Hey Arnold!", provider = Provider.NETFLIX, seasonCount = 5,
                episodeCount = 13, ageRating = 7, contentId = "70143836", linkChoice = LinkChoice.TITLE_PAGE
            )
        )
        shot("show_editor") { ShowEditorScreen(state, {}, {}, {}, {}, {}, {}, {}, {}, {}, {}) }
        capture("show_editor")
    }

    @Test fun pinPadCreate() {
        shot("pin_pad_create") {
            PinOverlay(PinDialogState(PinDialogMode.CREATE, digits = "12"), {}, {}, {}, {})
        }
        capture("pin_pad_create")
    }

    @Test fun pinPadVerifyError() {
        shot("pin_pad_verify_error") {
            PinOverlay(
                PinDialogState(PinDialogMode.VERIFY, digits = "", error = "Wrong PIN. 3 tries left."),
                {}, {}, {}, {}
            )
        }
        capture("pin_pad_verify_error")
    }

    @Test fun banner() {
        rawShot {
            Box(Modifier.fillMaxSize().background(Color(0xFF303030)), contentAlignment = Alignment.Center) {
                Image(painterResource(R.drawable.app_banner), null, Modifier.size(320.dp, 180.dp))
            }
        }
        capture("app_banner")
    }

    @Test fun launcherForeground() {
        rawShot {
            Box(Modifier.fillMaxSize().background(Color(0xFF303030)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(216.dp).background(colorResource(R.color.launcher_background))) {
                    Image(painterResource(R.drawable.launcher_foreground), null, Modifier.size(216.dp))
                }
            }
        }
        capture("launcher_foreground")
    }
}
