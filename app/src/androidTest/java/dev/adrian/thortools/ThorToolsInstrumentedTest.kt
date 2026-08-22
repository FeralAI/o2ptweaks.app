package dev.adrian.thortools

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import android.hardware.display.DisplayManager
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ThorToolsInstrumentedTest {
    @Test
    fun usesThorToolsApplicationId() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(context.packageName.startsWith("dev.adrian.thortools"))
    }

    @Test
    fun keepsBootOverridesDisabledUntilTheUserConfiguresThem() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = context.getSharedPreferences("ThorToolsModulePolicyTest", android.content.Context.MODE_PRIVATE)
        try {
            preferences.edit().clear().commit()
            assertFalse(AppSettings.hasDpiOverride(preferences))
            assertFalse(AppSettings.hasAnimationSpeedOverride(preferences))
            assertFalse(AppSettings.hasModuleSettings(preferences))
            preferences.edit().putInt(AppSettings.DPI_KEY, 320).commit()
            preferences.edit().putFloat(AppSettings.ANIMATIONS_SPEED_KEY, 0.5f).commit()
            preferences.edit().putInt(AppSettings.VOLUME_STEPS_KEY, 20).commit()
            assertTrue(AppSettings.hasDpiOverride(preferences))
            assertTrue(AppSettings.hasAnimationSpeedOverride(preferences))
            assertTrue(AppSettings.hasModuleSettings(preferences))
        } finally {
            preferences.edit().clear().commit()
        }
    }

    @Test
    fun debugBackendPresentsThorCapabilities() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val snapshot = SystemBackendFactory.create(context).snapshot()
        assertEquals(ThorVariant.PRO, snapshot.profile.variant)
        assertTrue(snapshot.rootServiceAvailable)
        assertTrue(snapshot.backupDestinationWritable)
    }

    @Test
    fun clearingPatchedCacheKeepsStockBackups() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val backend = SystemBackendFactory.create(context)
        assertTrue(backend.perform(ThorOperation.BACKUP).success)
        assertTrue(backend.snapshot().stockRestoreAvailable)
        assertTrue(backend.perform(ThorOperation.PATCH).success)
        assertTrue(backend.snapshot().patchedBackupAvailable)
        assertTrue(backend.perform(ThorOperation.CLEAR_CACHE).success)
        val snapshot = backend.snapshot()
        assertEquals(setOf("_a", "_b"), snapshot.stockBackupSlots)
        assertTrue(snapshot.patchedBackupSlots.isEmpty())
    }

    @Test
    fun thorAvdExposesLowerDisplayGeometry() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val displayManager = context.getSystemService(DisplayManager::class.java)
        assertTrue(
            displayManager.displays.any { display ->
                DeviceProfile.isThorLowerDisplay(display.mode.physicalWidth, display.mode.physicalHeight)
            },
        )
    }

    @Test
    fun launchesTheDualDisplayActivityWithoutASecondaryWindowCrash() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        scenario.onActivity { activity ->
            assertEquals(Lifecycle.State.RESUMED, activity.lifecycle.currentState)
        }
        scenario.close()
    }

    @Test
    fun restoresInterruptedOperationStateWithoutRepeatingBackendWork() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = AppSettings.getSharedPrefs(context)
        preferences.edit()
            .putString(AppSettings.JOURNAL_OPERATION_KEY, ThorOperation.FLASH.name)
            .putString(AppSettings.JOURNAL_MESSAGE_KEY, "flash was interrupted")
            .commit()
        var performCount = 0
        val baseline = SystemBackendFactory.create(context).snapshot()
        val backend = object : SystemBackend {
            override fun snapshot(operation: OperationState): ThorSnapshot = baseline.copy(operation = operation)

            override fun perform(operation: ThorOperation, argument: String?): OperationResult {
                performCount += 1
                return OperationResult(true, "unexpected backend execution")
            }
        }
        try {
            val session = ThorSession(context, backend)
            assertEquals(OperationStatus.INTERRUPTED, session.snapshot.operation.status)
            session.load()
            assertEquals(OperationStatus.INTERRUPTED, session.snapshot.operation.status)
            assertEquals(0, performCount)
            assertTrue(session.acknowledgeInterruptedOperation())
            assertEquals(OperationStatus.IDLE, session.snapshot.operation.status)
            assertFalse(preferences.contains(AppSettings.JOURNAL_OPERATION_KEY))
            assertFalse(preferences.contains(AppSettings.JOURNAL_MESSAGE_KEY))
        } finally {
            preferences.edit()
                .remove(AppSettings.JOURNAL_OPERATION_KEY)
                .remove(AppSettings.JOURNAL_MESSAGE_KEY)
                .commit()
        }
    }
}
