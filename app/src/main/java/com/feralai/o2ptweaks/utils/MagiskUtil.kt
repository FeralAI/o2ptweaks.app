package com.feralai.o2ptweaks.utils

import android.content.Context
import android.content.Intent
import java.io.File

object MagiskUtil {
    const val MAGISK_DIR = "/data/adb/magisk"
    const val MAGISK_PACKAGE_NAME = "com.topjohnwu.magisk"
    const val MAGISK_ACTIVITY_MAIN = "com.topjohnwu.magisk/com.topjohnwu.magisk.ui.MainActivity"

    private fun getMagiskAppPath(context: Context): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(MAGISK_PACKAGE_NAME, 0)
            appInfo.publicSourceDir
        } catch (e: Throwable) {
            ""
        }
    }

    fun downloadMagisk(context: Context): Pair<String, Long> {
        val downloadUrl = RootUtils.runRootCommand(
            context,
            "curl -s https://api.github.com/repos/topjohnwu/Magisk/releases/latest | grep browser_download_url | cut -d '\"' -f 4"
        )
        if ((downloadUrl ?: "") == "")
            return Pair("", 0)

        return DownloadUtils.downloadFile(context, "Magisk", downloadUrl.toString())
    }

    fun getMagiskPath(context: Context): String {
        if (RootUtils.checkFileExistsRoot(context, "$MAGISK_DIR/magisk"))
            return MAGISK_DIR

        if (!hasMagiskPackage(context))
            return ""

        // Make local copy of Magisk utils if not fully installed
        if (!File(FileUtils.getPathAppFiles(context, "/magisk/magisk")).exists())
            installLocalMagiskUtils(context)

        val tempDir = FileUtils.getPathAppFiles(context, "/magisk")
        if (File("$tempDir/magisk").exists())
            return tempDir

        return ""
    }

    fun hasMagiskPackage(context: Context): Boolean {
        return RootUtils.isPackageInstalled(context, MAGISK_PACKAGE_NAME)
    }

    fun installMagiskModule(context: Context, zipFile: String): Boolean {
        if (!FileUtils.checkFileExists(zipFile))
            return false

        //val cmd = arrayOf("magisk", "--install-module", "\"$zipFile\"")
        val cmd = "magisk --install-module \"$zipFile\""
        val result = RootUtils.runRootCommand(context, cmd)
        return result?.startsWith("- Current boot slot:") ?: false
    }

    fun installLocalMagiskUtils(context: Context): Boolean {
        // define paths
        val dstPath = FileUtils.getPathAppFiles(context, "/magisk")
        val dstPathCos = FileUtils.getPathAppFiles(context, "/magisk/chromeos")

        // creating subfolder will create parent
        val dstPathCosDir = File(dstPathCos)
        if (!dstPathCosDir.exists() && !dstPathCosDir.mkdirs())
            return false

        // extract the base.apk
        val apkFilePath = getMagiskAppPath(context)
        val srcPath = apkFilePath.replace("/base.apk", "")
        val apkFile = File(apkFilePath)
        if (!apkFile.exists())
            return false

        val dstApkPath = "$dstPath/base"
        val dstApkFile = File(dstApkPath)
        if (!dstApkFile.exists() && !dstApkFile.mkdir())
            return false

        try {
            RootUtils.runRootCommand(context, "unzip -o -q \"$apkFilePath\" -d \"$dstApkPath/\"")
        }
        catch (e: Exception) {
            return false
        }

        // set up file copy
        val filesToCopy = listOf(
            "$srcPath/lib/arm64/libbusybox.so" to "$dstPath/busybox",
            "$srcPath/lib/arm64/libinit-ld.so" to "$dstPath/init-ld",
            "$srcPath/lib/arm64/libmagisk.so" to "$dstPath/magisk",
            "$srcPath/lib/arm/libmagisk.so" to "$dstPath/magisk32",
            "$srcPath/lib/arm64/libmagiskboot.so" to "$dstPath/magiskboot",
            "$srcPath/lib/arm64/libmagiskinit.so" to "$dstPath/magiskinit",
            "$srcPath/lib/arm64/libmagiskpolicy.so" to "$dstPath/magiskpolicy",

            "$dstApkPath/assets/addon.d.sh" to "$dstPath/addon.d.sh",
            "$dstApkPath/assets/boot_patch.sh" to "$dstPath/boot_patch.sh",
            "$dstApkPath/assets/stub.apk" to "$dstPath/stub.apk",
            "$dstApkPath/assets/util_functions.sh" to "$dstPath/util_functions.sh",

            "$dstApkPath/assets/chromeos/futility" to "$dstPathCos/futility",
            "$dstApkPath/assets/chromeos/kernel.keyblock" to "$dstPathCos/kernel.keyblock",
            "$dstApkPath/assets/chromeos/kernel_data_key.vbprivk" to "$dstPathCos/kernel_data_key.vbprivk",
        )

        for (files in filesToCopy) {
            val (srcFile, dstFile) = files
            RootUtils.runRootCommand(context, "cp -f \"$srcFile\" \"$dstFile\"")
        }

        //RootUtils.runRootCommand(context, "chown -R root \"$dstPath/*\"")
        //RootUtils.runRootCommand(context, "chmod 775 \"$dstPath/*\"")
        RootUtils.runRootCommand(context, "chmod a+x \"$dstPath/*\"")

        return true
    }

    fun startMagiskApp(context: Context) {
        val launchIntent: Intent? = context.packageManager.getLaunchIntentForPackage(
            MAGISK_PACKAGE_NAME
        )
        if (launchIntent != null) {
            context.startActivity(launchIntent)
        }
    }
}