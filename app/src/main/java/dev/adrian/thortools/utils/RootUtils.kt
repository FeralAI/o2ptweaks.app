package dev.adrian.thortools.utils

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dev.adrian.thortools.DeviceProfile
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
        get() = checkRootMethod2() || checkRootMethod3()

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
            val result = BufferedReader(InputStreamReader(process.inputStream)).use { it.readLine() != null }
            process.waitFor()
            result
        } catch (_: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    fun hasPServer(): Boolean = RootExec().pServerAvailable

    fun isDeviceRooted(context: Context, rootServiceAvailable: Boolean): Boolean {
        if (!rootServiceAvailable) return isDeviceRooted
        val activeRoot = runRootCommand(
            context,
            "if [ -x /data/adb/magisk/magisk ] || [ -x /data/adb/ksu/bin/ksud ] || [ -x /data/adb/ap/bin/apd ]; then printf '%s' 1; else printf '%s' 0; fi",
        )
        return activeRoot == "1" || isDeviceRooted
    }

    fun runRootCommand(context: Context, command: String): String? {
        val result = RootExec().executeAsRoot(command)
        return result.getOrNull().also { Log.d(TAG, "root command completed: ${result.isSuccess}") }
    }

    fun runRootAction(context: Context, command: String): Boolean =
        RootExec().executeAsRoot("$command >/dev/null 2>&1; printf '%s' \$?").getOrNull() == "0"

    fun runRootScript(context: Context, script: String, arguments: List<String> = emptyList()): String? {
        if (!script.matches(Regex("[A-Za-z0-9_.-]+"))) return null
        val filesPath = File(context.filesDir, ASSET_SUBFOLDER).absolutePath
        val logPath = getLogFile(context)?.absolutePath ?: return null
        val workingPath = context.getExternalFilesDir(null)?.absolutePath ?: return null
        val scriptPath = "$filesPath/support/subscripts/$script"
        val commandArguments = arguments.joinToString(" ") { shellQuote(it) }
        val command = "THORTOOLS_WORKING_PATH=${shellQuote(workingPath)} sh ${shellQuote(scriptPath)} $commandArguments > ${shellQuote(logPath)} 2>&1; printf '%s' $?"
        return RootExec().executeAsRoot(command).getOrNull()
    }

    private fun shellQuote(value: String): String = "'${value.replace("'", "'\\''")}'"

    fun checkFileExistsRoot(context: Context, path: String): Boolean =
        runRootCommand(context, "[ -e ${shellQuote(path)} ] && printf '%s' 1 || printf '%s' 0") == "1"

    fun checkFileNonEmptyRoot(context: Context, path: String): Boolean =
        runRootCommand(context, "[ -s ${shellQuote(path)} ] && printf '%s' 1 || printf '%s' 0") == "1"

    fun reboot(context: Context): Boolean =
        RootExec().executeAsRoot("reboot >/dev/null 2>&1 & printf '%s' 0").getOrNull() == "0"

    fun rootCopy(context: Context, from: String, to: String): Boolean =
        File(from).exists() && runRootAction(context, "cp -afv ${shellQuote(from)} ${shellQuote(to)}")

    fun isPackageInstalled(context: Context, packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun findPartition(context: Context, partitionName: String, slot: String): String? {
        val normalizedSlot = DeviceProfile.normalizeSlot(slot)
        if (normalizedSlot.isBlank() || !partitionName.matches(Regex("[A-Za-z0-9_.-]+"))) return null
        val name = "$partitionName$normalizedSlot"
        val command = "for base in /dev/block/by-name /dev/block/bootdevice/by-name /dev/block/platform/*/by-name /dev/block/platform/*/*/by-name /dev/block/platform/*/*/*/by-name; do path=\"\$base/$name\"; if [ -e \"\$path\" ]; then printf '%s' \"\$path\"; break; fi; done"
        return runRootCommand(context, command)?.trim()?.ifBlank { null }
    }

    fun hasPartition(context: Context, partitionName: String, slot: String): Boolean =
        hasPServer() && findPartition(context, partitionName, slot) != null

    fun installThorToolsMagiskModule(context: Context): Boolean {
        val modulePath = "$MODULE_DIR/thortools"
        val sourcePath = "${context.filesDir}/app/support/magisk/thortools"
        return runRootAction(context, "mkdir -p $modulePath") &&
            runRootAction(context, "cp -fR \"$sourcePath/.\" \"$modulePath/\"")
    }

    fun isThorToolsMagiskModuleInstalled(context: Context): Boolean =
        checkFileExistsRoot(context, "$MODULE_DIR/thortools/module.prop")

    fun startActivityRoot(context: Context, activity: String): Boolean = runRootAction(context, "am start -n $activity")

    fun setAnimationSpeed(context: Context, animationSpeed: Float): Boolean =
        runRootAction(context, "settings put global window_animation_scale $animationSpeed") &&
            runRootAction(context, "settings put global transition_animation_scale $animationSpeed") &&
            runRootAction(context, "settings put global animator_duration_scale $animationSpeed")

    fun setDpi(context: Context, dpi: Int): Boolean = runRootAction(context, "wm density $dpi")

    fun resetDpi(context: Context): Boolean = runRootAction(context, "resetprop -p ro.sf.lcd_density")
}
