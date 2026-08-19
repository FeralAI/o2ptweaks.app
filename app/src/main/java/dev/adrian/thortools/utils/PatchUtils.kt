package dev.adrian.thortools.utils

import android.content.Context
import java.io.File
import java.security.MessageDigest

object PatchUtils {
    private val slots = listOf("_a", "_b")

    fun backupBoot(context: Context): Boolean {
        if (!RootUtils.hasPServer() || !FileUtils.isBackupDestinationWritable(context)) return false
        RootUtils.runRootScript(context, "init_boot.backup.sh")
        RootUtils.runRootScript(context, "boot.backup.sh")
        return checkBootBackupExists(context)
    }

    fun checkBootBackupExists(context: Context): Boolean = slots.any { slot ->
        nonEmpty(FileUtils.getPathBackup(context, "/init_boot$slot.img")) ||
            nonEmpty(FileUtils.getPathBackup(context, "/boot$slot.img"))
    }

    fun checkBootMagiskExists(context: Context): Boolean {
        val slot = SystemUtils.getPropSlot()
        return nonEmpty(FileUtils.getPathBackup(context, "/init_boot_patched$slot.img")) ||
            nonEmpty(FileUtils.getPathBackup(context, "/boot_patched$slot.img"))
    }

    fun imageHashes(context: Context): Map<String, String> {
        val paths = slots.flatMap { slot ->
            listOf(
                "init_boot$slot.img" to FileUtils.getPathBackup(context, "/init_boot$slot.img"),
                "boot$slot.img" to FileUtils.getPathBackup(context, "/boot$slot.img"),
                "init_boot_patched$slot.img" to FileUtils.getPathBackup(context, "/init_boot_patched$slot.img"),
                "boot_patched$slot.img" to FileUtils.getPathBackup(context, "/boot_patched$slot.img"),
            )
        }
        return paths.mapNotNull { (name, path) ->
            if (nonEmpty(path)) name to sha256(path) else null
        }.toMap()
    }

    fun clearBootCache(context: Context) {
        slots.forEach { slot ->
            listOf(
                "/boot$slot.img",
                "/boot_patched$slot.img",
                "/init_boot$slot.img",
                "/init_boot_patched$slot.img",
            ).forEach { relativePath ->
                FileUtils.deleteFile(FileUtils.getPathBackup(context, relativePath))
            }
        }
    }

    fun flashBoot(context: Context): Boolean {
        val slot = validSlot() ?: return false
        val initBootPath = FileUtils.getPathBackup(context, "/init_boot_patched$slot.img")
        val bootPath = FileUtils.getPathBackup(context, "/boot_patched$slot.img")
        return when {
            nonEmpty(initBootPath) && RootUtils.hasPartition(context, "init_boot", slot) -> {
                RootUtils.runRootScript(context, "init_boot.flash.sh")
                true
            }
            nonEmpty(bootPath) && RootUtils.hasPartition(context, "boot", slot) -> {
                RootUtils.runRootScript(context, "boot.flash.sh")
                true
            }
            else -> false
        }
    }

    fun patchBoot(context: Context): String {
        val magiskPath = MagiskUtil.getMagiskPath(context)
        val slot = validSlot() ?: return ""
        if (magiskPath.isBlank()) return ""
        if (RootUtils.hasPartition(context, "init_boot", slot)) {
            RootUtils.runRootScript(context, "init_boot.patch.sh \"$magiskPath\"")
            val initBootPatched = FileUtils.getPathBackup(context, "/init_boot_patched$slot.img")
            if (nonEmpty(initBootPatched)) {
                FileUtils.deleteFile(FileUtils.getPathBackup(context, "/boot$slot.img"))
                return initBootPatched
            }
        }
        if (RootUtils.hasPartition(context, "boot", slot)) {
            RootUtils.runRootScript(context, "boot.patch.sh \"$magiskPath\"")
            val bootPatched = FileUtils.getPathBackup(context, "/boot_patched$slot.img")
            if (nonEmpty(bootPatched)) {
                FileUtils.deleteFile(FileUtils.getPathBackup(context, "/init_boot$slot.img"))
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
            nonEmpty(initBootPath) && RootUtils.hasPartition(context, "init_boot", slot) -> {
                RootUtils.runRootScript(context, "init_boot.restore.sh")
                true
            }
            nonEmpty(bootPath) && RootUtils.hasPartition(context, "boot", slot) -> {
                RootUtils.runRootScript(context, "boot.restore.sh")
                true
            }
            else -> false
        }
    }

    private fun validSlot(): String? = SystemUtils.getPropSlot().takeIf { it == "_a" || it == "_b" }

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
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
}
