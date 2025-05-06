package com.feralai.o2ptweaks.utils

import android.content.Context
import java.io.File

object PatchUtils {
    fun backupBoot(context: Context) {
        RootUtils.runRootScript(context, "init_boot.backup.sh")
        RootUtils.runRootScript(context, "boot.backup.sh")
    }

    fun checkBootBackupExists(context: Context): Boolean {
        val slot = SystemUtils.getPropSlot()

        var imgPath = FileUtils.getPathBackup(context, "/init_boot$slot.img")
        if (FileUtils.checkFileExists(imgPath))
            return true

//        imgPath = FileUtils.getPathDownload("/init_boot$slot.img")
//        if (FileUtils.checkFileExists(imgPath))
//            return true

        imgPath = FileUtils.getPathBackup(context, "/boot$slot.img")
        if (FileUtils.checkFileExists(imgPath))
            return true

//        imgPath = FileUtils.getPathDownload("/boot$slot.img")
//        if (FileUtils.checkFileExists(imgPath))
//            return true

        return false
    }

    fun checkBootMagiskExists(context: Context): Boolean {
        val slot = SystemUtils.getPropSlot()

        var imgPath = FileUtils.getPathBackup(context, "/init_boot_patched$slot.img")
        if (FileUtils.checkFileExists(imgPath))
            return true

//        imgPath = FileUtils.getPathDownload("/init_boot_patched$slot.img")
//        if (FileUtils.checkFileExists(imgPath))
//            return true

        imgPath = FileUtils.getPathBackup(context, "/boot_patched$slot.img")
        if (FileUtils.checkFileExists(imgPath))
            return true

//        imgPath = FileUtils.getPathDownload("/boot_patched$slot.img")
//        if (FileUtils.checkFileExists(imgPath))
//            return true

        return false
    }

    fun clearBootCache(context: Context) {
        val slot = SystemUtils.getPropSlot()

        FileUtils.deleteFile(FileUtils.getPathBackup(context, "/boot${slot}.img"))
        FileUtils.deleteFile(FileUtils.getPathBackup(context, "/boot_patched${slot}.img"))
        FileUtils.deleteFile(FileUtils.getPathBackup(context, "/init_boot${slot}.img"))
        FileUtils.deleteFile(FileUtils.getPathBackup(context, "/init_boot_patched${slot}.img"))
    }

    fun flashBoot(context: Context): Boolean {
        val slot = SystemUtils.getPropSlot()

        // Check for patched init_boot file
        var imgPath = FileUtils.getPathBackup(context, "/init_boot_patched$slot.img")
        if (File(imgPath).exists()) {
            RootUtils.runRootScript(context, "init_boot.flash.sh")
            return true
        }

        // Fallback to boot file
        imgPath = FileUtils.getPathBackup(context, "/boot_patched$slot.img")
        if (File(imgPath).exists()) {
            RootUtils.runRootScript(context, "boot.flash.sh")
            return true
        }

        return false
    }

    fun patchBoot(context: Context): String {
        // Magisk path is passed as param to patch scripts
        val magiskPath = MagiskUtil.getMagiskPath(context)

        // Is Magisk available?
        if (magiskPath == "")
            return ""

        var deleteBootImg = false
        var deleteInitImg = false
        var patchedFile = ""

        // Try to patch init_boot file first
        RootUtils.runRootScript(context, "init_boot.patch.sh \"$magiskPath\"")
        val slot = SystemUtils.getPropSlot()
        var imgPath = FileUtils.getPathBackup(context, "/init_boot_patched$slot.img")
        if (File(imgPath).exists()) {
            patchedFile = imgPath
            deleteBootImg = true // Don't need to patch boot.img
        }

        if (patchedFile == "") {
            // Try to patch boot file next
            RootUtils.runRootScript(context, "boot.patch.sh \"$magiskPath\"")
            imgPath = FileUtils.getPathBackup(context, "/boot_patched$slot.img")
            if (File(imgPath).exists()) {
                patchedFile = imgPath
                deleteInitImg = true // If boot.img patched, remove init_boot.img
            }
        }

        // Cleanup
        if (deleteBootImg) {
            FileUtils.deleteFile(FileUtils.getPathBackup(context, "/boot${slot}.img"))
            FileUtils.deleteFile(FileUtils.getPathDownload("/boot${slot}.img"))
        }

        if (deleteInitImg) {
            FileUtils.deleteFile(FileUtils.getPathBackup(context, "/init_boot${slot}.img"))
            FileUtils.deleteFile(FileUtils.getPathDownload("/init_boot${slot}.img"))
        }

        return patchedFile
    }

    fun restoreBoot(context: Context): Boolean {
        val slot = SystemUtils.getPropSlot()

        // Check for patched init_boot file, fallback to boot file
        var imgPath = FileUtils.getPathBackup(context, "/init_boot$slot.img")
        if (File(imgPath).exists()) {
            RootUtils.runRootScript(context, "init_boot.restore.sh")
            return true
        }

//        imgPath = FileUtils.getPathDownload("/init_boot$slot.img")
//        if (File(imgPath).exists()) {
//            RootUtils.runRootScript(context, "init_boot.restore.sh")
//            return true
//        }

        // Fallback to boot file
        imgPath = FileUtils.getPathBackup(context, "/boot$slot.img")
        if (File(imgPath).exists()) {
            RootUtils.runRootScript(context, "boot.restore.sh")
            return true
        }

//        imgPath = FileUtils.getPathDownload("/boot$slot.img")
//        if (File(imgPath).exists()) {
//            RootUtils.runRootScript(context, "boot.restore.sh")
//            return true
//        }

        return false
    }

}