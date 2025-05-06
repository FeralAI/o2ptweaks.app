package com.feralai.o2ptweaks

object SupportedDevices {
    const val AYN_ODIN2_PORTAL: String = "Odin2 Portal"
    const val RETROID_POCKET_MINI: String = "Retroid Pocket Mini"
    const val RETROID_POCKET_MINI_V2: String = "Retroid Pocket Mini V2"

    val loweredSupportedDevices: List<String> = listOf(
        AYN_ODIN2_PORTAL.lowercase(),
//        RETROID_POCKET_MINI.lowercase(),
//        RETROID_POCKET_MINI_V2.lowercase(),
    )

    fun isSupportedDevice(device: String): Boolean {
        return loweredSupportedDevices.indexOf(device.lowercase()) == -1
    }
}