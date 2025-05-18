package com.feralai.o2ptweaks.utils

import android.content.Context

object JdspUtils {
    const val JDSP_PACKAGE_NAME = "james.dsp"

    fun copyBackupFile(context: Context) {
        val backupFile = "app/support/conf_files/jamesdsp_backup_o2ptweaks.backup"
        val outFile = FileUtils.getPathDownload("/jamesdsp_backup_o2ptweaks.tar.gz")
        FileUtils.copyAsset(context, backupFile, outFile)
    }

    fun enableJdsp(context: Context) {
        RootUtils.runRootScript(context, "jdsp.enable.sh")
    }

    fun disableJdsp(context: Context) {
        RootUtils.runRootScript(context, "jdsp.disable.sh")
    }

    fun downloadJdspManager(context: Context): Pair<String, Long> {
        val downloadUrl = "https://nightly.timschneeberger.me/jamesdsp-rootfull/JamesDSP-v1.6.14-51-root-full-universal-release-signed.apk"
        return DownloadUtils.downloadFile(context, "JamesDSP Manager", downloadUrl)
    }

    fun hasJdspPackage(context: Context): Boolean {
        return RootUtils.isPackageInstalled(context, JDSP_PACKAGE_NAME)
    }

    fun installJdspManager(context: Context) {
        ApkUtils.installApkFromAssets(context, "JamesDSPManagerThePBone.apk", "app")
    }

    fun installJdspMagiskModule(context: Context): Boolean {
        RootUtils.runRootScript(context, "jdsp.magisk.sh")
        return true
    }
}