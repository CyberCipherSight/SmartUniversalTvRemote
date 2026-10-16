import com.cybersight.panasonicremote.ir.*
import com.cybersight.panasonicremote.tv.*

fun main() {
    fun checkSignal(name: String, signal: IrSignal) {
        val p = signal.encode()
        check(p.isNotEmpty()) { "$name produced no pattern" }
        check(p.all { it > 0 }) { "$name contains non-positive timing" }
        check(p.sum() < 2_000_000) { "$name exceeds Android's 2 second transmit limit" }
    }

    val panasonic = TvProfiles.byId(TvProfiles.PANASONIC_VIERA)!!
    check(panasonic.signal("POWER") == IrSignal.Panasonic(0x0100BCBDL))
    val pp = panasonic.signal("POWER")!!.encode()
    check(pp.size == 99) { "Panasonic baseline changed: ${pp.size}" }
    check(pp.sum() in 58_000..60_500) { "Panasonic baseline duration changed: ${pp.sum()}" }

    val samsung = TvProfiles.byId(TvProfiles.SAMSUNG_STANDARD)!!
    check(samsung.signal("POWER") == IrSignal.Samsung(0x07, 0x02))
    check(samsung.signal("VOL_UP") == IrSignal.Samsung(0x07, 0x07))
    check(samsung.signal("POWER")!!.encode().size == 67)

    val lg = TvProfiles.byId(TvProfiles.LG_STANDARD)!!
    check(lg.signal("POWER") == IrSignal.Nec(0x04, 0x08))
    check(lg.signal("VOL_UP") == IrSignal.Nec(0x04, 0x02))
    check(lg.signal("POWER")!!.encode().size == 67)

    val xiaomi = TvProfiles.byId(TvProfiles.XIAOMI_MI_TV_80)!!
    check(xiaomi.signal("POWER") == IrSignal.Nec(0x80, 0x46))
    check(xiaomi.signal("HOME") == IrSignal.Nec(0x80, 0x17))

    val tcl = TvProfiles.byId(TvProfiles.TCL_NIKAI)!!
    check(tcl.signal("POWER") == IrSignal.Nikai(0x0D5F2A))
    check(tcl.signal("VOL_UP") == IrSignal.Nikai(0x0D0F2F))
    check(tcl.signal("POWER")!!.encode().size == 52)

    check(TvProfiles.all.map { it.id }.distinct().size == TvProfiles.all.size)
    check(TvProfiles.all.all { it.supports("POWER") })
    TvProfiles.all.forEach { profile ->
        profile.commands.forEach { (command, signal) -> checkSignal("${profile.id}:$command", signal) }
    }

    ModelCatalog.brands.filter { it != ModelCatalog.AUTO_BRAND }.forEach { brand ->
        check(ModelCatalog.seriesFor(brand).isNotEmpty()) { "No model hints for $brand" }
        ModelCatalog.seriesFor(brand).forEach { hint ->
            check(TvProfiles.byId(hint.profileId) != null) { "Broken profile mapping: ${hint.label}" }
        }
    }

    println("Profiles: ${TvProfiles.all.size}")
    println("Brands: ${TvProfiles.all.map { it.brand }.distinct().joinToString()}")
    println("Commands/profile: ${TvProfiles.all.joinToString { "${it.id}=${it.commands.size}" }}")
    println("Panasonic baseline: ${pp.size} timings, ${pp.sum()} us")
    println("All protocol checks passed")
}
