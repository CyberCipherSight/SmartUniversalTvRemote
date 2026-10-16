package com.cybersight.panasonicremote.tv

/**
 * User-facing family hints covering roughly the last decade. These are not an
 * exhaustive list of every size/region SKU; selecting one chooses the most
 * likely IR family and Find My TV can verify it on the physical set.
 */
object ModelCatalog {
    const val AUTO_BRAND = "I don't know (Auto)"

    val brands = listOf(AUTO_BRAND, "Samsung", "LG", "Xiaomi", "TCL", "Panasonic")

    private val samsung = listOf(
        TvSeriesHint("2016 · K / KU Series", TvProfiles.SAMSUNG_STANDARD),
        TvSeriesHint("2017 · M / MU / QLED Q7-Q9", TvProfiles.SAMSUNG_STANDARD),
        TvSeriesHint("2018 · N / NU / QLED", TvProfiles.SAMSUNG_STANDARD),
        TvSeriesHint("2019 · R / RU / QLED", TvProfiles.SAMSUNG_STANDARD),
        TvSeriesHint("2020 · T / TU / QLED", TvProfiles.SAMSUNG_STANDARD),
        TvSeriesHint("2021 · A / AU / Neo QLED", TvProfiles.SAMSUNG_STANDARD),
        TvSeriesHint("2022 · B / BU / Neo QLED", TvProfiles.SAMSUNG_STANDARD),
        TvSeriesHint("2023 · C / CU / OLED / Neo QLED", TvProfiles.SAMSUNG_STANDARD),
        TvSeriesHint("2024 · D / DU / OLED / Neo QLED", TvProfiles.SAMSUNG_STANDARD),
        TvSeriesHint("2025-26 · U/F-era Crystal UHD / QLED / Neo QLED", TvProfiles.SAMSUNG_STANDARD)
    )

    private val lg = listOf(
        TvSeriesHint("2016 · UH / OLED 6-series", TvProfiles.LG_STANDARD),
        TvSeriesHint("2017 · UJ / OLED 7-series", TvProfiles.LG_STANDARD),
        TvSeriesHint("2018 · UK / SK / OLED 8-series", TvProfiles.LG_STANDARD),
        TvSeriesHint("2019 · UM / SM / OLED 9-series", TvProfiles.LG_STANDARD),
        TvSeriesHint("2020 · UN / NanoCell / OLED X", TvProfiles.LG_STANDARD),
        TvSeriesHint("2021 · UP / NanoCell / OLED 1-series", TvProfiles.LG_STANDARD),
        TvSeriesHint("2022 · UQ / QNED / OLED B2-C2-G2", TvProfiles.LG_STANDARD),
        TvSeriesHint("2023 · UR / QNED / OLED B3-C3-G3", TvProfiles.LG_STANDARD),
        TvSeriesHint("2024 · UT / QNED / OLED B4-C4-G4", TvProfiles.LG_STANDARD),
        TvSeriesHint("2025-26 · QNED evo / OLED B5-C5-G5", TvProfiles.LG_STANDARD)
    )

    private val xiaomi = listOf(
        TvSeriesHint("2018-20 · Mi TV 4A / 4A Pro", TvProfiles.XIAOMI_MI_TV_80),
        TvSeriesHint("2019-21 · Mi TV 4X / Horizon Edition", TvProfiles.XIAOMI_MI_TV_80),
        TvSeriesHint("2020-21 · Mi TV 4S (global variants)", TvProfiles.XIAOMI_4S_ALT),
        TvSeriesHint("2021 · Mi TV Q1 / 5X", TvProfiles.XIAOMI_MI_TV_80),
        TvSeriesHint("2022 · Xiaomi Smart TV 5A / X Series", TvProfiles.XIAOMI_MI_TV_80),
        TvSeriesHint("2023 · Xiaomi X Pro / A Series", TvProfiles.XIAOMI_MI_TV_80),
        TvSeriesHint("2024 · X / X Pro / A / Redmi Fire TV families", TvProfiles.XIAOMI_MI_TV_80),
        TvSeriesHint("2025-26 · F / FX / A Pro / X Pro QLED families", TvProfiles.XIAOMI_MI_TV_80,
            "Some newer Bluetooth/Google-TV functions may not exist over IR.")
    )

    private val tcl = listOf(
        TvSeriesHint("2017-19 · S/P6/P8 families", TvProfiles.TCL_NEC_EXT),
        TvSeriesHint("2020 · P715 / C715", TvProfiles.TCL_NIKAI),
        TvSeriesHint("2021 · P725 / C725", TvProfiles.TCL_NIKAI),
        TvSeriesHint("2022 · P735 / C635", TvProfiles.TCL_NIKAI),
        TvSeriesHint("2023 · P745 / C645", TvProfiles.TCL_NIKAI),
        TvSeriesHint("2024 · P755 / C655", TvProfiles.TCL_NIKAI),
        TvSeriesHint("2025-26 · P6K / C6K / C8K-era models", TvProfiles.TCL_NIKAI,
            "Use Find My TV because handset families vary by region.")
    )

    private val panasonic = listOf(
        TvSeriesHint("2016-26 · Panasonic Viera / common IR handset family", TvProfiles.PANASONIC_VIERA,
            "This profile is physically verified in this project.")
    )

    fun seriesFor(brand: String): List<TvSeriesHint> = when (brand) {
        "Samsung" -> samsung
        "LG" -> lg
        "Xiaomi" -> xiaomi
        "TCL" -> tcl
        "Panasonic" -> panasonic
        else -> emptyList()
    }
}
