package dev.adrian.thortools

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RecoveryScriptContractTest {
    @Test
    fun stockBackupsRequireVerifiedDownloadCopies() {
        listOf("boot.backup.sh", "init_boot.backup.sh").forEach { name ->
            val source = script(name)
            assertTrue(source.contains("failed=0"))
            assertTrue(source.contains("DOWNLOAD_FILE="))
            assertTrue(source.contains("[ -s \"\$DOWNLOAD_FILE\" ]"))
            assertTrue(source.contains("[ \"\$copied\" -eq 1 ] && [ \"\$failed\" -eq 0 ]"))
        }
    }

    @Test
    fun patchedImagesRemainUsableWhenDownloadCopyFails() {
        listOf("boot.patch.sh", "init_boot.patch.sh").forEach { name ->
            val source = script(name)
            assertTrue(source.contains("[ -s \"\$PATCHED_BOOT\" ]"))
            assertTrue(source.contains("Download copy unavailable; the app-local patched image remains usable"))
        }
    }

    private fun script(name: String): String =
        File("src/main/assets/app/support/subscripts/$name").readText()
}
