package com.cybersight.panasonicremote.ir

sealed interface IrSignal {
    val preferredCarrierHz: Int
    fun encode(): IntArray

    data class Panasonic(val command: Long) : IrSignal {
        override val preferredCarrierHz: Int = PanasonicProtocol.PREFERRED_CARRIER_HZ
        override fun encode(): IntArray = PanasonicProtocol.encode(command)
    }

    data class Nec(val address: Int, val command: Int) : IrSignal {
        override val preferredCarrierHz: Int = 38_000
        override fun encode(): IntArray = IrProtocols.encodeNec(address, command)
    }

    data class NecExtended(
        val addressLow: Int,
        val addressHigh: Int,
        val command: Int
    ) : IrSignal {
        override val preferredCarrierHz: Int = 38_000
        override fun encode(): IntArray = IrProtocols.encodeNecExtended(addressLow, addressHigh, command)
    }

    data class Samsung(val address: Int, val command: Int) : IrSignal {
        override val preferredCarrierHz: Int = 38_000
        override fun encode(): IntArray = IrProtocols.encodeSamsung32(address, command)
    }

    data class Nikai(val data: Int) : IrSignal {
        override val preferredCarrierHz: Int = 38_000
        override fun encode(): IntArray = IrProtocols.encodeNikai24(data)
    }
}
