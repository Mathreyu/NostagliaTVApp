package com.example.newtv.ui

import com.example.newtv.data.ChannelEntity

sealed class Screen {
    data object Home : Screen()
    data class Channel(val channelId: String) : Screen()
    data class Schedule(val channelId: String) : Screen()
    data class RuleEditor(val channelId: String) : Screen()
    data object ChannelEditor : Screen()
    data class LineupEditor(val channelId: String) : Screen()

    /** Create ([showId] null) or edit one show of the library. */
    data class ShowEditor(val showId: String?) : Screen()

    /** Id of the channel this screen shows, if any. */
    val ownerChannelId: String?
        get() = when (this) {
            is Channel -> channelId
            is Schedule -> channelId
            is RuleEditor -> channelId
            is LineupEditor -> channelId
            Home, ChannelEditor, is ShowEditor -> null
        }

    val isEditor: Boolean
        get() = this is RuleEditor || this is ChannelEditor || this is LineupEditor || this is ShowEditor
}

/** Something a parent must unlock with the PIN while kid-safe mode stays on. */
sealed class ParentAction {
    data object AddChannel : ParentAction()
    data class EditChannel(val channelId: String) : ParentAction()
    data class EditRules(val channelId: String) : ParentAction()
}

/** Pure back-stack helpers. The stack is never empty; Home is the root. */
object BackStack {
    val Initial: List<Screen> = listOf(Screen.Home)

    fun push(stack: List<Screen>, screen: Screen): List<Screen> =
        if (stack.lastOrNull() == screen) stack else stack + screen

    /** Pops the top screen, or returns null when only the root remains. */
    fun pop(stack: List<Screen>): List<Screen>? =
        if (stack.size > 1) stack.dropLast(1) else null

    fun replaceTop(stack: List<Screen>, screen: Screen): List<Screen> =
        if (stack.isEmpty()) listOf(screen) else stack.dropLast(1) + screen

    /** Drops the first editor screen and everything above it. */
    fun pruneEditors(stack: List<Screen>): List<Screen> {
        val cut = stack.indexOfFirst { it.isEditor }
        return if (cut < 0) stack else stack.take(cut).ifEmpty { Initial }
    }

    /**
     * Cuts the stack just before the first screen whose channel is not in [allowedIds]
     * (and everything above it). Home is always kept.
     */
    fun pruneChannels(stack: List<Screen>, allowedIds: Set<String>): List<Screen> {
        val cut = stack.indexOfFirst { s -> s.ownerChannelId?.let { it !in allowedIds } == true }
        return if (cut < 0) stack else stack.take(cut).ifEmpty { Initial }
    }
}

/** Channel up/down ordering by channel number, wrapping at both ends. */
object ChannelNavigation {
    /**
     * The channel [step] positions away from [currentId] in number order (ties by name),
     * wrapping. When the current channel is not in [channels], +1 gives the first and -1 the last.
     * Null when there are no channels.
     */
    fun adjacent(channels: List<ChannelEntity>, currentId: String?, step: Int): ChannelEntity? {
        if (channels.isEmpty()) return null
        val ordered = channels.sortedWith(compareBy({ it.number }, { it.name }, { it.id }))
        val index = ordered.indexOfFirst { it.id == currentId }
        if (index < 0) return if (step >= 0) ordered.first() else ordered.last()
        return ordered[Math.floorMod(index + step, ordered.size)]
    }
}
