package com.example.newtv

import com.example.newtv.data.ChannelEntity
import com.example.newtv.ui.BackStack
import com.example.newtv.ui.ChannelNavigation
import com.example.newtv.ui.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationTest {
    private val home = Screen.Home
    private fun ch(id: String, n: Int, name: String = id) = ChannelEntity(id, name, 0, false, number = n)

    @Test
    fun pushPopAndRoot() {
        var stack = BackStack.Initial
        stack = BackStack.push(stack, Screen.Channel("a"))
        stack = BackStack.push(stack, Screen.RuleEditor("a"))
        assertEquals(listOf(home, Screen.Channel("a"), Screen.RuleEditor("a")), stack)
        stack = BackStack.pop(stack)!!
        assertEquals(Screen.Channel("a"), stack.last())
        stack = BackStack.pop(stack)!!
        assertEquals(listOf(home), stack)
        assertNull(BackStack.pop(stack))
    }

    @Test
    fun pushSameTopIsNoOp() {
        val stack = BackStack.push(BackStack.Initial, Screen.Channel("a"))
        assertEquals(stack, BackStack.push(stack, Screen.Channel("a")))
    }

    @Test
    fun replaceTopKeepsDepth() {
        val stack = listOf(home, Screen.Channel("a"))
        assertEquals(listOf(home, Screen.Channel("b")), BackStack.replaceTop(stack, Screen.Channel("b")))
    }

    @Test
    fun pruneChannelsCutsAtFirstDisallowed() {
        val stack = listOf(home, Screen.Channel("a"), Screen.Schedule("a"))
        assertEquals(stack, BackStack.pruneChannels(stack, setOf("a")))
        assertEquals(listOf(home), BackStack.pruneChannels(stack, setOf("b")))
        val deep = listOf(home, Screen.Channel("a"), Screen.ChannelEditor, Screen.RuleEditor("a"))
        assertEquals(listOf(home), BackStack.pruneChannels(deep, emptySet()))
    }

    @Test
    fun pruneEditorsDropsEditorsAndAbove() {
        val stack = listOf(home, Screen.Channel("a"), Screen.ChannelEditor, Screen.RuleEditor("a"))
        assertEquals(listOf(home, Screen.Channel("a")), BackStack.pruneEditors(stack))
        assertEquals(listOf(home), BackStack.pruneEditors(listOf(home, Screen.ChannelEditor)))
    }

    @Test
    fun screenHelpers() {
        assertTrue(Screen.ChannelEditor.isEditor)
        assertTrue(Screen.RuleEditor("a").isEditor)
        assertEquals("a", Screen.Schedule("a").ownerChannelId)
        assertNull(Screen.Home.ownerChannelId)
    }

    @Test
    fun channelUpDownOrderedByNumberAndWraps() {
        val channels = listOf(ch("c", 9), ch("a", 2), ch("b", 5))
        assertEquals("b", ChannelNavigation.adjacent(channels, "a", +1)!!.id)
        assertEquals("c", ChannelNavigation.adjacent(channels, "b", +1)!!.id)
        assertEquals("a", ChannelNavigation.adjacent(channels, "c", +1)!!.id)
        assertEquals("c", ChannelNavigation.adjacent(channels, "a", -1)!!.id)
        assertEquals("a", ChannelNavigation.adjacent(channels, "b", -1)!!.id)
    }

    @Test
    fun unknownCurrentAndEmpty() {
        val channels = listOf(ch("a", 2), ch("b", 5))
        assertEquals("a", ChannelNavigation.adjacent(channels, "gone", +1)!!.id)
        assertEquals("b", ChannelNavigation.adjacent(channels, "gone", -1)!!.id)
        assertNull(ChannelNavigation.adjacent(emptyList(), "a", +1))
        assertEquals("a", ChannelNavigation.adjacent(listOf(ch("a", 1)), "a", +1)!!.id)
    }
}
