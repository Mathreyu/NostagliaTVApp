package com.example.newtv.data

/**
 * A streaming app we can hand playback to.
 *
 * Link knowledge is data, not code: [titleUrlTemplate] opens a show's title page and
 * [episodeUrlTemplate] (null when the provider gives us no way to target a single episode) opens
 * one episode. Templates use `{contentId}`, `{season}` and `{episode}`.
 */
enum class Provider(
    val packageName: String,
    val displayName: String,
    val titleUrlTemplate: String,
    val episodeUrlTemplate: String? = null
) {
    NETFLIX(
        packageName = "com.netflix.ninja",
        displayName = "Netflix",
        titleUrlTemplate = "https://www.netflix.com/title/{contentId}"
    ),
    DISNEY(
        packageName = "com.disney.disneyplus",
        displayName = "Disney+",
        titleUrlTemplate = "https://www.disneyplus.com/browse/entity-{contentId}"
    );
}
