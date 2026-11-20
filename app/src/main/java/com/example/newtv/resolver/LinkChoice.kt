package com.example.newtv.resolver

import com.example.newtv.data.Provider
import com.example.newtv.data.ShowEntity

/** How a show's deep link is chosen in the show editor. */
enum class LinkChoice {
    /** Provider's title-page template; episodes are not targeted. */
    TITLE_PAGE,

    /** Provider's episode template (only for providers that have one). */
    EPISODE_LINK,

    /** Patterns the show already carried (e.g. older seeded intent:// links), kept as they are. */
    CUSTOM
}

/** A pattern pair that is neither of a provider's standard templates. */
data class CustomLink(val provider: Provider, val titlePattern: String, val episodePattern: String?)

object LinkChoices {
    /** Which choice describes [show]'s current patterns. */
    fun detect(show: ShowEntity): LinkChoice {
        val standardTitle = show.deepLinkPattern == show.provider.titleUrlTemplate
        val episode = show.episodeLinkPattern?.takeIf { it.isNotBlank() }
        return when {
            standardTitle && episode == null -> LinkChoice.TITLE_PAGE
            standardTitle && episode == show.provider.episodeUrlTemplate -> LinkChoice.EPISODE_LINK
            else -> LinkChoice.CUSTOM
        }
    }

    fun customOf(show: ShowEntity): CustomLink? =
        if (detect(show) == LinkChoice.CUSTOM) {
            CustomLink(show.provider, show.deepLinkPattern, show.episodeLinkPattern)
        } else {
            null
        }

    /** Choices offered for [provider]; CUSTOM only while [custom] was written for that provider. */
    fun available(provider: Provider, custom: CustomLink?): List<LinkChoice> = buildList {
        add(LinkChoice.TITLE_PAGE)
        if (provider.episodeUrlTemplate != null) add(LinkChoice.EPISODE_LINK)
        if (custom != null && custom.provider == provider) add(LinkChoice.CUSTOM)
    }

    /** [choice] if still offered for [provider], else TITLE_PAGE. */
    fun coerce(choice: LinkChoice, provider: Provider, custom: CustomLink?): LinkChoice =
        if (choice in available(provider, custom)) choice else LinkChoice.TITLE_PAGE

    fun step(choice: LinkChoice, provider: Provider, custom: CustomLink?, direction: Int): LinkChoice {
        val options = available(provider, custom)
        val index = options.indexOf(choice).let { if (it < 0) 0 else it }
        return options[Math.floorMod(index + if (direction >= 0) 1 else -1, options.size)]
    }

    /** The (title pattern, episode pattern) pair to store for [choice]. */
    fun patterns(choice: LinkChoice, provider: Provider, custom: CustomLink?): Pair<String, String?> =
        when (coerce(choice, provider, custom)) {
            LinkChoice.TITLE_PAGE -> provider.titleUrlTemplate to null
            LinkChoice.EPISODE_LINK -> provider.titleUrlTemplate to provider.episodeUrlTemplate
            LinkChoice.CUSTOM -> custom!!.titlePattern to custom.episodePattern
        }
}
