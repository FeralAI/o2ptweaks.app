package dev.adrian.thortools

import dev.adrian.thortools.utils.PatchUtils
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PatchUtilsTest {
    @Test
    fun requiresEveryAvailableSlotToBeBackedUp() {
        assertTrue(PatchUtils.hasCompleteSlotCoverage(setOf("_a", "_b"), setOf("_a", "_b")))
        assertFalse(PatchUtils.hasCompleteSlotCoverage(setOf("_a", "_b"), setOf("_a")))
        assertTrue(PatchUtils.hasCompleteSlotCoverage(setOf("_a"), setOf("_a", "_b")))
        assertFalse(PatchUtils.hasCompleteSlotCoverage(emptySet(), setOf("_a", "_b")))
    }
}
