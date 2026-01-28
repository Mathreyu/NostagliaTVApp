package com.example.newtv

import com.example.newtv.data.ChannelEntity
import com.example.newtv.data.ChannelStateEntity
import com.example.newtv.data.EpisodeStateEntity
import com.example.newtv.data.RecentEpisodeEntity
import com.example.newtv.data.RotationMode
import com.example.newtv.data.ScheduleRuleEntity
import com.example.newtv.data.ShowEntity
import com.example.newtv.data.TvRepository
import com.example.newtv.engine.ProgrammingEngine
import com.example.newtv.resolver.DeepLinkResolver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.Instant

class ProgrammingEngineTest {
    @Test
    fun selectNextProgramRotatesShows() = runTest {
        val channel = ChannelEntity("cartoons", "Cartoons", ageRating = 7, kidSafeOnly = true)
        val shows = listOf(
            ShowEntity(
                id = "show_a",
                title = "Alpha",
                provider = com.example.newtv.data.Provider.NETFLIX,
                deepLinkPattern = "intent://example/{contentId}",
                contentId = "111",
                seasonCount = 1,
                episodeCount = 2,
                ageRating = 7
            ),
            ShowEntity(
                id = "show_b",
                title = "Bravo",
                provider = com.example.newtv.data.Provider.NETFLIX,
                deepLinkPattern = "intent://example/{contentId}",
                contentId = "222",
                seasonCount = 1,
                episodeCount = 2,
                ageRating = 7
            )
        )
        val rules = listOf(
            ScheduleRuleEntity(
                id = "rule",
                channelId = channel.id,
                startMinute = 0,
                endMinute = 1440,
                noRepeatWindow = 2,
                rotationMode = RotationMode.ROUND_ROBIN
            )
        )
        val repo = FakeRepository(channel, shows, rules)
        val engine = ProgrammingEngine(repo, FakeResolver())

        val now = Instant.parse("2025-01-01T10:00:00Z")
        val first = engine.getProgramNow(channel.id, now)
        val second = engine.getNextProgram(channel.id, now)

        assertNotNull(first)
        assertNotNull(second)
        assertEquals("show_a", first?.show?.id)
        assertEquals("show_b", second?.show?.id)
    }

    private class FakeResolver : DeepLinkResolver {
        override fun buildDeepLink(show: ShowEntity, season: Int, episode: Int): String {
            return "intent://example/${show.contentId}"
        }
    }

    private class FakeRepository(
        private val channel: ChannelEntity,
        private val shows: List<ShowEntity>,
        private val rules: List<ScheduleRuleEntity>
    ) : TvRepository(null) {
        private var channelState: ChannelStateEntity? = null
        private val episodeState = mutableMapOf<String, EpisodeStateEntity>()
        private val recent = mutableListOf<RecentEpisodeEntity>()

        override suspend fun getChannel(channelId: String): ChannelEntity? = channel

        override suspend fun getShows(channelId: String): List<ShowEntity> = shows

        override suspend fun getRules(channelId: String): List<ScheduleRuleEntity> = rules

        override suspend fun getChannelState(channelId: String): ChannelStateEntity? = channelState

        override suspend fun upsertChannelState(state: ChannelStateEntity) {
            channelState = state
        }

        override suspend fun getEpisodeState(showId: String): EpisodeStateEntity? = episodeState[showId]

        override suspend fun upsertEpisodeState(state: EpisodeStateEntity) {
            episodeState[state.showId] = state
        }

        override suspend fun addRecentEpisode(recentEpisode: RecentEpisodeEntity) {
            recent.add(recentEpisode)
        }

        override suspend fun getRecentEpisodes(
            channelId: String,
            limit: Int
        ): List<RecentEpisodeEntity> {
            return recent.takeLast(limit).reversed()
        }
    }
}
