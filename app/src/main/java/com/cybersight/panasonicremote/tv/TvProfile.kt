package com.cybersight.panasonicremote.tv

import com.cybersight.panasonicremote.ir.IrSignal

data class TvProfile(
    val id: String,
    val brand: String,
    val displayName: String,
    val familyHint: String,
    val commands: Map<String, IrSignal>,
    val notes: String = ""
) {
    fun signal(command: String): IrSignal? = commands[command]
    fun supports(command: String): Boolean = commands.containsKey(command)
}

data class TvSeriesHint(
    val label: String,
    val profileId: String,
    val note: String = ""
)
