import com.cybersight.panasonicremote.ir.PanasonicCodes
import com.cybersight.panasonicremote.ir.PanasonicProtocol

fun main() {
    check(PanasonicCodes.get("POWER") == 0x0100BCBDL)
    val pattern = PanasonicProtocol.encode(PanasonicCodes.get("POWER")!!)
    check(pattern.size == 99) { "Expected 99 timing values, got ${pattern.size}" }
    check(pattern[0] == 3467 && pattern[1] == 1711)
    check(pattern.last() == 443)
    check(pattern.sum() < 2_000_000) { "Android rejects IR patterns >= 2 seconds" }
    println("Protocol verification passed: ${pattern.size} timings, ${pattern.sum()} us total")
}
