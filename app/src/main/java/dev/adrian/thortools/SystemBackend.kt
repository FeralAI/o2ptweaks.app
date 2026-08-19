package dev.adrian.thortools

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.adrian.thortools.utils.MagiskUtil
import dev.adrian.thortools.utils.PatchUtils
import dev.adrian.thortools.utils.RootUtils
import dev.adrian.thortools.utils.SystemUtils
import dev.adrian.thortools.utils.FileUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ThorOperation {
    REFRESH,
    INSTALL_MAGISK,
    BACKUP,
    PATCH,
    FLASH,
    RESTORE,
    CLEAR_CACHE,
    REBOOT,
    SET_DPI,
    SET_ANIMATION,
    SET_VOLUME_STEPS,
    SET_BOOT_ANIMATION,
}

enum class OperationStatus {
    IDLE,
    RUNNING,
    SUCCESS,
    FAILURE,
    INTERRUPTED,
}

data class OperationState(
    val operation: ThorOperation? = null,
    val status: OperationStatus = OperationStatus.IDLE,
    val message: String = "Ready",
)

data class ThorSnapshot(
    val profile: ThorDeviceProfile,
    val batteryPercent: Int,
    val lcdDensity: Int,
    val volumeSteps: Int,
    val animationSpeed: Float,
    val activeSlot: String,
    val kernelVersion: String,
    val rootServiceAvailable: Boolean,
    val rooted: Boolean,
    val magiskInstalled: Boolean,
    val initBootAvailable: Boolean,
    val bootAvailable: Boolean,
    val backupDestinationWritable: Boolean,
    val backupAvailable: Boolean,
    val patchedBackupAvailable: Boolean,
    val operation: OperationState,
) {
    val capabilityRows: List<Pair<String, Boolean>>
        get() = listOf(
            "Thor device" to profile.isThor,
            "Root service" to profile.supports(ThorCapability.ROOT_SERVICE),
            "Root access" to profile.supports(ThorCapability.ROOTED),
            "Magisk" to profile.supports(ThorCapability.MAGISK),
            "Active slot" to profile.supports(ThorCapability.ACTIVE_SLOT),
            "init_boot partition" to profile.supports(ThorCapability.INIT_BOOT_PARTITION),
            "boot partition" to profile.supports(ThorCapability.BOOT_PARTITION),
            "Battery state" to profile.supports(ThorCapability.BATTERY_STATE),
            "Backup destination" to profile.supports(ThorCapability.BACKUP_DESTINATION),
        )
}

data class OperationResult(
    val success: Boolean,
    val message: String,
)

interface SystemBackend {
    fun snapshot(operation: OperationState = OperationState()): ThorSnapshot
    fun perform(operation: ThorOperation, argument: String? = null): OperationResult
}

class RealSystemBackend(private val context: Context) : SystemBackend {
    override fun snapshot(operation: OperationState): ThorSnapshot {
        val properties = SystemUtils.getDeviceProperties()
        val rootService = RootUtils.hasPServer()
        val rooted = RootUtils.isDeviceRooted
        val magisk = MagiskUtil.hasMagiskPackage(context)
        val initBoot = rootService && RootUtils.checkFileExistsRoot(context, "/dev/block/by-name/init_boot${properties.slot}")
        val boot = rootService && RootUtils.checkFileExistsRoot(context, "/dev/block/by-name/boot${properties.slot}")
        val backupDestination = FileUtils.isBackupDestinationWritable(context)
        val capabilities = buildSet {
            if (rootService) add(ThorCapability.ROOT_SERVICE)
            if (rooted) add(ThorCapability.ROOTED)
            if (magisk) add(ThorCapability.MAGISK)
            if (properties.slot == "_a" || properties.slot == "_b") add(ThorCapability.ACTIVE_SLOT)
            if (initBoot) add(ThorCapability.INIT_BOOT_PARTITION)
            if (boot) add(ThorCapability.BOOT_PARTITION)
            if (SystemUtils.getBatteryPercent(context) != null) add(ThorCapability.BATTERY_STATE)
            if (backupDestination) add(ThorCapability.BACKUP_DESTINATION)
        }
        return ThorSnapshot(
            profile = DeviceProfile.detect(properties).copy(capabilities = capabilities),
            batteryPercent = SystemUtils.getBatteryPercent(context) ?: 0,
            lcdDensity = SystemUtils.getPropLcdDensity(),
            volumeSteps = SystemUtils.getPropVolumeSteps(),
            animationSpeed = AppSettings.getAnimationSpeed(AppSettings.getSharedPrefs(context)),
            activeSlot = properties.slot.ifBlank { "unknown" },
            kernelVersion = SystemUtils.getKernelVersion(context),
            rootServiceAvailable = rootService,
            rooted = rooted,
            magiskInstalled = magisk,
            initBootAvailable = initBoot,
            bootAvailable = boot,
            backupDestinationWritable = backupDestination,
            backupAvailable = PatchUtils.checkBootBackupExists(context),
            patchedBackupAvailable = PatchUtils.checkBootMagiskExists(context),
            operation = operation,
        )
    }

    override fun perform(operation: ThorOperation, argument: String?): OperationResult {
        val current = snapshot()
        val mutating = operation != ThorOperation.REFRESH && operation != ThorOperation.INSTALL_MAGISK
        if (mutating && !current.profile.isThor) return OperationResult(false, "Only an AYN Thor can be modified")
        if (operation in setOf(ThorOperation.BACKUP, ThorOperation.PATCH, ThorOperation.FLASH, ThorOperation.RESTORE) && !current.rootServiceAvailable) {
            return OperationResult(false, "The Thor privileged root service is unavailable")
        }
        if (operation in setOf(ThorOperation.BACKUP, ThorOperation.PATCH, ThorOperation.FLASH, ThorOperation.RESTORE) && current.batteryPercent < 35) {
            return OperationResult(false, "Charge the Thor to at least 35% before image operations")
        }
        if (operation in setOf(ThorOperation.BACKUP, ThorOperation.PATCH, ThorOperation.FLASH, ThorOperation.RESTORE) && current.activeSlot !in setOf("_a", "_b")) {
            return OperationResult(false, "The active Thor slot could not be determined")
        }
        if (operation in setOf(ThorOperation.BACKUP, ThorOperation.PATCH, ThorOperation.FLASH, ThorOperation.RESTORE) && !current.backupDestinationWritable) {
            return OperationResult(false, "The Thor backup destination is not writable")
        }
        if (operation == ThorOperation.BACKUP && !current.initBootAvailable && !current.bootAvailable) {
            return OperationResult(false, "No supported Thor boot partition was found")
        }
        return when (operation) {
            ThorOperation.REFRESH -> OperationResult(true, "System state refreshed")
            ThorOperation.INSTALL_MAGISK -> OperationResult(
                MagiskUtil.enqueueLatestDownload(context),
                "Magisk download started in the Download folder",
            )
            ThorOperation.BACKUP -> if (PatchUtils.backupBoot(context)) {
                OperationResult(true, "Both available boot partitions were backed up")
            } else {
                OperationResult(false, "No readable Thor boot partitions were backed up")
            }
            ThorOperation.PATCH -> if (!current.magiskInstalled || !current.backupAvailable) {
                OperationResult(false, "Install Magisk and create a stock backup before patching")
            } else {
                val patched = PatchUtils.patchBoot(context)
                if (patched.isNotBlank()) OperationResult(true, "Created $patched")
                else OperationResult(false, "Magisk could not patch a backed-up partition")
            }
            ThorOperation.FLASH -> if (!current.magiskInstalled || !current.patchedBackupAvailable) {
                OperationResult(false, "A Magisk-patched active-slot image is required")
            } else if (PatchUtils.flashBoot(context)) {
                OperationResult(true, "Root patch flashed; reboot required")
            } else {
                OperationResult(false, "No patched active-slot image is available")
            }
            ThorOperation.RESTORE -> if (!current.backupAvailable) {
                OperationResult(false, "A stock active-slot backup is required")
            } else if (PatchUtils.restoreBoot(context)) {
                OperationResult(true, "Stock image restored; reboot required")
            } else {
                OperationResult(false, "No stock active-slot image is available")
            }
            ThorOperation.CLEAR_CACHE -> {
                PatchUtils.clearBootCache(context)
                OperationResult(true, "Cached images cleared")
            }
            ThorOperation.REBOOT -> {
                RootUtils.reboot(context)
                OperationResult(true, "Reboot requested")
            }
            ThorOperation.SET_DPI -> {
                val value = argument?.toIntOrNull() ?: return OperationResult(false, "Invalid DPI")
                RootUtils.setDpi(context, value)
                OperationResult(true, "DPI set to $value")
            }
            ThorOperation.SET_ANIMATION -> {
                val value = argument?.toFloatOrNull() ?: return OperationResult(false, "Invalid animation speed")
                RootUtils.setAnimationSpeed(context, value)
                OperationResult(true, "Animation speed set to ${value}x")
            }
            ThorOperation.SET_VOLUME_STEPS -> {
                val value = argument?.toIntOrNull() ?: return OperationResult(false, "Invalid volume step count")
                AppSettings.setVolumeSteps(AppSettings.getSharedPrefs(context), value)
                AppSettings.save(context)
                OperationResult(true, "Volume steps set to $value; reboot required")
            }
            ThorOperation.SET_BOOT_ANIMATION -> {
                val enabled = argument == "true"
                AppSettings.setSkipBootAnimation(AppSettings.getSharedPrefs(context), enabled)
                AppSettings.save(context)
                OperationResult(true, if (enabled) "Boot animation disabled" else "Boot animation enabled")
            }
        }
    }
}

class ThorSession(
    private val context: Context,
    private val backend: SystemBackend = SystemBackendFactory.create(context),
) {
    var snapshot by mutableStateOf(backend.snapshot(operationFromJournal()))
        private set

    private fun operationFromJournal(): OperationState {
        val prefs = AppSettings.getSharedPrefs(context)
        val name = prefs.getString(AppSettings.JOURNAL_OPERATION_KEY, null) ?: return OperationState()
        val message = prefs.getString(AppSettings.JOURNAL_MESSAGE_KEY, "Interrupted") ?: "Interrupted"
        return OperationState(
            operation = runCatching { ThorOperation.valueOf(name) }.getOrNull(),
            status = OperationStatus.INTERRUPTED,
            message = "Previous operation stopped before completion: $message",
        )
    }

    fun refresh() {
        snapshot = backend.snapshot(snapshot.operation.copy(status = OperationStatus.IDLE, message = "Ready"))
    }

    fun run(scope: CoroutineScope, operation: ThorOperation, argument: String? = null) {
        if (snapshot.operation.status == OperationStatus.RUNNING) return
        val running = OperationState(operation, OperationStatus.RUNNING, "${operation.name.lowercase().replace('_', ' ')} in progress")
        persistJournal(running)
        snapshot = backend.snapshot(running)
        scope.launch {
            val result = withContext(Dispatchers.IO) { backend.perform(operation, argument) }
            val finished = OperationState(
                operation,
                if (result.success) OperationStatus.SUCCESS else OperationStatus.FAILURE,
                result.message,
            )
            clearJournal()
            snapshot = backend.snapshot(finished)
        }
    }

    private fun persistJournal(state: OperationState) {
        AppSettings.getSharedPrefs(context).edit()
            .putString(AppSettings.JOURNAL_OPERATION_KEY, state.operation?.name)
            .putString(AppSettings.JOURNAL_MESSAGE_KEY, state.message)
            .apply()
    }

    private fun clearJournal() {
        AppSettings.getSharedPrefs(context).edit()
            .remove(AppSettings.JOURNAL_OPERATION_KEY)
            .remove(AppSettings.JOURNAL_MESSAGE_KEY)
            .apply()
    }
}
