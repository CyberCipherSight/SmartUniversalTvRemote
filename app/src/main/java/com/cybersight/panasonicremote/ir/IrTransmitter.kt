package com.cybersight.panasonicremote.ir

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager

/** Defensive wrapper around Android ConsumerIrManager. OEM service failures are returned, never fatal. */
class IrTransmitter(context: Context) {
    private val appContext = context.applicationContext
    private val irManager = appContext.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager

    val hasEmitter: Boolean
        get() {
            if (!appContext.packageManager.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)) return false
            return runCatching { irManager?.hasIrEmitter() == true }.getOrDefault(false)
        }

    fun supportedRangeDescription(): String {
        val ranges = safeCarrierFrequencies()
        if (ranges.isEmpty()) return "carrier ranges unavailable"
        return ranges.joinToString { "${it.minFrequency / 1000}-${it.maxFrequency / 1000} kHz" }
    }

    fun carrierFor(signal: IrSignal): Int = chooseCarrier(signal.preferredCarrierHz)

    fun send(signal: IrSignal): Result<Int> {
        if (!hasEmitter) return Result.failure(IllegalStateException("No usable consumer IR emitter detected"))
        val manager = irManager ?: return Result.failure(IllegalStateException("Android consumer IR service is unavailable"))
        val carrier = carrierFor(signal)
        return runCatching {
            manager.transmit(carrier, signal.encode())
            carrier
        }
    }

    private fun chooseCarrier(preferred: Int): Int {
        val ranges = safeCarrierFrequencies()
        if (ranges.isEmpty()) return preferred
        ranges.firstOrNull { preferred in it.minFrequency..it.maxFrequency }?.let { return preferred }
        return ranges.flatMap { listOf(it.minFrequency, it.maxFrequency) }
            .minByOrNull { kotlin.math.abs(it - preferred) } ?: preferred
    }

    private fun safeCarrierFrequencies(): Array<ConsumerIrManager.CarrierFrequencyRange> {
        val manager = irManager ?: return emptyArray()
        return runCatching { manager.carrierFrequencies ?: emptyArray() }.getOrElse { emptyArray() }
    }
}
