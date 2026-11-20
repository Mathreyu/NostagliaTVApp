package com.example.newtv.resolver

import com.example.newtv.data.ShowEntity

/** What a resolved deep link opens. */
enum class LinkKind {
    /** The exact episode. */
    EPISODE,

    /** The show's title page only; the viewer has to pick the episode themselves. */
    TITLE_PAGE
}

data class ResolvedLink(val url: String, val kind: LinkKind)

/** Pure link selection: no Android, no package lookups. */
object LinkSelector {
    /** The episode template for [show]: its own pattern, else the provider's, else null. */
    fun episodePattern(show: ShowEntity): String? =
        show.episodeLinkPattern?.takeIf { it.isNotBlank() } ?: show.provider.episodeUrlTemplate

    /** The title-page template for [show]: its own pattern, else the provider's. */
    fun titlePattern(show: ShowEntity): String =
        show.deepLinkPattern.takeIf { it.isNotBlank() } ?: show.provider.titleUrlTemplate

    fun kindFor(show: ShowEntity): LinkKind =
        if (episodePattern(show) != null) LinkKind.EPISODE else LinkKind.TITLE_PAGE

    fun select(show: ShowEntity, season: Int, episode: Int): ResolvedLink {
        val episodePattern = episodePattern(show)
        return if (episodePattern != null) {
            ResolvedLink(DeepLinkTemplate(episodePattern).render(show.contentId, season, episode), LinkKind.EPISODE)
        } else {
            ResolvedLink(DeepLinkTemplate(titlePattern(show)).render(show.contentId, season, episode), LinkKind.TITLE_PAGE)
        }
    }
}
