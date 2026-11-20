package com.example.newtv.security

import com.example.newtv.data.ChannelEntity
import com.example.newtv.data.ShowEntity

/** Pure kid-safe filtering rules. No Android dependencies. */
object KidSafePolicy {
    /** Highest age rating (inclusive) that kid-safe mode allows, for channels and shows. */
    const val MAX_AGE_RATING = 7

    /** Show-rating cap to hand to the programming engine, or null when kid-safe is off. */
    fun maxShowRating(kidSafe: Boolean): Int? = if (kidSafe) MAX_AGE_RATING else null

    fun isShowAllowed(show: ShowEntity): Boolean = show.ageRating <= MAX_AGE_RATING

    /** Channel-level check only; see [visibleChannels] for the full rule including shows. */
    fun isChannelAllowed(channel: ChannelEntity): Boolean =
        channel.kidSafeOnly || channel.ageRating <= MAX_AGE_RATING

    fun allowedShows(shows: List<ShowEntity>): List<ShowEntity> = shows.filter(::isShowAllowed)

    /**
     * Channels visible in kid-safe mode: the channel itself must be allowed and it must still
     * have at least one allowed show (looked up in [showsByChannel]).
     */
    fun visibleChannels(
        channels: List<ChannelEntity>,
        showsByChannel: Map<String, List<ShowEntity>>
    ): List<ChannelEntity> = channels.filter { channel ->
        isChannelAllowed(channel) && showsByChannel[channel.id].orEmpty().any(::isShowAllowed)
    }
}
