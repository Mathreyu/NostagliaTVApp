package com.example.newtv

import com.example.newtv.data.ChannelEntity
import com.example.newtv.data.Provider
import com.example.newtv.data.ShowEntity
import com.example.newtv.engine.Program
import com.example.newtv.ui.LiveBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.ZoneId

class LiveBoardTest {
    private val channel = ChannelEntity("a", "A", 7, false, number = 3)
    private val show = ShowEntity("s", "Show", Provider.NETFLIX, "", "1", 1, 1, 7)

    private fun program(start: String, end: String) =
        Program(channel, show, 1, 1, null, Instant.parse(start), Instant.parse(end))

    @Test
    fun refreshNeededOnlyAfterASlotEnds() {
        val p = program("2026-01-10T14:00:00Z", "2026-01-10T14:30:00Z")
        val map = mapOf("a" to p)
        assertFalse(LiveBoard.needsRefresh(map, null, Instant.parse("2026-01-10T14:29:59Z")))
        assertTrue(LiveBoard.needsRefresh(map, null, Instant.parse("2026-01-10T14:30:00Z")))
        assertTrue(LiveBoard.needsRefresh(emptyMap(), p, Instant.parse("2026-01-10T14:31:00Z")))
        assertFalse(LiveBoard.needsRefresh(emptyMap(), null, Instant.parse("2026-01-10T14:31:00Z")))
    }

    @Test
    fun guideStartFloorsToTheHalfHourInTheZone() {
        val now = Instant.parse("2026-01-10T14:47:30Z")
        assertEquals(Instant.parse("2026-01-10T14:30:00Z"), LiveBoard.guideStart(now, ZoneId.of("UTC")))
        // 45-minute offset zone: local 20:32 floors to 20:30 local = 14:45 UTC.
        assertEquals(Instant.parse("2026-01-10T14:45:00Z"), LiveBoard.guideStart(now, ZoneId.of("Asia/Kathmandu")))
    }

    @Test
    fun channelsAreOrderedByNumberThenName() {
        val list = listOf(
            ChannelEntity("c", "C", 7, false, number = 11),
            ChannelEntity("b", "B", 7, false, number = 3),
            ChannelEntity("a", "A", 7, false, number = 3)
        )
        assertEquals(listOf("a", "b", "c"), LiveBoard.ordered(list).map { it.id })
    }
}
