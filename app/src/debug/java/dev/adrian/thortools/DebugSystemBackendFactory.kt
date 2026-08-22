package dev.adrian.thortools

import android.content.Context
import java.io.File

object DebugSystemBackendFactory {
    @JvmStatic
    fun create(context: Context): SystemBackend = FakeSystemBackend(context)
}

private class FakeSystemBackend(private val context: Context) : SystemBackend {
    private var state = ThorSnapshot(
        profile = DeviceProfile.detect(
            DeviceProperties(
                manufacturer = "AYN",
                model = "AYN Thor Pro",
                device = "thor_pro",
                product = "thor",
                platform = "sm8650",
                firmware = "Android 13 / ThorTools AVD",
                buildId = "THOR_DEBUG",
                buildDisplayId = "ThorTools AVD",
                buildDate = "2026-08-19",
                buildFingerprint = "AYN/Thor/ThorTools:13/THOR_DEBUG/10001:userdebug/test-keys",
                serial = "thor-avd",
                slot = "_a",
            ),
        ).copy(
            capabilities = setOf(
                ThorCapability.ROOT_SERVICE,
                ThorCapability.MAGISK,
                ThorCapability.ACTIVE_SLOT,
                ThorCapability.INIT_BOOT_PARTITION,
                ThorCapability.BOOT_PARTITION,
                ThorCapability.BATTERY_STATE,
                ThorCapability.BACKUP_DESTINATION,
            ),
        ),
        batteryPercent = 88,
        lcdDensity = 320,
        volumeSteps = 20,
        animationSpeed = 1f,
        activeSlot = "_a",
        kernelVersion = "ThorTools deterministic emulator backend",
        rootServiceAvailable = true,
        rooted = false,
        magiskInstalled = true,
        initBootAvailable = true,
        bootAvailable = true,
        backupDestinationWritable = true,
        backupAvailable = false,
        stockRestoreAvailable = false,
        patchedBackupAvailable = false,
        stockBackupSlots = emptySet(),
        patchedBackupSlots = emptySet(),
        operation = OperationState(),
    )

    override fun snapshot(operation: OperationState): ThorSnapshot = state.copy(operation = operation)

    override fun perform(operation: ThorOperation, argument: String?): OperationResult {
        ThorOperationGuard.validate(state, operation)?.let { return OperationResult(false, it) }
        when (operation) {
            ThorOperation.BACKUP -> {
                writeImages("boot", "init_boot")
                state = state.copy(
                    backupAvailable = true,
                    stockRestoreAvailable = true,
                    stockBackupSlots = setOf("_a", "_b"),
                )
            }
            ThorOperation.PATCH -> {
                writeImages("boot_patched", "init_boot_patched")
                state = state.copy(
                    patchedBackupAvailable = true,
                    patchedBackupSlots = setOf("_a", "_b"),
                )
            }
            ThorOperation.FLASH -> state = state.copy(
                rooted = true,
                profile = state.profile.copy(capabilities = state.profile.capabilities + ThorCapability.ROOTED),
            )
            ThorOperation.RESTORE -> state = state.copy(
                rooted = false,
                profile = state.profile.copy(capabilities = state.profile.capabilities - ThorCapability.ROOTED),
            )
            ThorOperation.CLEAR_CACHE -> {
                listOf("boot_patched", "init_boot_patched").forEach { prefix ->
                    listOf("_a", "_b").forEach { slot ->
                        File(context.getExternalFilesDir(null), "$prefix$slot.img").delete()
                    }
                }
                state = state.copy(
                    patchedBackupAvailable = false,
                    patchedBackupSlots = emptySet(),
                )
            }
            ThorOperation.SET_DPI -> state = state.copy(lcdDensity = argument?.toIntOrNull() ?: state.lcdDensity)
            ThorOperation.SET_ANIMATION -> state = state.copy(animationSpeed = argument?.toFloatOrNull() ?: state.animationSpeed)
            ThorOperation.SET_VOLUME_STEPS,
            ThorOperation.SET_BOOT_ANIMATION,
            ThorOperation.INSTALL_MAGISK,
            ThorOperation.REBOOT,
            ThorOperation.REFRESH,
            -> Unit
        }
        return OperationResult(true, "Debug backend completed ${operation.name.lowercase().replace('_', ' ')}")
    }

    private fun writeImages(vararg prefixes: String) {
        val directory = context.getExternalFilesDir(null) ?: return
        directory.mkdirs()
        prefixes.forEach { prefix ->
            listOf("_a", "_b").forEach { slot ->
                File(directory, "$prefix$slot.img").writeText("ThorTools debug image $prefix$slot")
            }
        }
    }
}
