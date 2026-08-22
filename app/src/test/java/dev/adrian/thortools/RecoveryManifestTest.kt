package dev.adrian.thortools

import dev.adrian.thortools.utils.RecoveryImageRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecoveryManifestTest {
    private val record = RecoveryImageRecord(
        fileName = "boot_a.img",
        slot = "_a",
        partition = "boot",
        patched = false,
        size = 8,
        sha256 = "hash",
        buildIdentity = "fingerprint",
    )

    @Test
    fun matchesOnlyTheRecordedRecoveryContext() {
        assertTrue(record.matches("_a", "boot", false, "fingerprint"))
        assertFalse(record.matches("_b", "boot", false, "fingerprint"))
        assertFalse(record.matches("_a", "init_boot", false, "fingerprint"))
        assertFalse(record.matches("_a", "boot", true, "fingerprint"))
        assertFalse(record.matches("_a", "boot", false, "other-fingerprint"))
        assertFalse(record.matches("_a", "boot", false, ""))
    }
}
