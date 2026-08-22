package dev.adrian.thortools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThorDeviceProfileTest {
    @Test
    fun recognizesThorFromAnyHardwareIdentityField() {
        assertTrue(DeviceProfile.detect(DeviceProperties(model = "AYN Thor")).isThor)
        assertTrue(DeviceProfile.detect(DeviceProperties(model = "tHoR")).isThor)
        assertTrue(DeviceProfile.detect(DeviceProperties(model = "AYNThor")).isThor)
        assertTrue(DeviceProfile.detect(DeviceProperties(device = "thor_lite")).isThor)
        assertTrue(DeviceProfile.detect(DeviceProperties(device = "thorpro")).isThor)
        assertTrue(DeviceProfile.detect(DeviceProperties(product = "AYNThorMax")).isThor)
        assertTrue(DeviceProfile.detect(DeviceProperties(product = "AYN Thor Max")).isThor)
        assertTrue(DeviceProfile.detect(DeviceProperties(manufacturer = "AYN", model = "Thor")).isThor)
        assertTrue(DeviceProfile.detect(DeviceProperties(hardware = "thor_lite")).isThor)
        assertTrue(DeviceProfile.detect(DeviceProperties(board = "thor_max_board")).isThor)
    }

    @Test
    fun classifiesEveryThorVariant() {
        assertEquals(ThorVariant.LITE, DeviceProfile.variantFor("AYN Thor Lite"))
        assertEquals(ThorVariant.BASE, DeviceProfile.variantFor("AYN Thor"))
        assertEquals(ThorVariant.PRO, DeviceProfile.variantFor("AYN Thor Pro"))
        assertEquals(ThorVariant.MAX, DeviceProfile.variantFor("AYN Thor Max"))
        assertEquals(ThorVariant.PRO, DeviceProfile.variantFor("thorpro"))
        assertEquals(ThorVariant.UNKNOWN, DeviceProfile.variantFor("AYN Loki"))
        assertEquals(ThorVariant.UNKNOWN, DeviceProfile.variantFor("Loki Pro"))
    }

    @Test
    fun doesNotTreatOtherDevicesAsThor() {
        assertFalse(DeviceProfile.detect(DeviceProperties(model = "AYN Loki")).isThor)
        assertFalse(DeviceProfile.detect(DeviceProperties(model = "Retroid Pocket Mini")).isThor)
    }

    @Test
    fun classifiesThorDisplayGeometry() {
        assertTrue(DeviceProfile.isThorLowerDisplay(1240, 1080))
        assertFalse(DeviceProfile.isThorLowerDisplay(1080, 1240))
        assertFalse(DeviceProfile.isThorLowerDisplay(1920, 1080))
        assertEquals("lower", DeviceProfile.displayKind(1240, 1080))
        assertEquals("upper", DeviceProfile.displayKind(1920, 1080))
    }

    @Test
    fun keepsLowerDisplayTextReadable() {
        assertTrue(
            DeviceProfile.minimumReadablePixels(1240, 1080) >
                DeviceProfile.minimumReadablePixels(1920, 1080),
        )
        assertEquals(
            47.25f,
            DeviceProfile.minimumReadablePixels(1240, 1080, 2.625f),
            0.001f,
        )
    }

    @Test
    fun normalizesActiveSlotIdentifiers() {
        assertEquals("_a", DeviceProfile.normalizeSlot("_a"))
        assertEquals("_a", DeviceProfile.normalizeSlot("a"))
        assertEquals("_b", DeviceProfile.normalizeSlot(" B "))
        assertEquals("", DeviceProfile.normalizeSlot("unknown"))
    }

    @Test
    fun prefersBuildFingerprintForRecoveryIdentity() {
        assertEquals(
            "ayn/thor:13/THOR/1:user/release-keys",
            DeviceProperties(
                buildFingerprint = "ayn/thor:13/THOR/1:user/release-keys",
                buildId = "fallback",
            ).buildIdentity,
        )
        assertEquals(
            "fallback|display|14|date",
            DeviceProperties(
                buildId = "fallback",
                buildDisplayId = "display",
                firmware = "14",
                buildDate = "date",
            ).buildIdentity,
        )
    }

    @Test
    fun hidesThorCapabilitiesForDiagnosticsOutsideThor() {
        val snapshot = ThorSnapshot.loading(OperationState()).copy(
            profile = DeviceProfile.detect(DeviceProperties(model = "AYN Loki")).copy(
                capabilities = ThorCapability.entries.toSet(),
            ),
        )
        val rows = snapshot.capabilityRows.toMap()
        assertFalse(rows.getValue("Thor device"))
        assertFalse(rows.getValue("Root service"))
        assertFalse(rows.getValue("Magisk"))
        assertFalse(rows.getValue("Backup destination"))
    }
}
