package com.example.newtv.ui

import com.example.newtv.security.KidSafePinManager

enum class PinDialogMode { CREATE, VERIFY }

/**
 * State of the PIN pad. Digits live here (never logged) and are cleared after every attempt.
 * CREATE mode has two passes: [firstEntry] is null while choosing and set while confirming.
 * [pendingAction] is run after a successful VERIFY instead of leaving kid-safe mode.
 */
data class PinDialogState(
    val mode: PinDialogMode,
    val digits: String = "",
    val firstEntry: String? = null,
    val error: String? = null,
    val busy: Boolean = false,
    val pendingAction: ParentAction? = null
) {
    val confirming: Boolean get() = mode == PinDialogMode.CREATE && firstEntry != null

    // Keep digits out of accidental toString() logging.
    override fun toString(): String =
        "PinDialogState(mode=$mode, digits=${digits.length}, confirming=$confirming, busy=$busy)"
}

enum class PinProblem { INVALID_LENGTH, MISMATCH }

sealed interface PinSubmit {
    data object Ignore : PinSubmit
    data class Problem(val state: PinDialogState, val problem: PinProblem) : PinSubmit
    data class Advance(val state: PinDialogState) : PinSubmit
    data class Create(val pin: String) : PinSubmit
    data class Verify(val pin: String) : PinSubmit
}

fun PinDialogState.withDigit(c: Char): PinDialogState =
    if (busy || c !in '0'..'9' || digits.length >= KidSafePinManager.MAX_LENGTH) this
    else copy(digits = digits + c, error = null)

fun PinDialogState.withBackspace(): PinDialogState =
    if (busy || digits.isEmpty()) this else copy(digits = digits.dropLast(1), error = null)

fun PinDialogState.submit(): PinSubmit {
    if (busy) return PinSubmit.Ignore
    return when (mode) {
        PinDialogMode.VERIFY ->
            if (digits.isEmpty()) PinSubmit.Ignore else PinSubmit.Verify(digits)
        PinDialogMode.CREATE -> {
            val first = firstEntry
            when {
                first == null ->
                    if (KidSafePinManager.isValidPin(digits)) {
                        PinSubmit.Advance(copy(firstEntry = digits, digits = "", error = null))
                    } else {
                        PinSubmit.Problem(copy(digits = ""), PinProblem.INVALID_LENGTH)
                    }
                digits != first ->
                    PinSubmit.Problem(copy(digits = "", firstEntry = null), PinProblem.MISMATCH)
                else -> PinSubmit.Create(first)
            }
        }
    }
}
