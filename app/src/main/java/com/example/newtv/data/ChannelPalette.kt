package com.example.newtv.data

/** Accent palette for channels (ARGB). Deliberately excludes yellow, which is reserved for focus. */
object ChannelPalette {
    val colors: List<Long> = listOf(
        0xFFFF4FD8,
        0xFF33F6FF,
        0xFF8CFF5E,
        0xFFFF9F1C,
        0xFFB388FF
    )

    const val DEFAULT: Long = 0xFF33F6FF

    fun forNumber(number: Int): Long = colors[Math.floorMod(number - 1, colors.size)]
}
