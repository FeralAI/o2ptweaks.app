package dev.adrian.thortools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThorOperationGuardTest {
    @Test
    fun acceptsReadyThorImageOperation() {
        assertNull(ThorOperationGuard.validate(snapshot(), ThorOperation.BACKUP))
    }

    @Test
    fun allowsStockBackupBeforeMagiskIsInstalled() {
        assertNull(ThorOperationGuard.validate(snapshot(magiskInstalled = false), ThorOperation.BACKUP))
    }

    @Test
    fun rejectsMutationsOutsideThor() {
        val nonThor = snapshot().copy(profile = DeviceProfile.detect(DeviceProperties(model = "AYN Loki")))
        assertNull(ThorOperationGuard.validate(nonThor, ThorOperation.REFRESH))
        assertEquals(
            "Only an AYN Thor can be modified",
            ThorOperationGuard.validate(nonThor, ThorOperation.INSTALL_MAGISK),
        )
    }

    @Test
    fun rejectsMutationsWithoutRootService() {
        assertEquals(
            "The Thor privileged root service is unavailable",
            ThorOperationGuard.validate(snapshot(rootServiceAvailable = false), ThorOperation.SET_DPI),
        )
    }

    @Test
    fun rejectsUnsafeImageConditions() {
        assertEquals(
            "Charge the Thor to at least 35% before image operations",
            ThorOperationGuard.validate(snapshot(batteryPercent = 34), ThorOperation.BACKUP),
        )
        assertEquals(
            "The Thor battery state is unavailable",
            ThorOperationGuard.validate(snapshot(batteryAvailable = false), ThorOperation.BACKUP),
        )
        assertEquals(
            "The active Thor slot could not be determined",
            ThorOperationGuard.validate(snapshot(activeSlot = "unknown"), ThorOperation.BACKUP),
        )
        assertEquals(
            "The Thor backup destination is not writable",
            ThorOperationGuard.validate(snapshot(backupDestinationWritable = false), ThorOperation.BACKUP),
        )
        assertEquals(
            "No supported Thor boot partition was found",
            ThorOperationGuard.validate(snapshot(initBootAvailable = false, bootAvailable = false), ThorOperation.BACKUP),
        )
    }

    @Test
    fun protectsStockBackupAndPatchPrerequisites() {
        assertEquals(
            "Capture stock backups before root is active",
            ThorOperationGuard.validate(snapshot(rooted = true), ThorOperation.BACKUP),
        )
        assertEquals(
            "Create a verified init_boot stock backup before patching",
            ThorOperationGuard.validate(snapshot(magiskInstalled = true), ThorOperation.PATCH),
        )
        assertEquals(
            "Install Magisk before preparing a root patch",
            ThorOperationGuard.validate(snapshot(magiskInstalled = false, backupAvailable = true), ThorOperation.PATCH),
        )
        assertEquals(
            "Keep a verified init_boot stock backup before flashing a root patch",
            ThorOperationGuard.validate(snapshot(magiskInstalled = true, patchedBackupAvailable = true), ThorOperation.FLASH),
        )
    }

    @Test
    fun keepsPatchedCacheCleanupAvailableWithoutRootService() {
        assertNull(
            ThorOperationGuard.validate(
                snapshot(rootServiceAvailable = false),
                ThorOperation.CLEAR_CACHE,
            ),
        )
    }

    @Test
    fun keepsRestoreAvailableAfterRootFlash() {
        assertNull(
            ThorOperationGuard.validate(
                snapshot(rooted = true, backupAvailable = true, stockRestoreAvailable = true),
                ThorOperation.RESTORE,
            ),
        )
    }

    @Test
    fun allowsRestoreFromIndependentStockCopy() {
        assertNull(
            ThorOperationGuard.validate(
                snapshot(rooted = true, stockRestoreAvailable = true),
                ThorOperation.RESTORE,
            ),
        )
    }

    @Test
    fun rejectsRestoreWithoutAnyStockCopy() {
        assertEquals(
            "Create a verified init_boot stock backup before restoring",
            ThorOperationGuard.validate(snapshot(rooted = true), ThorOperation.RESTORE),
        )
    }

    @Test
    fun rejectsRestoreWithoutABootPartition() {
        assertEquals(
            "No supported Thor boot partition was found",
            ThorOperationGuard.validate(
                snapshot(rooted = true, stockRestoreAvailable = true, initBootAvailable = false, bootAvailable = false),
                ThorOperation.RESTORE,
            ),
        )
    }

    @Test
    fun reportsInitBootAsTheRecoveryTargetWhenBothPartitionsExist() {
        assertEquals("init_boot", snapshot().recoveryPartition)
        assertEquals("boot", snapshot(initBootAvailable = false).recoveryPartition)
        assertEquals("Unavailable", snapshot(initBootAvailable = false, bootAvailable = false).recoveryPartition)
    }

    @Test
    fun protectsMagiskModuleSettings() {
        assertEquals(
            "Root the Thor with Magisk before changing module settings",
            ThorOperationGuard.validate(snapshot(rooted = false), ThorOperation.SET_VOLUME_STEPS),
        )
    }

    private fun snapshot(
        rootServiceAvailable: Boolean = true,
        batteryPercent: Int = 80,
        activeSlot: String = "_a",
        backupDestinationWritable: Boolean = true,
        initBootAvailable: Boolean = true,
        bootAvailable: Boolean = true,
        rooted: Boolean = false,
        magiskInstalled: Boolean = true,
        backupAvailable: Boolean = false,
        stockRestoreAvailable: Boolean = backupAvailable,
        patchedBackupAvailable: Boolean = false,
        batteryAvailable: Boolean = true,
    ): ThorSnapshot {
        return ThorSnapshot(
            profile = DeviceProfile.detect(DeviceProperties(model = "AYN Thor")).copy(
                capabilities = if (batteryAvailable) setOf(ThorCapability.BATTERY_STATE) else emptySet(),
            ),
            batteryPercent = batteryPercent,
            lcdDensity = 320,
            volumeSteps = 15,
            animationSpeed = 1f,
            activeSlot = activeSlot,
            kernelVersion = "test",
            rootServiceAvailable = rootServiceAvailable,
            rooted = rooted,
            magiskInstalled = magiskInstalled,
            initBootAvailable = initBootAvailable,
            bootAvailable = bootAvailable,
            backupDestinationWritable = backupDestinationWritable,
            backupAvailable = backupAvailable,
            stockRestoreAvailable = stockRestoreAvailable,
            patchedBackupAvailable = patchedBackupAvailable,
            operation = OperationState(),
        )
    }
}
