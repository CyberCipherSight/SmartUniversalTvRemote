package com.cybersight.panasonicremote.tv

import com.cybersight.panasonicremote.ir.IrSignal
import com.cybersight.panasonicremote.ir.PanasonicCodes

/**
 * Curated IR families, not one entry per screen-size SKU.
 * TVs in a model family commonly reuse the same handset/protocol.
 */
object TvProfiles {
    const val PANASONIC_VIERA = "panasonic_viera"
    const val SAMSUNG_STANDARD = "samsung_standard"
    const val LG_STANDARD = "lg_standard"
    const val XIAOMI_MI_TV_80 = "xiaomi_mitv_80"
    const val XIAOMI_4S_ALT = "xiaomi_4s_alt"
    const val TCL_NIKAI = "tcl_nikai"
    const val TCL_NEC_EXT = "tcl_nec_ext"

    val all: List<TvProfile> = listOf(
        samsungStandard(),
        lgStandard(),
        xiaomiMiTv80(),
        xiaomi4sAlternate(),
        tclNikai(),
        tclNecExtended(),
        panasonicViera()
    )

    fun byId(id: String?): TvProfile? = all.firstOrNull { it.id == id }

    fun forBrand(brand: String): List<TvProfile> =
        if (brand == ModelCatalog.AUTO_BRAND) all else all.filter { it.brand == brand }

    private fun samsungStandard(): TvProfile {
        fun s(cmd: Int) = IrSignal.Samsung(address = 0x07, command = cmd)
        return TvProfile(
            id = SAMSUNG_STANDARD,
            brand = "Samsung",
            displayName = "Samsung Standard TV",
            familyHint = "K/KU → MU/NU/RU/TU/AU/BU/CU/DU and many QLED/Neo QLED IR functions",
            commands = mapOf(
                "POWER" to s(0x02), "INPUT" to s(0x01),
                "1" to s(0x04), "2" to s(0x05), "3" to s(0x06),
                "4" to s(0x08), "5" to s(0x09), "6" to s(0x0A),
                "7" to s(0x0C), "8" to s(0x0D), "9" to s(0x0E), "0" to s(0x11),
                "VOL_UP" to s(0x07), "VOL_DOWN" to s(0x0B), "MUTE" to s(0x0F),
                "CH_UP" to s(0x12), "CH_DOWN" to s(0x10),
                "MENU" to s(0x1A), "GUIDE" to s(0x4F), "INFO" to s(0x1F), "HOME" to s(0x79),
                "UP" to s(0x60), "DOWN" to s(0x61), "LEFT" to s(0x65), "RIGHT" to s(0x62),
                "OK" to s(0x68), "BACK" to s(0x58), "EXIT" to s(0x2D),
                "RED" to s(0x6C), "GREEN" to s(0x14), "YELLOW" to s(0x15), "BLUE" to s(0x16),
                "PLAY" to s(0x47)
            ),
            notes = "Samsung32 38 kHz family. Some newer Smart Remotes use Bluetooth for advanced functions."
        )
    }

    private fun lgStandard(): TvProfile {
        fun n(cmd: Int) = IrSignal.Nec(address = 0x04, command = cmd)
        return TvProfile(
            id = LG_STANDARD,
            brand = "LG",
            displayName = "LG TV Standard",
            familyHint = "UH/UJ/UK/UM/UN/UP/UQ/UR/UT plus many OLED & QNED models",
            commands = mapOf(
                "POWER" to n(0x08), "INPUT" to n(0x0B),
                "1" to n(0x10), "2" to n(0x11), "3" to n(0x12),
                "4" to n(0x13), "5" to n(0x14), "6" to n(0x15),
                "7" to n(0x16), "8" to n(0x17), "9" to n(0x18), "0" to n(0x19),
                "VOL_UP" to n(0x02), "VOL_DOWN" to n(0x03), "MUTE" to n(0x09),
                "CH_UP" to n(0x00), "CH_DOWN" to n(0x01),
                "SETTINGS" to n(0x43), "EXIT" to n(0x5B),
                "UP" to n(0x40), "DOWN" to n(0x41), "LEFT" to n(0x07), "RIGHT" to n(0x06),
                "OK" to n(0x44), "BACK" to n(0x28),
                "HDMI_1" to n(0xCE), "HDMI_2" to n(0xCC), "HDMI_3" to n(0xE9)
            ),
            notes = "NEC 38 kHz family used across many LG televisions; Magic Remote pointer/voice remains RF/Bluetooth."
        )
    }

    private fun xiaomiMiTv80(): TvProfile {
        fun n(cmd: Int) = IrSignal.Nec(address = 0x80, command = cmd)
        return TvProfile(
            id = XIAOMI_MI_TV_80,
            brand = "Xiaomi",
            displayName = "Xiaomi / Mi TV IR 0x80",
            familyHint = "Mi TV 4A/4X/Horizon and candidate for many X/A/X Pro families",
            commands = mapOf(
                "POWER" to n(0x46), "MUTE" to n(0x5A),
                "1" to n(0x49), "2" to n(0x55), "3" to n(0x59),
                "4" to n(0x4D), "5" to n(0x51), "6" to n(0x5D),
                "7" to n(0x48), "8" to n(0x54), "9" to n(0x58), "0" to n(0x50),
                "VOL_UP" to n(0x16), "VOL_DOWN" to n(0x15),
                "CH_UP" to n(0x1F), "CH_DOWN" to n(0x19),
                "MENU" to n(0x07), "HOME" to n(0x17), "SETTINGS" to n(0x45),
                "UP" to n(0x52), "DOWN" to n(0x13), "LEFT" to n(0x06), "RIGHT" to n(0x1A),
                "OK" to n(0x0F), "BACK" to n(0x1B), "PLAY" to n(0x01), "PAUSE" to n(0x1D),
                "RED" to n(0x47), "GREEN" to n(0x4B), "YELLOW" to n(0x57), "BLUE" to n(0x5B)
            ),
            notes = "IR command family captured for Mi TV 4A-class sets. New Google/Android TV remotes may use Bluetooth for some keys."
        )
    }

    private fun xiaomi4sAlternate(): TvProfile = TvProfile(
        id = XIAOMI_4S_ALT,
        brand = "Xiaomi",
        displayName = "Xiaomi Mi TV 4S Alternate",
        familyHint = "Mi TV 4S / selected global variants",
        commands = mapOf(
            "POWER" to IrSignal.Nec(address = 0x3C, command = 0xCC),
            "VOL_UP" to IrSignal.Nec(address = 0x86, command = 0x0E),
            "VOL_DOWN" to IrSignal.Nec(address = 0x86, command = 0x0F)
        ),
        notes = "Partial alternate profile. The UI intentionally hides functions for which we do not have verified IR data."
    )

    private fun tclNikai(): TvProfile {
        fun k(data: Int) = IrSignal.Nikai(data)
        return TvProfile(
            id = TCL_NIKAI,
            brand = "TCL",
            displayName = "TCL Android TV / NIKAI",
            familyHint = "P715/P725 and candidate for several P/C-series Android TVs",
            commands = mapOf(
                "POWER" to k(0x0D5F2A), "MUTE" to k(0x0C0F3F), "INPUT" to k(0x05CFA3),
                "1" to k(0x0CEF31), "2" to k(0x0CDF32), "3" to k(0x0CCF33),
                "4" to k(0x0CBF34), "5" to k(0x0CAF35), "6" to k(0x0C9F36),
                "7" to k(0x0C8F37), "8" to k(0x0C7F38), "9" to k(0x0C6F39), "0" to k(0x0CFF30),
                "VOL_UP" to k(0x0D0F2F), "VOL_DOWN" to k(0x0D1F2E),
                "CH_UP" to k(0x0D2F2D), "CH_DOWN" to k(0x0D3F2C),
                "MENU" to k(0x013FEC), "SETTINGS" to k(0x030FCF), "HOME" to k(0x0F7F08),
                "INFO" to k(0x0C3F3C), "GUIDE" to k(0x0E5F1A),
                "UP" to k(0x0A6F59), "DOWN" to k(0x0A7F58), "LEFT" to k(0x0A9F56), "RIGHT" to k(0x0A8F57),
                "OK" to k(0x00BFF4), "BACK" to k(0x0D8F27),
                "RED" to k(0x0FFF00), "GREEN" to k(0x017FE8), "YELLOW" to k(0x01BFE4), "BLUE" to k(0x027FD8),
                "NETFLIX" to k(0x010FEF), "PRIME" to k(0x03EFC1), "YOUTUBE" to k(0x01DFE2)
            ),
            notes = "NIKAI 24-bit 38 kHz profile seen on TCL P715-class remotes."
        )
    }

    private fun tclNecExtended(): TvProfile {
        fun n(cmd: Int) = IrSignal.NecExtended(addressLow = 0xEA, addressHigh = 0xC7, command = cmd)
        return TvProfile(
            id = TCL_NEC_EXT,
            brand = "TCL",
            displayName = "TCL Roku / NEC Extended",
            familyHint = "32S327-class / selected Roku-oriented TCL televisions",
            commands = mapOf(
                "POWER" to n(0x17), "VOL_UP" to n(0x0F), "VOL_DOWN" to n(0x10),
                "UP" to n(0x19), "DOWN" to n(0x33), "LEFT" to n(0x1E), "RIGHT" to n(0x2D),
                "OK" to n(0x2A), "HOME" to n(0x03), "EXIT" to n(0x66)
            ),
            notes = "Alternative TCL NEC-extended family; limited to captured keys."
        )
    }

    private fun panasonicViera(): TvProfile {
        val map = PanasonicCodes.all().mapValues { (_, value) -> IrSignal.Panasonic(value) }
        return TvProfile(
            id = PANASONIC_VIERA,
            brand = "Panasonic",
            displayName = "Panasonic Viera (verified)",
            familyHint = "Viera family; physically verified on the user's Panasonic TV",
            commands = map,
            notes = "Known-good 37 kHz, 48-bit Panasonic profile retained unchanged from v0.1.1."
        )
    }
}
