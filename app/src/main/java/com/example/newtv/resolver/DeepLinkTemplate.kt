package com.example.newtv.resolver

class DeepLinkTemplate(private val pattern: String) {
    fun render(
        contentId: String,
        season: Int,
        episode: Int
    ): String {
        return pattern
            .replace("{contentId}", contentId)
            .replace("{season}", season.toString())
            .replace("{episode}", episode.toString())
    }
}
