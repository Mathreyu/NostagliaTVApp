package com.example.newtv

import com.example.newtv.data.ChannelEntity
import com.example.newtv.data.Provider
import com.example.newtv.data.ShowEntity
import com.example.newtv.security.KidSafePolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KidSafePolicyTest {
    private fun channel(id: String, rating: Int, kidSafeOnly: Boolean = false) =
        ChannelEntity(id, id, rating, kidSafeOnly, number = 1)

    private fun show(id: String, rating: Int) = ShowEntity(
        id, id, Provider.NETFLIX, "intent://x/{contentId}", id, 1, 1, rating
    )

    @Test
    fun channelRule() {
        assertTrue(KidSafePolicy.isChannelAllowed(channel("a", 7)))
        assertTrue(KidSafePolicy.isChannelAllowed(channel("b", 18, kidSafeOnly = true)))
        assertFalse(KidSafePolicy.isChannelAllowed(channel("c", 8)))
    }

    @Test
    fun showRule() {
        assertTrue(KidSafePolicy.isShowAllowed(show("a", 7)))
        assertFalse(KidSafePolicy.isShowAllowed(show("b", 8)))
        assertEquals(listOf("a"), KidSafePolicy.allowedShows(listOf(show("a", 0), show("b", 12))).map { it.id })
    }

    @Test
    fun maxShowRatingOnlyWhenEnabled() {
        assertEquals(7, KidSafePolicy.maxShowRating(true))
        assertNull(KidSafePolicy.maxShowRating(false))
    }

    @Test
    fun visibleChannelsNeedAllowedChannelAndAllowedShow() {
        val channels = listOf(
            channel("ok", 3),
            channel("adultChannel", 16),
            channel("noAllowedShows", 3),
            channel("noShows", 3),
            channel("kidOnlyMixed", 18, kidSafeOnly = true)
        )
        val shows = mapOf(
            "ok" to listOf(show("s1", 7)),
            "adultChannel" to listOf(show("s2", 0)),
            "noAllowedShows" to listOf(show("s3", 12)),
            "kidOnlyMixed" to listOf(show("s4", 16), show("s5", 4))
        )
        val visible = KidSafePolicy.visibleChannels(channels, shows).map { it.id }
        assertEquals(listOf("ok", "kidOnlyMixed"), visible)
    }
}
