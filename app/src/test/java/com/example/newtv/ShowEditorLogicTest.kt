package com.example.newtv

import com.example.newtv.data.HistoryRetention
import com.example.newtv.data.Provider
import com.example.newtv.data.ShowEntity
import com.example.newtv.resolver.LinkChoice
import com.example.newtv.ui.LineupEditorState
import com.example.newtv.ui.LineupError
import com.example.newtv.ui.ShowEditorState
import com.example.newtv.ui.ShowError
import com.example.newtv.ui.slugify
import com.example.newtv.ui.steppedAge
import com.example.newtv.ui.steppedEpisodes
import com.example.newtv.ui.steppedLink
import com.example.newtv.ui.steppedProvider
import com.example.newtv.ui.steppedSeasons
import com.example.newtv.ui.toEntity
import com.example.newtv.ui.toggled
import com.example.newtv.ui.uniqueId
import com.example.newtv.ui.validate
import com.example.newtv.ui.withContentId
import com.example.newtv.ui.withTitle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration

class ShowEditorLogicTest {
    private fun show(id: String, rating: Int = 7) =
        ShowEntity(id, id, Provider.NETFLIX, Provider.NETFLIX.titleUrlTemplate, "c", 1, 1, rating)

    private fun valid() = ShowEditorState(null).withTitle("Rugrats").withContentId("42")

    @Test
    fun lineupToggleAndValidation() {
        val state = LineupEditorState("c", listOf(show("a"), show("b")), setOf("a"), error = "x")
        assertNull(state.validate())
        val off = state.toggled("a")
        assertEquals(emptySet<String>(), off.selected)
        assertNull(off.error)
        assertEquals(LineupError.EMPTY, off.validate())
        assertEquals(setOf("b"), off.toggled("b").selected)
        // A selection of deleted/unknown shows does not count.
        assertEquals(LineupError.EMPTY, state.copy(selected = setOf("gone")).validate())
    }

    @Test
    fun showValidation() {
        assertEquals(ShowError.TITLE_REQUIRED, ShowEditorState(null).validate())
        assertEquals(ShowError.CONTENT_ID_REQUIRED, ShowEditorState(null).withTitle("x").validate())
        assertNull(valid().validate())
        assertEquals(ShowError.SEASONS_RANGE, valid().copy(seasonCount = 31).validate())
        assertEquals(ShowError.EPISODES_RANGE, valid().copy(episodeCount = 0).validate())
        assertEquals(ShowError.AGE_RANGE, valid().copy(ageRating = 22).validate())
        assertEquals(ShowError.TITLE_REQUIRED, valid().withTitle("   ").validate())
    }

    @Test
    fun steppersWrapWithinRanges() {
        assertEquals(1, valid().copy(seasonCount = 30).steppedSeasons(+1).seasonCount)
        assertEquals(30, valid().copy(seasonCount = 1).steppedSeasons(-1).seasonCount)
        assertEquals(1, valid().copy(episodeCount = 60).steppedEpisodes(+1).episodeCount)
        assertEquals(60, valid().copy(episodeCount = 1).steppedEpisodes(-1).episodeCount)
        assertEquals(0, valid().copy(ageRating = 21).steppedAge(+1).ageRating)
        assertEquals(21, valid().copy(ageRating = 0).steppedAge(-1).ageRating)
    }

    @Test
    fun providerCyclesAndLinkFallsBack() {
        val all = Provider.entries
        val next = valid().steppedProvider(+1)
        assertEquals(all[1 % all.size], next.provider)
        assertEquals(all.first(), valid().copy(provider = all.last()).steppedProvider(+1).provider)
        assertEquals(all.last(), valid().copy(provider = all.first()).steppedProvider(-1).provider)
        // Only the title page is offered while no provider has an episode template.
        assertEquals(LinkChoice.TITLE_PAGE, valid().steppedLink(+1).linkChoice)
    }

    @Test
    fun editingClearsErrorAndDeleteConfirmation() {
        val e = valid().copy(error = "bad", confirmingDelete = true).withTitle("New")
        assertNull(e.error)
        assertFalse(e.confirmingDelete)
    }

    @Test
    fun toEntityTrimsAndUsesProviderTemplates() {
        val entity = valid().withTitle("  Rugrats ").withContentId(" 42 ").copy(provider = Provider.DISNEY)
            .toEntity("rugrats")
        assertEquals("Rugrats", entity.title)
        assertEquals("42", entity.contentId)
        assertEquals(Provider.DISNEY.titleUrlTemplate, entity.deepLinkPattern)
        assertNull(entity.episodeLinkPattern)
    }

    @Test
    fun existingShowRoundTripKeepsLegacyPattern() {
        val legacy = show("old").copy(deepLinkPattern = "intent://legacy/{contentId}")
        val state = ShowEditorState.from(legacy)
        assertEquals(LinkChoice.CUSTOM, state.linkChoice)
        assertEquals(legacy, state.toEntity("old"))
        // Switching provider drops the custom pattern for the new provider.
        val other = state.steppedProvider(+1)
        assertEquals(LinkChoice.TITLE_PAGE, other.linkChoice)
        assertEquals(other.provider.titleUrlTemplate, other.toEntity("old").deepLinkPattern)
    }

    @Test
    fun idGeneration() {
        assertEquals("hey_arnold", slugify("Hey Arnold!", "show"))
        assertEquals("show", slugify("!!!", "show"))
        assertEquals("a", uniqueId("a", emptySet()))
        assertEquals("a_2", uniqueId("a", setOf("a")))
        assertEquals("a_3", uniqueId("a", setOf("a", "a_2")))
    }

    @Test
    fun historyCutoff() {
        val now = 1_700_000_000_000L
        assertEquals(now - Duration.ofDays(30).toMillis(), HistoryRetention.cutoffMillis(now))
        assertEquals(now - Duration.ofDays(1).toMillis(), HistoryRetention.cutoffMillis(now, 1))
        assertTrue(HistoryRetention.MAX_ROWS == 500)
    }
}
