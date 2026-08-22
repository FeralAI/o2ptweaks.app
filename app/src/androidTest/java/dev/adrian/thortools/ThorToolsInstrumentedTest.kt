package dev.adrian.thortools

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import android.content.pm.ActivityInfo
import android.hardware.display.DisplayManager
import androidx.lifecycle.Lifecycle
import dev.adrian.thortools.utils.RecoveryImageInput
import dev.adrian.thortools.utils.RecoveryManifestStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun recoveryManifestRejectsTamperingAndBuildChanges() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val preferences = AppSettings.getSharedPrefs(context)
        val image = File(context.filesDir, "recovery-manifest-test.img")
        val patched = File(context.filesDir, "recovery-manifest-test-patched.img")
        preferences.edit().remove(AppSettings.RECOVERY_MANIFEST_KEY).commit()
        try {
            image.writeText("stock")
            assertTrue(
                RecoveryManifestStore.recordLocalImages(
                    context,
                    listOf(
                        RecoveryImageInput(
                            fileName = image.name,
                            slot = "_a",
                            partition = "boot",
                            patched = false,
                            path = image.path,
                            buildIdentity = "test-fingerprint",
                        ),
                    ),
                ),
            )
            assertEquals(
                image.path,
                RecoveryManifestStore.verifiedStockSource(
                    context,
                    "_a",
                    "boot",
                    image.path,
                    image.path,
                    "test-fingerprint",
                ),
            )
            image.writeText("tampered")
            assertNull(
                RecoveryManifestStore.verifiedStockSource(
                    context,
                    "_a",
                    "boot",
                    image.path,
                    image.path,
                    "test-fingerprint",
                ),
            )
            image.writeText("stock")
            assertNull(
                RecoveryManifestStore.verifiedStockSource(
                    context,
                    "_a",
                    "boot",
                    image.path,
                    image.path,
                    "other-fingerprint",
                ),
            )
            patched.writeText("patched")
            val stockHash = RecoveryManifestStore.hashFile(image.path) ?: error("stock hash missing")
            assertTrue(
                RecoveryManifestStore.recordLocalImages(
                    context,
                    listOf(
                        RecoveryImageInput(
                            fileName = patched.name,
                            slot = "_a",
                            partition = "boot",
                            patched = true,
                            path = patched.path,
                            buildIdentity = "test-fingerprint",
                            sourceSha256 = stockHash,
                        ),
                    ),
                ),
            )
            assertTrue(
                RecoveryManifestStore.hasVerifiedPatchedImage(
                    context,
                    "_a",
                    "boot",
                    patched.path,
                    image.path,
                    "test-fingerprint",
                ),
            )
            image.writeText("new-stock")
            assertFalse(
                RecoveryManifestStore.hasVerifiedPatchedImage(
                    context,
                    "_a",
                    "boot",
                    patched.path,
                    image.path,
                    "test-fingerprint",
                ),
            )
        } finally {
            preferences.edit().remove(AppSettings.RECOVERY_MANIFEST_KEY).commit()
            image.delete()
            patched.delete()
        }
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
            assertEquals(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, activity.requestedOrientation)
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
            session.run(CoroutineScope(Dispatchers.Unconfined), ThorOperation.FLASH)
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
