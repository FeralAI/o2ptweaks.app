package dev.adrian.thortools.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader

fun copyAssetFolderToFilesDir(context: Context, assetFolderPath: String) {
    val assetManager = context.assets
    val assetFiles = runCatching { assetManager.list(assetFolderPath) }.getOrNull() ?: return
    if (assetFiles.isEmpty()) {
        File(context.filesDir, assetFolderPath).mkdirs()
        return
    }
    assetFiles.forEach { assetFileName ->
        val fullAssetPath = if (assetFolderPath.isEmpty()) assetFileName else "$assetFolderPath/$assetFileName"
        try {
            assetManager.open(fullAssetPath).use { input ->
                val outputFile = File(context.filesDir, fullAssetPath)
                outputFile.parentFile?.mkdirs()
                FileOutputStream(outputFile).use(input::copyTo)
                outputFile.setReadable(true)
                outputFile.setExecutable(true)
            }
        } catch (_: IOException) {
            copyAssetFolderToFilesDir(context, fullAssetPath)
        }
    }
}

object RootUtils {
    const val MODULE_DIR = "/data/adb/modules"
    private const val TAG = "RootUtils"
    private const val ASSET_SUBFOLDER = "app"

    val isDeviceRooted: Boolean
        get() = checkRootMethod1() || checkRootMethod2() || checkRootMethod3()

    private fun checkRootMethod1(): Boolean = Build.TAGS?.contains("test-keys") == true

    private fun checkRootMethod2(): Boolean = arrayOf(
        "/system/app/Superuser.apk",
        "/sbin/su",
        "/system/bin/su",
        "/system/xbin/su",
        "/data/local/xbin/su",
        "/data/local/bin/su",
        "/system/sd/xbin/su",
        "/system/bin/failsafe/su",
        "/data/local/su",
        "/su/bin/su",
        "/product/bin/su",
    ).any { path -> File(path).exists() }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            BufferedReader(InputStreamReader(process.inputStream)).readLine() != null
        } catch (_: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    fun hasPServer(): Boolean = RootExec().pServerAvailable

    fun runRootCommand(context: Context, command: String): String? {
        val result = RootExec().executeAsRoot(command)
        return result.getOrNull().also { Log.d(TAG, "root command completed: ${result.isSuccess}") }
    }

    fun runRootScript(context: Context, script: String): String? {
        val filesPath = File(context.filesDir, ASSET_SUBFOLDER).absolutePath
        val logPath = getLogFile(context)?.absolutePath ?: return null
        val workingPath = context.getExternalFilesDir(null)?.absolutePath ?: return null
        val command = "THORTOOLS_WORKING_PATH=\"$workingPath\" sh $filesPath/support/subscripts/$script $filesPath > $logPath; printf '%s' $?"
        return RootExec().executeAsRoot(command).getOrNull()
    }

    fun checkFileExistsRoot(context: Context, path: String): Boolean =
        runRootCommand(context, "[ -e \"$path\" ] && echo 1 || echo 0") == "1"

    fun reboot(context: Context) {
        runRootCommand(context, "reboot")
    }

    fun rootCopy(context: Context, from: String, to: String) {
        if (File(from).exists()) runRootCommand(context, "cp -afv \"$from\" \"$to\"")
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun hasPartition(context: Context, partitionName: String, slot: String): Boolean =
        hasPServer() && checkFileExistsRoot(context, "/dev/block/by-name/$partitionName$slot")

    fun installThorToolsMagiskModule(context: Context) {
        val modulePath = "$MODULE_DIR/thortools"
        val sourcePath = "${context.filesDir}/app/support/magisk/thortools"
        runRootCommand(context, "mkdir -p $modulePath")
        runRootCommand(context, "cp -fR $sourcePath/. $modulePath/")
    }

    fun startActivityRoot(context: Context, activity: String) {
        runRootCommand(context, "am start -n $activity")
    }

    fun setAnimationSpeed(context: Context, animationSpeed: Float) {
        runRootCommand(context, "settings put global window_animation_scale $animationSpeed")
        runRootCommand(context, "settings put global transition_animation_scale $animationSpeed")
        runRootCommand(context, "settings put global animator_duration_scale $animationSpeed")
    }

    fun setDpi(context: Context, dpi: Int) {
        runRootCommand(context, "wm density $dpi")
    }

    fun resetDpi(context: Context) {
        runRootCommand(context, "resetprop -p ro.sf.lcd_density")
    }
}
