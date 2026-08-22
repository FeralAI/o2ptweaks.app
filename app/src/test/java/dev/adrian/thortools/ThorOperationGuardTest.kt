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

    private fun snapshot(
        rootServiceAvailable: Boolean = true,
        batteryPercent: Int = 80,
        activeSlot: String = "_a",
        backupDestinationWritable: Boolean = true,
        initBootAvailable: Boolean = true,
        bootAvailable: Boolean = true,
    ): ThorSnapshot {
        return ThorSnapshot(
            profile = DeviceProfile.detect(DeviceProperties(model = "AYN Thor")),
            batteryPercent = batteryPercent,
            lcdDensity = 320,
            volumeSteps = 15,
            animationSpeed = 1f,
            activeSlot = activeSlot,
            kernelVersion = "test",
            rootServiceAvailable = rootServiceAvailable,
            rooted = false,
            magiskInstalled = true,
            initBootAvailable = initBootAvailable,
            bootAvailable = bootAvailable,
            backupDestinationWritable = backupDestinationWritable,
            backupAvailable = false,
            patchedBackupAvailable = false,
            operation = OperationState(),
        )
    }
}
