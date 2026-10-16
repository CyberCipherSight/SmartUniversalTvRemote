package com.cybersight.panasonicremote.ir

/**
 * Panasonic Viera command words captured from a Panasonic TH-42PZ700A remote.
 * The full 48-bit IR frame is 0x4004 (16-bit prefix) + one 32-bit command word.
 */
object PanasonicCodes {
    val commands: Map<String, Long> = linkedMapOf(
        "POWER" to 0x0100BCBDL,
        "TV" to 0x01400C4DL,
        "INPUT" to 0x0100A0A1L,
        "ASPECT" to 0x01207B5AL,
        "OPTION" to 0x0190E574L,
        "GUIDE" to 0x0190E170L,
        "EXIT" to 0x0100CBCAL,
        "LEFT" to 0x01007273L,
        "UP" to 0x01005253L,
        "RIGHT" to 0x0100F2F3L,
        "DOWN" to 0x0100D2D3L,
        "OK" to 0x01009293L,
        "MENU" to 0x01004A4BL,
        "BACK" to 0x01002B2AL,
        "RED" to 0x01000E0FL,
        "GREEN" to 0x01008E8FL,
        "YELLOW" to 0x01004E4FL,
        "BLUE" to 0x0100CECFL,
        "1" to 0x01000809L,
        "2" to 0x01008889L,
        "3" to 0x01004849L,
        "4" to 0x0100C8C9L,
        "5" to 0x01002829L,
        "6" to 0x0100A8A9L,
        "7" to 0x01006869L,
        "8" to 0x0100E8E9L,
        "9" to 0x01001819L,
        "0" to 0x01009899L,
        "INFO" to 0x01009C9DL,
        "MUTE" to 0x01004C4DL,
        "CH_UP" to 0x01002C2DL,
        "CH_DOWN" to 0x0100ACADL,
        "VOL_UP" to 0x01000405L,
        "VOL_DOWN" to 0x01008485L,
        "POWER_ON" to 0x01007C7DL,
        "POWER_OFF" to 0x0100FCFDL,
        "HDMI_1" to 0x01200D2CL,
        "HDMI_2" to 0x01208DACL,
        "HDMI_3" to 0x01204D6CL
    )

    fun get(name: String): Long? = commands[name]

    fun all(): Map<String, Long> = commands.toMap()
}
