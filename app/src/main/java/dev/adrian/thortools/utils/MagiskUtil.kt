package dev.adrian.thortools.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import java.io.File

object MagiskUtil {
    const val MAGISK_DIR = "/data/adb/magisk"
    const val MAGISK_PACKAGE_NAME = "com.topjohnwu.magisk"
    const val MAGISK_ACTIVITY_MAIN = "com.topjohnwu.magisk/com.topjohnwu.magisk.ui.MainActivity"
    private const val MAGISK_DOWNLOAD_URL = "https://github.com/topjohnwu/Magisk/releases/latest/download/Magisk.apk"

    private fun getMagiskAppPath(context: Context): String = runCatching {
        context.packageManager.getApplicationInfo(MAGISK_PACKAGE_NAME, 0).publicSourceDir
    }.getOrDefault("")

    fun enqueueLatestDownload(context: Context): Boolean = runCatching {
        val request = DownloadManager.Request(Uri.parse(MAGISK_DOWNLOAD_URL))
            .setTitle("Magisk")
            .setDescription("Downloading the latest Magisk release")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Magisk.apk")
        (context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager).enqueue(request)
        true
    }.getOrDefault(false)

    fun getMagiskPath(context: Context): String {
        if (RootUtils.checkFileExistsRoot(context, "$MAGISK_DIR/magisk")) return MAGISK_DIR
        if (!hasMagiskPackage(context)) return ""
        val localPath = FileUtils.getPathAppFiles(context, "/magisk/magisk")
        val localMagisk = File(localPath)
        if (!localMagisk.isFile || localMagisk.length() == 0L) {
            val temporaryDirectory = FileUtils.getPathAppFiles(context, "/magisk")
            RootUtils.runRootCommand(context, "rm -rf \"$temporaryDirectory\"")
            installLocalMagiskUtils(context)
        }
        return if (localMagisk.isFile && localMagisk.length() > 0L) FileUtils.getPathAppFiles(context, "/magisk") else ""
    }

    fun hasMagiskPackage(context: Context): Boolean = RootUtils.isPackageInstalled(context, MAGISK_PACKAGE_NAME)

    fun installMagiskModule(context: Context, zipFile: String): Boolean {
        if (!FileUtils.checkFileExists(zipFile)) return false
        return RootUtils.runRootCommand(context, "magisk --install-module \"$zipFile\" >/dev/null 2>&1; printf '%s' \$?") == "0"
    }

    fun installLocalMagiskUtils(context: Context): Boolean {
        val destination = File(FileUtils.getPathAppFiles(context, "/magisk"))
        val sourceApk = File(getMagiskAppPath(context))
        if (!sourceApk.exists()) return false
        destination.mkdirs()
        val command = "unzip -o -q \"${sourceApk.absolutePath}\" -d \"${destination.absolutePath}/base\""
        if (!RootUtils.runRootAction(context, command)) return false
        val sourceRoot = listOf(
            "${destination.absolutePath}/base/lib/arm64-v8a",
            "${destination.absolutePath}/base/lib/arm64",
        ).firstOrNull { File(it).isDirectory } ?: return false
        val binaries = listOf(
            "libbusybox.so" to "busybox",
            "libinit-ld.so" to "init-ld",
            "libmagisk.so" to "magisk",
            "libmagiskboot.so" to "magiskboot",
            "libmagiskinit.so" to "magiskinit",
            "libmagiskpolicy.so" to "magiskpolicy",
        )
        if (!binaries.all { (source, target) ->
                RootUtils.runRootAction(context, "cp -f \"$sourceRoot/$source\" \"${destination.absolutePath}/$target\"")
            }) return false
        if (!RootUtils.runRootAction(context, "chmod a+x \"${destination.absolutePath}\"/*")) return false
        return binaries.all { (_, target) ->
            File(destination, target).isFile && File(destination, target).length() > 0
        }
    }
}
