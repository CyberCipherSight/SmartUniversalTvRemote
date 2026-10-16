package com.cybersight.panasonicremote.ir

/**
 * Encoder matching the captured Panasonic Viera waveform used by the bundled profile.
 * Timing values are microseconds.
 *
 * Frame layout:
 *   header + 16-bit prefix 0x4004 + 32-bit command + trailing mark
 * Bits are transmitted MSB-first, matching the source LIRC profile.
 */
object PanasonicProtocol {
    const val PREFERRED_CARRIER_HZ = 37_000

    private const val HEADER_MARK = 3467
    private const val HEADER_SPACE = 1711
    private const val BIT_MARK = 449
    private const val ONE_SPACE = 1277
    private const val ZERO_SPACE = 413
    private const val TRAILING_MARK = 443
    private const val PREFIX = 0x4004L

    fun encode(command: Long): IntArray {
        require(command in 0..0xFFFF_FFFFL) { "Command must fit in 32 bits" }

        val pattern = ArrayList<Int>(2 + (48 * 2) + 1)
        pattern += HEADER_MARK
        pattern += HEADER_SPACE
        appendBits(pattern, PREFIX, 16)
        appendBits(pattern, command, 32)
        pattern += TRAILING_MARK
        return pattern.toIntArray()
    }

    private fun appendBits(out: MutableList<Int>, value: Long, bitCount: Int) {
        for (bit in bitCount - 1 downTo 0) {
            out += BIT_MARK
            out += if (((value ushr bit) and 1L) == 1L) ONE_SPACE else ZERO_SPACE
        }
    }
}
