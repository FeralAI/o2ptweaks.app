package dev.adrian.thortools

enum class ThorVariant {
    LITE,
    BASE,
    PRO,
    MAX,
    UNKNOWN,
}

enum class ThorCapability {
    ROOT_SERVICE,
    ROOTED,
    MAGISK,
    ACTIVE_SLOT,
    INIT_BOOT_PARTITION,
    BOOT_PARTITION,
    BATTERY_STATE,
    BACKUP_DESTINATION,
}

data class DeviceProperties(
    val manufacturer: String = "",
    val model: String = "",
    val device: String = "",
    val product: String = "",
    val platform: String = "",
    val firmware: String = "",
    val buildId: String = "",
    val buildDisplayId: String = "",
    val buildDate: String = "",
    val serial: String = "",
    val slot: String = "",
)

data class ThorDeviceProfile(
    val properties: DeviceProperties,
    val isThor: Boolean,
    val variant: ThorVariant,
    val capabilities: Set<ThorCapability> = emptySet(),
) {
    val displayName: String
        get() = when {
            !isThor -> properties.model.ifBlank { "Unknown device" }
            variant == ThorVariant.UNKNOWN -> "AYN Thor"
            else -> "AYN Thor ${variant.name.lowercase().replaceFirstChar { it.uppercase() }}"
        }

    fun supports(capability: ThorCapability): Boolean = capability in capabilities
}

object DeviceProfile {
    const val THOR_MODEL = "AYN Thor"
    const val UPPER_WIDTH_PIXELS = 1920
    const val UPPER_HEIGHT_PIXELS = 1080
    const val LOWER_WIDTH_PIXELS = 1240
    const val LOWER_HEIGHT_PIXELS = 1080
    const val UPPER_DIAGONAL_MILLIMETRES = 152.4f
    const val LOWER_DIAGONAL_MILLIMETRES = 99.6f
    const val LOWER_MINIMUM_TEXT_SP = 18f

    private val thorPattern = Regex("(^|\\s)(ayn\\s*)?thor(?:\\s*(lite|base|pro|max))?(\\s|$)")

    fun detect(properties: DeviceProperties): ThorDeviceProfile {
        val searchable = listOf(
            properties.manufacturer,
            properties.model,
            properties.device,
            properties.product,
            properties.platform,
        ).joinToString(" ").normalized()
        val isThor = thorPattern.containsMatchIn(searchable)
        return ThorDeviceProfile(properties, isThor, variantFor(searchable))
    }

    fun variantFor(value: String): ThorVariant {
        val normalized = value.normalized()
        if (!thorPattern.containsMatchIn(normalized)) return ThorVariant.UNKNOWN
        return when {
            normalized.contains("lite") -> ThorVariant.LITE
            normalized.contains("max") -> ThorVariant.MAX
            normalized.contains("pro") -> ThorVariant.PRO
            normalized.contains("thor") -> ThorVariant.BASE
            else -> ThorVariant.UNKNOWN
        }
    }

    private fun String.normalized(): String = lowercase().replace('_', ' ').replace('-', ' ')

    fun isThorLowerDisplay(widthPixels: Int, heightPixels: Int): Boolean =
        widthPixels == LOWER_WIDTH_PIXELS && heightPixels == LOWER_HEIGHT_PIXELS

    fun displayKind(widthPixels: Int, heightPixels: Int): String = if (
        widthPixels <= 1400 && heightPixels >= 900 && widthPixels.toFloat() / heightPixels < 1.4f
    ) {
        "lower"
    } else {
        "upper"
    }

    fun minimumReadablePixels(
        widthPixels: Int,
        heightPixels: Int,
        scaledDensity: Float = 1f,
        minimumTextSp: Float = LOWER_MINIMUM_TEXT_SP,
    ): Float = if (displayKind(widthPixels, heightPixels) == "lower") {
        maxOf(32f, minimumTextSp * scaledDensity)
    } else {
        24f
    }
}
