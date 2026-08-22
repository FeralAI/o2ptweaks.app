package dev.adrian.thortools.utils

import android.content.Context
import java.io.File
import java.security.MessageDigest

object PatchUtils {
    private val slots = listOf("_a", "_b")

    fun backupBoot(context: Context): Boolean {
        if (!RootUtils.hasPServer() || !FileUtils.isBackupDestinationWritable(context)) return false
        val requiredSlots = slots.filter { slot ->
            RootUtils.hasPartition(context, "init_boot", slot) ||
                RootUtils.hasPartition(context, "boot", slot)
        }.toSet()
        val initBootBackedUp = RootUtils.runRootScript(context, "init_boot.backup.sh") == "0"
        val bootBackedUp = RootUtils.runRootScript(context, "boot.backup.sh") == "0"
        return (initBootBackedUp || bootBackedUp) &&
            hasCompleteSlotCoverage(requiredSlots, stockBackupSlots(context))
    }

    internal fun hasCompleteSlotCoverage(requiredSlots: Set<String>, backedUpSlots: Set<String>): Boolean =
        requiredSlots.isNotEmpty() && requiredSlots.all(backedUpSlots::contains)

    fun stockBackupSlots(context: Context): Set<String> = slots.filter { slot ->
        nonEmpty(FileUtils.getPathBackup(context, "/init_boot$slot.img")) ||
            nonEmpty(FileUtils.getPathBackup(context, "/boot$slot.img"))
    }.toSet()

    fun patchedBackupSlots(context: Context): Set<String> = slots.filter { slot ->
        nonEmpty(FileUtils.getPathBackup(context, "/init_boot_patched$slot.img")) ||
            nonEmpty(FileUtils.getPathBackup(context, "/boot_patched$slot.img"))
    }.toSet()

    fun checkActiveSlotBackupExists(context: Context): Boolean {
        val slot = validSlot() ?: return false
        return slot in stockBackupSlots(context)
    }

    fun checkActiveSlotRestoreExists(context: Context): Boolean {
        val slot = validSlot() ?: return false
        val localImages = listOf(
            FileUtils.getPathBackup(context, "/init_boot$slot.img"),
            FileUtils.getPathBackup(context, "/boot$slot.img"),
        )
        if (localImages.any(::nonEmpty)) return true
        if (!RootUtils.hasPServer()) return false
        return downloadStockImageAvailable(context, slot, "init_boot") ||
            downloadStockImageAvailable(context, slot, "boot")
    }

    fun checkBootMagiskExists(context: Context): Boolean {
        val slot = validSlot() ?: return false
        return slot in patchedBackupSlots(context)
    }

    fun imagePaths(context: Context): Map<String, String> = slots.flatMap { slot ->
        listOf(
            "init_boot$slot.img" to FileUtils.getPathBackup(context, "/init_boot$slot.img"),
            "boot$slot.img" to FileUtils.getPathBackup(context, "/boot$slot.img"),
            "init_boot_patched$slot.img" to FileUtils.getPathBackup(context, "/init_boot_patched$slot.img"),
            "boot_patched$slot.img" to FileUtils.getPathBackup(context, "/boot_patched$slot.img"),
        )
    }.filter { (_, path) -> nonEmpty(path) }.toMap()

    fun imageHashes(context: Context): Map<String, String> {
        return imagePaths(context).mapNotNull { (name, path) ->
            runCatching { name to sha256(path) }.getOrNull()
        }.toMap()
    }

    fun clearBootCache(context: Context): Boolean {
        var success = true
        slots.forEach { slot ->
            listOf(
                "/boot_patched$slot.img",
                "/init_boot_patched$slot.img",
            ).forEach { relativePath ->
                val path = FileUtils.getPathBackup(context, relativePath)
                if (File(path).exists() && !FileUtils.deleteFile(path)) success = false
            }
        }
        return success
    }

    fun flashBoot(context: Context): Boolean {
        val slot = validSlot() ?: return false
        val initBootPath = FileUtils.getPathBackup(context, "/init_boot_patched$slot.img")
        val bootPath = FileUtils.getPathBackup(context, "/boot_patched$slot.img")
        return when {
            nonEmpty(initBootPath) && RootUtils.hasPartition(context, "init_boot", slot) -> {
                RootUtils.runRootScript(context, "init_boot.flash.sh") == "0"
            }
            nonEmpty(bootPath) && RootUtils.hasPartition(context, "boot", slot) -> {
                RootUtils.runRootScript(context, "boot.flash.sh") == "0"
            }
            else -> false
        }
    }

    fun patchBoot(context: Context): String {
        val magiskPath = MagiskUtil.getMagiskPath(context)
        val slot = validSlot() ?: return ""
        if (magiskPath.isBlank()) return ""
        val initBootSource = FileUtils.getPathBackup(context, "/init_boot$slot.img")
        val bootSource = FileUtils.getPathBackup(context, "/boot$slot.img")
        if (nonEmpty(initBootSource) && RootUtils.hasPartition(context, "init_boot", slot)) {
            val initBootPatched = FileUtils.getPathBackup(context, "/init_boot_patched$slot.img")
            FileUtils.deleteFile(initBootPatched)
            if (RootUtils.runRootScript(context, "init_boot.patch.sh", listOf(magiskPath)) == "0" && nonEmpty(initBootPatched)) {
                return initBootPatched
            }
        }
        if (nonEmpty(bootSource) && RootUtils.hasPartition(context, "boot", slot)) {
            val bootPatched = FileUtils.getPathBackup(context, "/boot_patched$slot.img")
            FileUtils.deleteFile(bootPatched)
            if (RootUtils.runRootScript(context, "boot.patch.sh", listOf(magiskPath)) == "0" && nonEmpty(bootPatched)) {
                return bootPatched
            }
        }
        return ""
    }

    fun restoreBoot(context: Context): Boolean {
        val slot = validSlot() ?: return false
        val initBootPath = FileUtils.getPathBackup(context, "/init_boot$slot.img")
        val bootPath = FileUtils.getPathBackup(context, "/boot$slot.img")
        return when {
            (nonEmpty(initBootPath) || downloadStockImageAvailable(context, slot, "init_boot")) && RootUtils.hasPartition(context, "init_boot", slot) -> {
                RootUtils.runRootScript(context, "init_boot.restore.sh") == "0"
            }
            (nonEmpty(bootPath) || downloadStockImageAvailable(context, slot, "boot")) && RootUtils.hasPartition(context, "boot", slot) -> {
                RootUtils.runRootScript(context, "boot.restore.sh") == "0"
            }
            else -> false
        }
    }

    private fun validSlot(): String? = SystemUtils.getPropSlot().takeIf { it == "_a" || it == "_b" }

    private fun downloadStockImageAvailable(context: Context, slot: String, partitionName: String): Boolean =
        RootUtils.checkFileNonEmptyRoot(context, FileUtils.getPathDownload("/$partitionName$slot.img"))

    private fun nonEmpty(path: String): Boolean = File(path).isFile && File(path).length() > 0

    private fun sha256(path: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        File(path).inputStream().use { input ->
            val buffer = ByteArray(1024 * 1024)
            var read = input.read(buffer)
            while (read > 0) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}
