package com.feralai.o2ptweaks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ApkInstaller {

    private const val TAG = "ApkInstaller"

    fun installApkFromAssets(context: Context, assetFileName: String, subfolder: String? = null): Boolean {
        try {
            val inputStream = if (subfolder != null) {
                context.assets.open("$subfolder/$assetFileName")
            } else {
                context.assets.open(assetFileName)
            }

            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                Log.e(TAG, "Impossibile creare la directory Download")
                return false
            }

            val apkFile = File(downloadsDir, assetFileName)
            val outputStream = FileOutputStream(apkFile)

            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            val intent = Intent(Intent.ACTION_VIEW)
            val apkUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
            } else {
                Uri.fromFile(apkFile)
            }
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) // Importante per Android 7.0+
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Necessario se chiamato da un contesto non Activity

            context.startActivity(intent)

            return true
        } catch (e: IOException) {
            Log.e(TAG, "Errore durante l'installazione dell'APK", e)
            return false
        }
    }
}

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

object JdspUtils {

    private const val TAG = "JdspUtils"

    val subfolder = "app"

    fun installJdsp(context:Context) {
        // extract assets/$subfolder/ under applicazione/files/
        // which means into in context.filesDir
        var apkname = "JamesDSPManagerThePBone.apk"
        ApkInstaller.installApkFromAssets(context, apkname, subfolder)
    }


    fun enableJdsp(context: Context) {
        // extract assets/$subfolder/ under application/files/
        // which means into in context.filesDir
        copyAssetFolderToFilesDir(context, subfolder)

        // filespath is: /data/user/0/com.feralai.o2ptweaks/files/$subfolder/
        val filespath = File(context.filesDir, subfolder).absolutePath.toString()

        // logpath is: /storage/emulated/0/app.name/files/lostlog.txt
        val logpath = getLogFile(context)

        var cmd = "sh " + filespath + "/support/subscripts/jdsp.setup.sh" + " " + filespath + " > " + logpath
        Log.d(TAG, "enabling jdsp with cmd= " + cmd)

        //execute it:
        val rootexec = RootExec() // get instance
        val result = rootexec.executeAsRoot(cmd)
    }

    fun disableJdsp(context: Context) {
        // extract assets/$subfolder/ under applicazione/files/
        // which means into in context.filesDir
        val subfolder = "app"
        copyAssetFolderToFilesDir(context, subfolder)

        // filespath is: /data/user/0/com.feralai.o2ptweaks/files/$subfolder/
        val filespath = File(context.filesDir, subfolder).absolutePath.toString()

        // logpath is: /storage/emulated/0/app.name/files/lostlog.txt
        val logpath = getLogFile(context)

        var cmd = "sh " + filespath + "/support/subscripts/jdsp.cleanup.sh" + " " + filespath + " > " + logpath
        Log.d(TAG, "enabling jdsp with cmd= " + cmd)

        //execute it:
        val rootexec = RootExec() // get instance
        val result = rootexec.executeAsRoot(cmd)
    }
}