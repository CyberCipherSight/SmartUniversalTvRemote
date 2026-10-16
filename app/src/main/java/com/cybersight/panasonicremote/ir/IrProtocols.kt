package com.cybersight.panasonicremote.ir

/**
 * Pure timing encoders. Every value is in microseconds and Android transmits
 * the array as mark, space, mark, space... starting with the IR LED ON.
 */
object IrProtocols {
    private const val NEC_HEADER_MARK = 9000
    private const val NEC_HEADER_SPACE = 4500
    private const val NEC_BIT_MARK = 560
    private const val NEC_ZERO_SPACE = 560
    private const val NEC_ONE_SPACE = 1690

    private const val SAMSUNG_HEADER_MARK = 4500
    private const val SAMSUNG_HEADER_SPACE = 4500
    private const val SAMSUNG_BIT_MARK = 560
    private const val SAMSUNG_ZERO_SPACE = 560
    private const val SAMSUNG_ONE_SPACE = 1690

    private const val NIKAI_HEADER_MARK = 4000
    private const val NIKAI_HEADER_SPACE = 4000
    private const val NIKAI_BIT_MARK = 500
    private const val NIKAI_ZERO_SPACE = 1000
    private const val NIKAI_ONE_SPACE = 2000
    private const val NIKAI_GAP = 8500

    /** Standard 32-bit NEC: address, inverted address, command, inverted command. */
    fun encodeNec(address: Int, command: Int): IntArray {
        require(address in 0..0xFF)
        require(command in 0..0xFF)
        val out = ArrayList<Int>(67)
        out += NEC_HEADER_MARK
        out += NEC_HEADER_SPACE
        appendByteLsb(out, address, NEC_BIT_MARK, NEC_ZERO_SPACE, NEC_ONE_SPACE)
        appendByteLsb(out, address xor 0xFF, NEC_BIT_MARK, NEC_ZERO_SPACE, NEC_ONE_SPACE)
        appendByteLsb(out, command, NEC_BIT_MARK, NEC_ZERO_SPACE, NEC_ONE_SPACE)
        appendByteLsb(out, command xor 0xFF, NEC_BIT_MARK, NEC_ZERO_SPACE, NEC_ONE_SPACE)
        out += NEC_BIT_MARK
        return out.toIntArray()
    }

    /** Extended NEC: two address bytes, command, inverted command. */
    fun encodeNecExtended(addressLow: Int, addressHigh: Int, command: Int): IntArray {
        require(addressLow in 0..0xFF)
        require(addressHigh in 0..0xFF)
        require(command in 0..0xFF)
        val out = ArrayList<Int>(67)
        out += NEC_HEADER_MARK
        out += NEC_HEADER_SPACE
        appendByteLsb(out, addressLow, NEC_BIT_MARK, NEC_ZERO_SPACE, NEC_ONE_SPACE)
        appendByteLsb(out, addressHigh, NEC_BIT_MARK, NEC_ZERO_SPACE, NEC_ONE_SPACE)
        appendByteLsb(out, command, NEC_BIT_MARK, NEC_ZERO_SPACE, NEC_ONE_SPACE)
        appendByteLsb(out, command xor 0xFF, NEC_BIT_MARK, NEC_ZERO_SPACE, NEC_ONE_SPACE)
        out += NEC_BIT_MARK
        return out.toIntArray()
    }

    /** Samsung 32-bit framing: address twice, command, inverted command. */
    fun encodeSamsung32(address: Int, command: Int): IntArray {
        require(address in 0..0xFF)
        require(command in 0..0xFF)
        val out = ArrayList<Int>(67)
        out += SAMSUNG_HEADER_MARK
        out += SAMSUNG_HEADER_SPACE
        appendByteLsb(out, address, SAMSUNG_BIT_MARK, SAMSUNG_ZERO_SPACE, SAMSUNG_ONE_SPACE)
        appendByteLsb(out, address, SAMSUNG_BIT_MARK, SAMSUNG_ZERO_SPACE, SAMSUNG_ONE_SPACE)
        appendByteLsb(out, command, SAMSUNG_BIT_MARK, SAMSUNG_ZERO_SPACE, SAMSUNG_ONE_SPACE)
        appendByteLsb(out, command xor 0xFF, SAMSUNG_BIT_MARK, SAMSUNG_ZERO_SPACE, SAMSUNG_ONE_SPACE)
        out += SAMSUNG_BIT_MARK
        return out.toIntArray()
    }

    /** NIKAI 24-bit framing used by a number of TCL Android-TV remotes. */
    fun encodeNikai24(data: Int): IntArray {
        require(data in 0..0xFF_FFFF)
        val out = ArrayList<Int>(52)
        out += NIKAI_HEADER_MARK
        out += NIKAI_HEADER_SPACE
        for (bit in 23 downTo 0) {
            out += NIKAI_BIT_MARK
            out += if (((data ushr bit) and 1) == 1) NIKAI_ONE_SPACE else NIKAI_ZERO_SPACE
        }
        out += NIKAI_BIT_MARK
        out += NIKAI_GAP
        return out.toIntArray()
    }

    private fun appendByteLsb(
        out: MutableList<Int>,
        value: Int,
        mark: Int,
        zeroSpace: Int,
        oneSpace: Int
    ) {
        for (bit in 0..7) {
            out += mark
            out += if (((value ushr bit) and 1) == 1) oneSpace else zeroSpace
        }
    }
}
