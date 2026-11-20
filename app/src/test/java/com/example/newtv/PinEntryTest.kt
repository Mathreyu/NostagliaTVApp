package com.example.newtv

import com.example.newtv.ui.PinDialogMode
import com.example.newtv.ui.PinDialogState
import com.example.newtv.ui.PinProblem
import com.example.newtv.ui.PinSubmit
import com.example.newtv.ui.submit
import com.example.newtv.ui.withBackspace
import com.example.newtv.ui.withDigit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinEntryTest {
    private fun enter(state: PinDialogState, digits: String) = digits.fold(state) { s, c -> s.withDigit(c) }

    @Test
    fun digitsAreFilteredAndCapped() {
        var s = PinDialogState(PinDialogMode.VERIFY)
        s = s.withDigit('x')
        assertEquals("", s.digits)
        s = enter(s, "123456789")
        assertEquals("12345678", s.digits)
        assertEquals("1234567", s.withBackspace().digits)
        assertEquals("", PinDialogState(PinDialogMode.VERIFY).withBackspace().digits)
    }

    @Test
    fun busyIgnoresInput() {
        val s = PinDialogState(PinDialogMode.VERIFY, digits = "12", busy = true)
        assertEquals("12", s.withDigit('3').digits)
        assertEquals("12", s.withBackspace().digits)
        assertEquals(PinSubmit.Ignore, s.submit())
    }

    @Test
    fun verifySubmit() {
        assertEquals(PinSubmit.Ignore, PinDialogState(PinDialogMode.VERIFY).submit())
        assertEquals(PinSubmit.Verify("1234"), enter(PinDialogState(PinDialogMode.VERIFY), "1234").submit())
    }

    @Test
    fun createNeedsEnterThenConfirm() {
        val first = enter(PinDialogState(PinDialogMode.CREATE), "2468")
        val advance = first.submit() as PinSubmit.Advance
        assertTrue(advance.state.confirming)
        assertEquals("", advance.state.digits)
        assertEquals(PinSubmit.Create("2468"), enter(advance.state, "2468").submit())
    }

    @Test
    fun createMismatchRestarts() {
        val advance = enter(PinDialogState(PinDialogMode.CREATE), "2468").submit() as PinSubmit.Advance
        val problem = enter(advance.state, "1111").submit() as PinSubmit.Problem
        assertEquals(PinProblem.MISMATCH, problem.problem)
        assertFalse(problem.state.confirming)
        assertEquals("", problem.state.digits)
    }

    @Test
    fun createRejectsShortPin() {
        val problem = enter(PinDialogState(PinDialogMode.CREATE), "12").submit() as PinSubmit.Problem
        assertEquals(PinProblem.INVALID_LENGTH, problem.problem)
    }

    @Test
    fun toStringDoesNotLeakDigits() {
        assertFalse(enter(PinDialogState(PinDialogMode.VERIFY), "9876").toString().contains("9876"))
    }
}
