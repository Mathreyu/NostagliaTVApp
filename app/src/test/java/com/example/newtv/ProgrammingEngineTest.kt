package com.example.newtv

import com.example.newtv.data.ChannelEntity
import com.example.newtv.data.Provider
import com.example.newtv.data.RecentEpisodeEntity
import com.example.newtv.data.RotationMode
import com.example.newtv.data.ScheduleRuleEntity
import com.example.newtv.data.ShowEntity
import com.example.newtv.data.TvRepository
import com.example.newtv.engine.Program
import com.example.newtv.engine.ProgrammingEngine
import com.example.newtv.resolver.DeepLinkResolver
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class ProgrammingEngineTest {
    private val zone = ZoneId.of("UTC")
    private val channel = ChannelEntity("cartoons", "Cartoons", 7, true, number = 3, accentColor = 0xFFFF4FD8)

    private fun show(id: String, seasons: Int = 1, episodes: Int = 2) = ShowEntity(
        id = id,
        title = id,
        provider = Provider.NETFLIX,
        deepLinkPattern = "intent://example/{contentId}",
        contentId = id,
        seasonCount = seasons,
        episodeCount = episodes,
        ageRating = 7
    )

    private fun rule(id: String, start: Int, end: Int, slot: Int) = ScheduleRuleEntity(
        id = id,
        channelId = channel.id,
        startMinute = start,
        endMinute = end,
        noRepeatWindow = 2,
        slotMinutes = slot,
        rotationMode = RotationMode.ROUND_ROBIN
    )

    private fun engine(
        shows: List<ShowEntity>,
        rules: List<ScheduleRuleEntity>,
        repo: FakeRepository = FakeRepository(channel, shows, rules)
    ) = ProgrammingEngine(repo, FakeResolver(), zone)

    private fun t(s: String) = Instant.parse(s)

    @Test
    fun deterministicAndNoWrites() = runTest {
        val repo = FakeRepository(channel, listOf(show("a"), show("b")), listOf(rule("r", 0, 1440, 30)))
        val engine = engine(emptyList(), emptyList(), repo)
        val now = t("2025-01-01T10:10:00Z")
        val first = engine.getProgramNow(channel.id, now)
        repeat(5) {
            assertEquals(first, engine.getProgramNow(channel.id, now))
            engine.getNextProgram(channel.id, now)
            engine.getSchedulePreview(channel.id, now)
            engine.getNowPlaying(null, now)
        }
        assertEquals(0, repo.recent.size)
        assertEquals(t("2025-01-01T10:00:00Z"), first?.startTime)
        assertEquals(t("2025-01-01T10:30:00Z"), first?.endTime)
    }

    @Test
    fun sameSlotSameProgramRegardlessOfInstantWithinSlot() = runTest {
        val e = engine(listOf(show("a"), show("b")), listOf(rule("r", 0, 1440, 30)))
        assertEquals(
            e.getProgramAt(channel.id, t("2025-01-01T10:00:00Z")),
            e.getProgramAt(channel.id, t("2025-01-01T10:29:59Z"))
        )
    }

    @Test
    fun roundRobinAcrossConsecutiveSlots() = runTest {
        val e = engine(listOf(show("b"), show("a"), show("c")), listOf(rule("r", 0, 1440, 30)))
        val ids = (0 until 6).map {
            e.getProgramAt(channel.id, t("2025-01-01T10:00:00Z").plusSeconds(it * 1800L))!!.show.id
        }
        assertEquals(listOf(ids[0], ids[1], ids[2]), ids.subList(3, 6))
        assertEquals(setOf("a", "b", "c"), ids.toSet())
        // ordered by id: a -> b -> c cyclically
        val order = listOf("a", "b", "c")
        val start = order.indexOf(ids[0])
        ids.forEachIndexed { i, id -> assertEquals(order[(start + i) % 3], id) }
    }

    @Test
    fun nextProgramStartsAtCurrentEnd() = runTest {
        val e = engine(listOf(show("a"), show("b")), listOf(rule("r", 0, 1440, 30)))
        val now = t("2025-01-01T10:10:00Z")
        val current = e.getProgramNow(channel.id, now)!!
        val next = e.getNextProgram(channel.id, now)!!
        assertEquals(current.endTime, next.startTime)
        assertTrue(current.show.id != next.show.id)
    }

    @Test
    fun previewIsContiguousAndDoesNotOverlapCurrent() = runTest {
        val e = engine(listOf(show("a"), show("b")), listOf(rule("r", 0, 1440, 30)))
        val now = t("2025-01-01T10:10:00Z")
        val current = e.getProgramNow(channel.id, now)!!
        val preview = e.getSchedulePreview(channel.id, now, 5)
        assertEquals(5, preview.size)
        assertEquals(current.endTime, preview.first().startTime)
        preview.zipWithNext().forEach { (x, y) -> assertEquals(x.endTime, y.startTime) }
        assertEquals(e.getNextProgram(channel.id, now), preview.first())
    }

    @Test
    fun timeOfDayRulesProduceCorrectBoundaries() = runTest {
        val rules = listOf(rule("a_morning", 540, 720, 15), rule("b_default", 0, 1440, 30))
        val e = engine(listOf(show("a"), show("b")), rules)
        val p = e.getProgramAt(channel.id, t("2025-01-01T09:20:00Z"))!!
        assertEquals(t("2025-01-01T09:15:00Z"), p.startTime)
        assertEquals(t("2025-01-01T09:30:00Z"), p.endTime)
        val q = e.getProgramAt(channel.id, t("2025-01-01T13:10:00Z"))!!
        assertEquals(t("2025-01-01T13:00:00Z"), q.startTime)
        assertEquals(t("2025-01-01T13:30:00Z"), q.endTime)
        // Whole day is contiguous with no overlap/gap across rule boundaries.
        var cursor = t("2025-01-01T00:00:00Z")
        val dayEnd = t("2025-01-02T00:00:00Z")
        while (cursor < dayEnd) {
            val prog = e.getProgramAt(channel.id, cursor)!!
            assertEquals(cursor, prog.startTime)
            assertTrue(prog.endTime > prog.startTime)
            cursor = prog.endTime
        }
        assertEquals(dayEnd, cursor)
        // 08:50 slot is truncated at the 09:00 boundary (30-min slot would end 09:20).
        val r = e.getProgramAt(channel.id, t("2025-01-01T08:45:00Z"))!!
        assertEquals(t("2025-01-01T08:30:00Z"), r.startTime)
        assertEquals(t("2025-01-01T09:00:00Z"), r.endTime)
        val last = e.getProgramAt(channel.id, t("2025-01-01T11:50:00Z"))!!
        assertEquals(t("2025-01-01T11:45:00Z"), last.startTime)
        assertEquals(t("2025-01-01T12:00:00Z"), last.endTime)
    }

    @Test
    fun truncatesSlotAtUnalignedRuleBoundary() = runTest {
        val rules = listOf(rule("a", 0, 100, 60), rule("b", 100, 1440, 30))
        val e = engine(listOf(show("a")), rules)
        val p = e.getProgramAt(channel.id, t("2025-01-01T01:10:00Z"))!!
        assertEquals(t("2025-01-01T01:00:00Z"), p.startTime)
        assertEquals(t("2025-01-01T01:40:00Z"), p.endTime)
        val q = e.getProgramAt(channel.id, t("2025-01-01T01:40:00Z"))!!
        assertEquals(t("2025-01-01T02:10:00Z"), q.endTime)
    }

    @Test
    fun ruleWrappingMidnight() = runTest {
        val rules = listOf(rule("a_night", 1320, 120, 60), rule("b_default", 0, 1440, 30))
        val e = engine(listOf(show("a"), show("b")), rules)
        val late = e.getProgramAt(channel.id, t("2025-01-01T22:20:00Z"))!!
        assertEquals(t("2025-01-01T22:00:00Z"), late.startTime)
        assertEquals(t("2025-01-01T23:00:00Z"), late.endTime)
        val early = e.getProgramAt(channel.id, t("2025-01-02T01:10:00Z"))!!
        assertEquals(t("2025-01-02T01:00:00Z"), early.startTime)
        assertEquals(t("2025-01-02T02:00:00Z"), early.endTime)
        val day = e.getProgramAt(channel.id, t("2025-01-02T03:10:00Z"))!!
        assertEquals(t("2025-01-02T03:00:00Z"), day.startTime)
        assertEquals(t("2025-01-02T03:30:00Z"), day.endTime)
        // Crossing midnight stays contiguous.
        val before = e.getProgramAt(channel.id, t("2025-01-01T23:30:00Z"))!!
        val after = e.getProgramAt(channel.id, before.endTime)!!
        assertEquals(t("2025-01-02T00:00:00Z"), after.startTime)
    }

    @Test
    fun noRulesUsesDefaultThirtyMinutes() = runTest {
        val e = engine(listOf(show("a")), emptyList())
        val p = e.getProgramAt(channel.id, t("2025-01-01T10:40:00Z"))!!
        assertEquals(t("2025-01-01T10:30:00Z"), p.startTime)
        assertEquals(t("2025-01-01T11:00:00Z"), p.endTime)
    }

    @Test
    fun episodesAdvanceAcrossSeasonsAndWrapAround() = runTest {
        // One show, 2 seasons x 3 episodes = 6; consecutive airings advance sequentially.
        val e = engine(listOf(show("a", seasons = 2, episodes = 3)), listOf(rule("r", 0, 1440, 30)))
        val progs = scheduleFrom(e, t("2025-01-01T00:00:00Z"), 13)
        val idx = progs.map { (it.season - 1) * 3 + (it.episode - 1) }
        idx.zipWithNext().forEach { (x, y) -> assertEquals((x + 1) % 6, y) }
        assertTrue(progs.any { it.season == 2 })
        assertEquals(idx[0], idx[6])
        assertEquals(idx[1], idx[7])
    }

    @Test
    fun noRepeatsWithinTotalEpisodes() = runTest {
        val shows = listOf(show("a", 2, 3), show("b", 1, 4))
        val e = engine(shows, listOf(rule("r", 0, 1440, 30)))
        val progs = scheduleFrom(e, t("2025-03-10T00:00:00Z"), 96)
        for (s in shows) {
            val total = s.seasonCount * s.episodeCount
            val keys = progs.filter { it.show.id == s.id }.map { it.season to it.episode }
            for (i in 0..keys.size - total) {
                assertEquals(total, keys.subList(i, i + total).toSet().size)
            }
        }
    }

    @Test
    fun progressIsClampedFraction() = runTest {
        val e = engine(listOf(show("a")), listOf(rule("r", 0, 1440, 30)))
        val p = e.getProgramNow(channel.id, t("2025-01-01T10:00:00Z"))!!
        assertEquals(0f, p.progress(t("2025-01-01T10:00:00Z")), 0.001f)
        assertEquals(0.5f, p.progress(t("2025-01-01T10:15:00Z")), 0.001f)
        assertEquals(1f, p.progress(t("2025-01-01T12:00:00Z")), 0.001f)
        assertEquals(0f, p.progress(t("2025-01-01T09:00:00Z")), 0.001f)
    }

    @Test
    fun noShowsYieldsNoProgramAndRecordPlayedWritesHistory() = runTest {
        val repo = FakeRepository(channel, emptyList(), emptyList())
        val e = ProgrammingEngine(repo, FakeResolver(), zone)
        assertNull(e.getProgramNow(channel.id, t("2025-01-01T10:00:00Z")))
        assertTrue(e.getSchedulePreview(channel.id, t("2025-01-01T10:00:00Z")).isEmpty())

        val repo2 = FakeRepository(channel, listOf(show("a")), emptyList())
        val e2 = ProgrammingEngine(repo2, FakeResolver(), zone)
        val program = e2.getProgramNow(channel.id, t("2025-01-01T10:00:00Z"))
        assertNotNull(program)
        e2.recordPlayed(program!!)
        assertEquals(1, repo2.recent.size)
        assertEquals("a", repo2.recent[0].showId)
    }

    @Test
    fun dstDayNeverCrashesOrOverlaps() = runTest {
        val ny = ZoneId.of("America/New_York")
        val e = ProgrammingEngine(
            FakeRepository(channel, listOf(show("a"), show("b")), listOf(rule("r", 0, 1440, 30))),
            FakeResolver(),
            ny
        )
        var cursor = t("2025-03-09T00:00:00Z").minusSeconds(6 * 3600)
        repeat(120) {
            val p = e.getProgramAt(channel.id, cursor)!!
            assertTrue(p.endTime > p.startTime)
            assertTrue(p.startTime <= cursor)
            cursor = p.endTime
        }
    }

    private suspend fun scheduleFrom(e: ProgrammingEngine, from: Instant, count: Int): List<Program> {
        val list = ArrayList<Program>()
        var cursor = from
        repeat(count) {
            val p = e.getProgramAt(channel.id, cursor)!!
            list.add(p)
            cursor = p.endTime
        }
        return list
    }

    private class FakeResolver : DeepLinkResolver {
        override fun buildDeepLink(show: ShowEntity, season: Int, episode: Int): String =
            "intent://example/${show.contentId}/$season/$episode"
    }

    private class FakeRepository(
        private val channel: ChannelEntity,
        private val shows: List<ShowEntity>,
        private val rules: List<ScheduleRuleEntity>
    ) : TvRepository(null) {
        val recent = mutableListOf<RecentEpisodeEntity>()

        override suspend fun getChannels(): List<ChannelEntity> = listOf(channel)
        override suspend fun getChannel(channelId: String): ChannelEntity? = channel
        override suspend fun getShows(channelId: String): List<ShowEntity> = shows
        override suspend fun getRules(channelId: String): List<ScheduleRuleEntity> = rules
        override suspend fun addRecentEpisode(recent: RecentEpisodeEntity) {
            this.recent.add(recent)
        }
    }
}
