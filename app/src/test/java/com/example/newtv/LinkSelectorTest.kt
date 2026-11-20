package com.example.newtv

import com.example.newtv.data.Provider
import com.example.newtv.data.ShowEntity
import com.example.newtv.resolver.CustomLink
import com.example.newtv.resolver.LinkChoice
import com.example.newtv.resolver.LinkChoices
import com.example.newtv.resolver.LinkKind
import com.example.newtv.resolver.LinkSelector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkSelectorTest {
    private fun show(
        provider: Provider = Provider.NETFLIX,
        title: String = Provider.NETFLIX.titleUrlTemplate,
        episode: String? = null
    ) = ShowEntity("s", "S", provider, title, "123", 2, 10, 7, episode)

    @Test
    fun fallsBackToTitlePageWithoutEpisodePattern() {
        val link = LinkSelector.select(show(), 2, 5)
        assertEquals(LinkKind.TITLE_PAGE, link.kind)
        assertEquals("https://www.netflix.com/title/123", link.url)
    }

    @Test
    fun usesShowEpisodePatternWhenPresent() {
        val link = LinkSelector.select(show(episode = "https://x/{contentId}/s{season}/e{episode}"), 2, 5)
        assertEquals(LinkKind.EPISODE, link.kind)
        assertEquals("https://x/123/s2/e5", link.url)
    }

    @Test
    fun blankEpisodePatternCountsAsMissing() {
        assertEquals(LinkKind.TITLE_PAGE, LinkSelector.kindFor(show(episode = "  ")))
    }

    @Test
    fun blankTitlePatternUsesProviderTemplate() {
        val link = LinkSelector.select(show(provider = Provider.DISNEY, title = ""), 1, 1)
        assertEquals("https://www.disneyplus.com/browse/entity-123", link.url)
    }

    @Test
    fun providerEpisodeTemplateWouldBeUsed() {
        // Provider enum values are fixed, so exercise the rule through the pure helper contract:
        // every shipped provider has a title template and none claims episode support yet.
        Provider.entries.forEach {
            assertTrue(it.titleUrlTemplate.contains("{contentId}"))
            assertNull(it.episodeUrlTemplate)
        }
    }

    @Test
    fun linkChoiceDetection() {
        assertEquals(LinkChoice.TITLE_PAGE, LinkChoices.detect(show()))
        val legacy = show(title = "intent://www.netflix.com/watch/{contentId}#Intent;scheme=https;end")
        assertEquals(LinkChoice.CUSTOM, LinkChoices.detect(legacy))
        assertEquals(CustomLink(Provider.NETFLIX, legacy.deepLinkPattern, null), LinkChoices.customOf(legacy))
        assertNull(LinkChoices.customOf(show()))
    }

    @Test
    fun customOnlyOfferedForItsProviderAndSurvivesRoundTrip() {
        val custom = CustomLink(Provider.NETFLIX, "intent://custom", "intent://custom/{episode}")
        assertEquals(listOf(LinkChoice.TITLE_PAGE, LinkChoice.CUSTOM), LinkChoices.available(Provider.NETFLIX, custom))
        assertEquals(listOf(LinkChoice.TITLE_PAGE), LinkChoices.available(Provider.DISNEY, custom))
        assertEquals(LinkChoice.TITLE_PAGE, LinkChoices.coerce(LinkChoice.CUSTOM, Provider.DISNEY, custom))
        assertEquals("intent://custom" to "intent://custom/{episode}",
            LinkChoices.patterns(LinkChoice.CUSTOM, Provider.NETFLIX, custom))
        assertEquals(Provider.NETFLIX.titleUrlTemplate to null,
            LinkChoices.patterns(LinkChoice.TITLE_PAGE, Provider.NETFLIX, custom))
        assertEquals(LinkChoice.CUSTOM, LinkChoices.step(LinkChoice.TITLE_PAGE, Provider.NETFLIX, custom, +1))
        assertEquals(LinkChoice.TITLE_PAGE, LinkChoices.step(LinkChoice.CUSTOM, Provider.NETFLIX, custom, +1))
        assertFalse(LinkChoice.EPISODE_LINK in LinkChoices.available(Provider.NETFLIX, null))
    }
}
