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
import kotlinx.coroutines.CancellationException
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

private val ThorOperation.requiresThor: Boolean
    get() = this != ThorOperation.REFRESH

private val ThorOperation.requiresRootService: Boolean
    get() = this !in setOf(ThorOperation.REFRESH, ThorOperation.INSTALL_MAGISK)

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
    val stockBackupSlots: Set<String> = emptySet(),
    val patchedBackupSlots: Set<String> = emptySet(),
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

    companion object {
        fun loading(operation: OperationState): ThorSnapshot = ThorSnapshot(
            profile = DeviceProfile.detect(DeviceProperties()),
            batteryPercent = 0,
            lcdDensity = 0,
            volumeSteps = AppSettings.VOLUME_STEPS_DEFAULT,
            animationSpeed = AppSettings.ANIMATION_SPEED_DEFAULT,
            activeSlot = "unknown",
            kernelVersion = "",
            rootServiceAvailable = false,
            rooted = false,
            magiskInstalled = false,
            initBootAvailable = false,
            bootAvailable = false,
            backupDestinationWritable = false,
            backupAvailable = false,
            patchedBackupAvailable = false,
            operation = operation,
        )
    }
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
        val battery = SystemUtils.getBatteryPercent(context)
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
            if (battery != null) add(ThorCapability.BATTERY_STATE)
            if (backupDestination) add(ThorCapability.BACKUP_DESTINATION)
        }
        return ThorSnapshot(
            profile = DeviceProfile.detect(properties).copy(capabilities = capabilities),
            batteryPercent = battery ?: 0,
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
            backupAvailable = PatchUtils.checkActiveSlotBackupExists(context),
            patchedBackupAvailable = PatchUtils.checkBootMagiskExists(context),
            stockBackupSlots = PatchUtils.stockBackupSlots(context),
            patchedBackupSlots = PatchUtils.patchedBackupSlots(context),
            operation = operation,
        )
    }

    override fun perform(operation: ThorOperation, argument: String?): OperationResult {
        val current = snapshot()
        if (operation.requiresThor && !current.profile.isThor) return OperationResult(false, "Only an AYN Thor can be modified")
        if (operation.requiresRootService && !current.rootServiceAvailable) {
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
            ThorOperation.INSTALL_MAGISK -> {
                val queued = MagiskUtil.enqueueLatestDownload(context)
                OperationResult(queued, if (queued) "Magisk download started in the Download folder" else "Could not start the Magisk download")
            }
            ThorOperation.BACKUP -> if (PatchUtils.backupBoot(context)) {
                OperationResult(true, "Available Thor boot partitions were backed up")
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
                val cleared = PatchUtils.clearBootCache(context)
                OperationResult(cleared, if (cleared) "Cached images cleared" else "Some cached images could not be removed")
            }
            ThorOperation.REBOOT -> {
                val rebooted = RootUtils.reboot(context)
                OperationResult(rebooted, if (rebooted) "Reboot requested" else "The Thor reboot command failed")
            }
            ThorOperation.SET_DPI -> {
                val value = argument?.toIntOrNull() ?: return OperationResult(false, "Invalid DPI")
                if (value !in AppSettings.DPI_MIN..AppSettings.DPI_MAX) return OperationResult(false, "DPI must be between ${AppSettings.DPI_MIN} and ${AppSettings.DPI_MAX}")
                val changed = RootUtils.setDpi(context, value)
                OperationResult(changed, if (changed) "DPI set to $value" else "The Thor DPI command failed")
            }
            ThorOperation.SET_ANIMATION -> {
                val value = argument?.toFloatOrNull() ?: return OperationResult(false, "Invalid animation speed")
                if (!value.isFinite() || value !in 0f..1f) return OperationResult(false, "Animation speed must be between 0x and 1x")
                val changed = RootUtils.setAnimationSpeed(context, value)
                OperationResult(changed, if (changed) "Animation speed set to ${value}x" else "The Thor animation command failed")
            }
            ThorOperation.SET_VOLUME_STEPS -> {
                val value = argument?.toIntOrNull() ?: return OperationResult(false, "Invalid volume step count")
                if (value !in AppSettings.VOLUME_STEPS_MIN..AppSettings.VOLUME_STEPS_MAX) return OperationResult(false, "Volume steps must be between ${AppSettings.VOLUME_STEPS_MIN} and ${AppSettings.VOLUME_STEPS_MAX}")
                AppSettings.setVolumeSteps(AppSettings.getSharedPrefs(context), value)
                val saved = AppSettings.save(context)
                OperationResult(saved, if (saved) "Volume steps set to $value; reboot required" else "Could not save the volume-step setting")
            }
            ThorOperation.SET_BOOT_ANIMATION -> {
                val enabled = when (argument) {
                    "true" -> true
                    "false" -> false
                    else -> return OperationResult(false, "Invalid boot-animation setting")
                }
                AppSettings.setSkipBootAnimation(AppSettings.getSharedPrefs(context), enabled)
                val saved = AppSettings.save(context)
                OperationResult(saved, if (saved) {
                    if (enabled) "Boot animation disabled; reboot required" else "Boot animation enabled; reboot required"
                } else "Could not save the boot-animation setting")
            }
        }
    }
}

class ThorSession(
    private val context: Context,
    private val backend: SystemBackend = SystemBackendFactory.create(context),
) {
    private val initialOperation = operationFromJournal()

    var snapshot by mutableStateOf(ThorSnapshot.loading(initialOperation))
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

    suspend fun load() {
        snapshot = runCatching {
            withContext(Dispatchers.IO) { backend.snapshot(initialOperation) }
        }.getOrElse {
            ThorSnapshot.loading(initialOperation.copy(status = OperationStatus.FAILURE, message = "Could not read Thor system state"))
        }
    }

    suspend fun refresh() {
        val refreshedOperation = snapshot.operation.copy(status = OperationStatus.IDLE, message = "Ready")
        snapshot = runCatching {
            withContext(Dispatchers.IO) { backend.snapshot(refreshedOperation) }
        }.getOrElse {
            snapshot.copy(operation = refreshedOperation)
        }
    }

    fun run(scope: CoroutineScope, operation: ThorOperation, argument: String? = null) {
        if (snapshot.operation.status == OperationStatus.RUNNING) return
        val running = OperationState(operation, OperationStatus.RUNNING, "${operation.name.lowercase().replace('_', ' ')} in progress")
        persistJournal(running)
        snapshot = snapshot.copy(operation = running)
        scope.launch {
            val result = try {
                withContext(Dispatchers.IO) { backend.perform(operation, argument) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                OperationResult(false, error.message?.takeIf { it.isNotBlank() } ?: "The operation failed")
            }
            val finished = OperationState(
                operation,
                if (result.success) OperationStatus.SUCCESS else OperationStatus.FAILURE,
                result.message,
            )
            clearJournal()
            snapshot = runCatching {
                withContext(Dispatchers.IO) { backend.snapshot(finished) }
            }.getOrElse {
                snapshot.copy(operation = finished)
            }
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
