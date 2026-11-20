package com.example.newtv

import com.example.newtv.ui.ChannelEditorState
import com.example.newtv.ui.ChannelError
import com.example.newtv.ui.RuleEditorState
import com.example.newtv.ui.RuleError
import com.example.newtv.ui.RuleField
import com.example.newtv.ui.Steps
import com.example.newtv.ui.formatMinuteOfDay
import com.example.newtv.ui.stepped
import com.example.newtv.ui.steppedAge
import com.example.newtv.ui.validate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class EditorLogicTest {
    private fun rule(start: Int = 0, end: Int = 1440, noRepeat: Int = 4, slot: Int = 30) =
        RuleEditorState("c", start, end, noRepeat, slot)

    @Test
    fun startStepsBy15AndWraps() {
        assertEquals(15, rule(start = 0).stepped(RuleField.START, +1).startMinute)
        assertEquals(1425, rule(start = 0).stepped(RuleField.START, -1).startMinute)
        assertEquals(0, rule(start = 1425).stepped(RuleField.START, +1).startMinute)
    }

    @Test
    fun endAllowsMidnight1440AndWraps() {
        assertEquals(1440, rule(end = 1425).stepped(RuleField.END, +1).endMinute)
        assertEquals(0, rule(end = 1440).stepped(RuleField.END, +1).endMinute)
        assertEquals(1440, rule(end = 0).stepped(RuleField.END, -1).endMinute)
    }

    @Test
    fun offGridValuesSnapToNextStep() {
        assertEquals(30, rule(start = 20).stepped(RuleField.START, +1).startMinute)
        assertEquals(15, rule(start = 20).stepped(RuleField.START, -1).startMinute)
    }

    @Test
    fun slotLengthSteppsThroughFixedList() {
        assertEquals(45, rule(slot = 30).stepped(RuleField.SLOT_MINUTES, +1).slotMinutes)
        assertEquals(15, rule(slot = 30).stepped(RuleField.SLOT_MINUTES, -1).slotMinutes)
        assertEquals(15, rule(slot = 120).stepped(RuleField.SLOT_MINUTES, +1).slotMinutes)
        assertEquals(120, rule(slot = 15).stepped(RuleField.SLOT_MINUTES, -1).slotMinutes)
        assertEquals(60, rule(slot = 50).stepped(RuleField.SLOT_MINUTES, +1).slotMinutes)
    }

    @Test
    fun noRepeatRange() {
        assertEquals(1, rule(noRepeat = 50).stepped(RuleField.NO_REPEAT, +1).noRepeatWindow)
        assertEquals(50, rule(noRepeat = 1).stepped(RuleField.NO_REPEAT, -1).noRepeatWindow)
        assertEquals(5, rule(noRepeat = 4).stepped(RuleField.NO_REPEAT, +1).noRepeatWindow)
    }

    @Test
    fun steppingClearsError() {
        assertNull(rule().copy(error = "x").stepped(RuleField.START, +1).error)
    }

    @Test
    fun ruleValidation() {
        assertNull(rule().validate())
        assertNull(rule(start = 1320, end = 360).validate())
        assertEquals(RuleError.START_EQUALS_END, rule(start = 60, end = 60).validate())
        assertEquals(RuleError.START_END_RANGE, rule(start = -1).validate())
        assertEquals(RuleError.START_END_RANGE, rule(start = 1440, end = 0).validate())
        assertEquals(RuleError.START_END_RANGE, rule(end = 1500).validate())
        assertEquals(RuleError.SLOT_LENGTH, rule(slot = 4).validate())
        assertEquals(RuleError.NO_REPEAT_RANGE, rule(noRepeat = 0).validate())
        assertEquals(RuleError.NO_REPEAT_RANGE, rule(noRepeat = 51).validate())
    }

    @Test
    fun steppedValuesAlwaysValidate() {
        var r = rule(start = 0, end = 1440)
        repeat(200) {
            r = r.stepped(RuleField.END, +1).stepped(RuleField.SLOT_MINUTES, +1).stepped(RuleField.NO_REPEAT, +1)
            assertEquals(true, r.endMinute in 0..1440)
        }
        assertEquals(97, Steps.EndTimes.size)
        assertEquals(96, Steps.StartTimes.size)
    }

    @Test
    fun channelValidationAndAgeStepper() {
        val e = ChannelEditorState(null, "  ", 7, false, true)
        assertEquals(ChannelError.NAME_REQUIRED, e.validate())
        assertNull(e.copy(name = "Cartoons").validate())
        assertEquals(ChannelError.AGE_RANGE, e.copy(name = "x", ageRating = 22).validate())
        assertEquals(8, e.steppedAge(+1).ageRating)
        assertEquals(0, e.copy(ageRating = 21).steppedAge(+1).ageRating)
        assertEquals(21, e.copy(ageRating = 0).steppedAge(-1).ageRating)
    }

    @Test
    fun timeFormatting() {
        assertEquals("12:00 AM", formatMinuteOfDay(0, Locale.US))
        assertEquals("4:30 PM", formatMinuteOfDay(16 * 60 + 30, Locale.US))
        assertEquals("11:45 PM", formatMinuteOfDay(1425, Locale.US))
        assertEquals("12:00 AM", formatMinuteOfDay(1440, Locale.US))
    }
}
