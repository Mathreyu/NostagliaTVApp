package com.example.newtv.screenshots

import com.example.newtv.data.ChannelEntity
import com.example.newtv.data.ChannelPalette
import com.example.newtv.data.Provider
import com.example.newtv.data.ShowEntity
import com.example.newtv.engine.Program
import com.example.newtv.resolver.LinkKind
import com.example.newtv.ui.MainUiState
import com.example.newtv.ui.Screen
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime

/** Deterministic data for the screenshot tests: fixed clock, fixed zone, fixed programs. */
object Fixtures {
    val zone: ZoneId = ZoneOffset.UTC

    /** Saturday evening, ten minutes into the 7:00 slot. */
    val now: Instant = Instant.parse("2026-03-14T19:10:00Z")
    val clock: ZonedDateTime = now.atZone(zone)

    private fun channel(id: String, name: String, number: Int, age: Int = 10, kidSafe: Boolean = false) =
        ChannelEntity(id, name, age, kidSafe, number, ChannelPalette.forNumber(number))

    val channels = listOf(
        channel("cartoons", "Saturday Morning Cartoons", 3, 7, true),
        channel("nick", "Classic Nickelodeon", 7),
        channel("disney", "Disney After School", 11, 7, true),
        channel("sitcoms", "Retro Sitcoms", 24, 13),
        channel("scifi", "Late Night Sci-Fi", 42, 16)
    )

    private fun show(id: String, title: String, provider: Provider = Provider.NETFLIX, age: Int = 7) = ShowEntity(
        id = id, title = title, provider = provider, deepLinkPattern = "https://example.invalid/$id",
        contentId = id, seasonCount = 4, episodeCount = 12, ageRating = age
    )

    private val titles = mapOf(
        "cartoons" to listOf("Looney Tunes", "Scooby-Doo, Where Are You!", "The Smurfs", "Tom and Jerry", "Pink Panther"),
        "nick" to listOf("Rugrats", "Hey Arnold!", "The Wild Thornberrys", "Doug", "All That"),
        "disney" to listOf("Recess", "Gargoyles", "DuckTales", "Darkwing Duck", "Pepper Ann"),
        "sitcoms" to listOf("Seinfeld", "Fresh Prince of Bel-Air", "Boy Meets World", "Friends", "Frasier"),
        "scifi" to listOf("The X-Files", "Star Trek: Voyager", "Babylon 5", "Quantum Leap", "Sliders")
    )

    fun program(channel: ChannelEntity, index: Int, linkKind: LinkKind = LinkKind.EPISODE): Program {
        val start = Instant.parse("2026-03-14T19:00:00Z").plusSeconds(1800L * index)
        val title = titles.getValue(channel.id)[index % 5]
        return Program(
            channel = channel,
            show = show("${channel.id}-$index", title, if (index % 2 == 0) Provider.NETFLIX else Provider.DISNEY),
            season = 1 + index, episode = 3 + index * 2,
            deepLink = "https://example.invalid", startTime = start, endTime = start.plusSeconds(1800),
            linkKind = linkKind
        )
    }

    private fun programsFor(channel: ChannelEntity) = (0..4).map { program(channel, it) }

    val guide: Map<String, List<Program>> = channels.associate { it.id to programsFor(it) }
    val nowPlaying: Map<String, Program> = guide.mapValues { it.value.first() }

    fun home() = MainUiState(
        channels = channels, nowPlaying = nowPlaying, guide = guide, now = now,
        lastFocusedChannelId = "cartoons"
    )

    fun homeKidSafeEmpty() = MainUiState(kidSafeEnabled = true, now = now)

    fun channelScreen(linkKind: LinkKind = LinkKind.EPISODE, providerAvailable: Boolean? = true): MainUiState {
        val ch = channels[1]
        val list = guide.getValue(ch.id)
        val current = list.first().copy(linkKind = linkKind)
        return home().copy(
            backStack = listOf(Screen.Home, Screen.Channel(ch.id)),
            currentProgram = current,
            schedule = list.drop(1),
            providerAvailable = providerAvailable
        )
    }

    fun scheduleScreen(): MainUiState {
        val ch = channels[1]
        val list = (0..7).map { program(ch, it) }
        return home().copy(
            backStack = listOf(Screen.Home, Screen.Channel(ch.id), Screen.Schedule(ch.id)),
            currentProgram = list.first(),
            schedule = list.drop(1)
        )
    }
}
