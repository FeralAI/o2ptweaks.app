package com.feralai.o2ptweaks.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.feralai.o2ptweaks.AppSettings
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader


fun copyAssetFolderToFilesDir(context: Context, assetFolderPath: String) {
    try {
        val assetManager = context.assets

        // Ottieni la lista dei file e delle sottocartelle nella cartella assets specificata
        val assetFiles = assetManager.list(assetFolderPath) ?: return // Se la cartella non esiste, esci

        if (assetFiles.isEmpty()) {
            // È una cartella vuota, creala nella directory dell'app
            val targetDir = File(context.filesDir, assetFolderPath)
            targetDir.mkdirs()
            return
        }

        for (assetFileName in assetFiles) {
            val fullAssetPath = if (assetFolderPath.isEmpty()) assetFileName else "$assetFolderPath/$assetFileName"

            try {
                //Prova ad aprire il file. Se fallisce, significa che è una cartella.
                assetManager.open(fullAssetPath).use {
                    // È un file, copialo
                    val outFile = File(context.filesDir, fullAssetPath)
                    outFile.parentFile?.mkdirs() // Crea le directory parent
                    FileOutputStream(outFile).use { output -> it.copyTo(output) }
                    outFile.setReadable(true)
                    outFile.setExecutable(true)
                }
            } catch (e: IOException) {
                // Gestisci il caso in cui è una sottocartella, richiamando ricorsivamente la funzione
                copyAssetFolderToFilesDir(context, fullAssetPath)
            }
        }
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

object RootUtils {

    const val MODULE_DIR = "/data/adb/modules"
    private const val TAG = "RootUtils"

    val subfolder = "app"

    val isDeviceRooted: Boolean
        get() = checkRootMethod1() || checkRootMethod2() || checkRootMethod3()

    private fun checkRootMethod1(): Boolean {
        val buildTags = Build.TAGS
        return buildTags != null && buildTags.contains("test-keys")
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su",
            "/su/bin/su"
        )
        for (path in paths) {
            if (File(path).exists()) return true
        }
        return false
    }

    private fun checkRootMethod3(): Boolean {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(arrayOf("/system/xbin/which", "su"))
            val `in` = BufferedReader(InputStreamReader(process.inputStream))
            `in`.readLine() != null
        } catch (t: Throwable) {
            false
        } finally {
            process?.destroy()
        }
    }

    fun hasPServer(): Boolean {
        val rootExec = RootExec()
        return rootExec.pServerAvailable
    }

    fun runRootCommand(context: Context, command: String): String? {
        Log.d(TAG, "running root command= $command")

        //execute it:
        val rootexec = RootExec() // get instance
        val result = rootexec.executeAsRoot(command)
        Log.d(TAG, "command finished with result: $result")

        return result.getOrNull();
    }

    fun runRootScript(context: Context, script: String): String? {
        // filespath is: /data/user/0/com.feralai.o2ptweaks/files/$subfolder/
        val filespath = File(context.filesDir, subfolder).absolutePath.toString()

        // logpath is: /storage/emulated/0/app.name/files/lostlog.txt
        val logpath = getLogFile(context)

        val cmd = "sh $filespath/support/subscripts/$script $filespath > $logpath"
        Log.d(TAG, "running root script with cmd= $cmd")

        //execute it:
        val rootexec = RootExec() // get instance
        val result = rootexec.executeAsRoot(cmd)
        Log.d(TAG, "$script finished with result: $result")
        return result.getOrNull();
    }

    fun checkFileExistsRoot(context: Context, path: String): Boolean {
        val cmd = "[ -e \"$path\" ] && echo 1 || echo 0"
        val result = runRootCommand(context, cmd)
        return result == "1"
    }

    fun reboot(context: Context) {
        runRootCommand(context, "reboot")
    }

    fun rootCopy(context: Context, from: String, to: String) {
        val fromFile = File(from)

        if (!fromFile.exists())
            return

        runRootCommand(context, "cp -afv \"$from\" \"$to\"")
    }

    fun isPackageInstalled(context: Context, packageName: String?): Boolean {
        var result = false
        try {
            // is the application installed?
            context.packageManager.getPackageInfo(packageName!!, PackageManager.GET_ACTIVITIES)
            result = true
        } catch (e: PackageManager.NameNotFoundException) {
            //Not installed
        }
        return result
    }

    fun installO2PTweaksMagiskModule(context: Context) {
        val o2pModulePath = "$MODULE_DIR/odin2portal-tweaks/"
        runRootCommand(context, "rm -rf $o2pModulePath")
        runRootCommand(context, "mkdir -p $o2pModulePath")
        runRootCommand(context, "cp -fR ${context.filesDir}/app/support/magisk/odin2portal-tweaks/*.prop $o2pModulePath")
    }

    fun installO2PVolumeFixMagisk(context: Context) {
        val volumeFixPath = "$MODULE_DIR/odin2portal-tweaks/system/vendor/etc"
        runRootCommand(context, "mkdir -p $volumeFixPath")

        val volumeFixFilePath = "$volumeFixPath/default_volume_tables.xml"
        runRootCommand(context, "cp -f ${context.filesDir}/app/support/magisk/odin2portal-tweaks/system/vendor/etc/default_volume_tables.off $volumeFixFilePath")
    }

    fun uninstallO2PVolumeFixMagisk(context: Context) {
        val volumeFixFilePath = "$MODULE_DIR/odin2portal-tweaks/system/vendor/etc/default_volume_tables.xml"
        runRootCommand(context, "rm -f $volumeFixFilePath")
    }

    fun enableO2PVolumeFix(context: Context) {
        if (isDeviceRooted) {
            installO2PVolumeFixMagisk(context)
        }
        else {
            runRootScript(context, "o2pvf.enable.sh")
        }
    }

    fun disableO2PVolumeFix(context: Context) {
        if (isDeviceRooted) {
            uninstallO2PVolumeFixMagisk(context)
        }
        else {
            runRootScript(context, "o2pvf.disable.sh")
        }
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