package com.feralai.o2ptweaks.utils

import android.content.Context
import android.content.Intent
import android.os.Environment
import android.util.Log
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ApkUtils {
    private const val TAG = "ApkInstaller"

    fun installApk(context: Context, filename: String, onComplete: () -> Unit = {}): Boolean {
        try {
            val apkFile = File(filename)
            if (!apkFile.exists())
                return false

            val intent = Intent(Intent.ACTION_VIEW)
            val apkUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
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

    fun installApkFromAssets(context: Context, assetFileName: String, subfolder: String? = null, onComplete: () -> Unit = {}): Boolean {
        try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
                Log.e(TAG, "Cannot access Download folder")
                return false
            }

            val assetPath = if (subfolder != null) {
                "$subfolder/$assetFileName"
            } else {
                assetFileName
            }

            val apkFile = File(downloadsDir, assetFileName)
            FileUtils.copyAsset(context, assetPath, apkFile.path)
            return installApk(context, apkFile.path)
        } catch (e: IOException) {
            Log.e(TAG, "Errore durante l'installazione dell'APK", e)
            return false
        }
    }
}