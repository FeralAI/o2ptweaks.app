package com.feralai.o2ptweaks.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

// context.filesDir
// /data/user/0/com.feralai.o2ptweaks/files

// context.getExternalFilesDir(null)
// /storage/emulated/0/Android/data/com.feralai.o2ptweaks/files

object FileUtils {
    private const val TAG = "FileUtils"

    fun checkFileExists(path: String): Boolean {
        val file = File(path)
        return file.exists()
    }

    fun copyAsset(context: Context, assetFile: String, dstFile: String): Boolean {
        try {
            val inputStream = context.assets.open(assetFile)

            val apkFile = File(dstFile)
            if (apkFile.exists())
                apkFile.delete()

            val outputStream = FileOutputStream(apkFile)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            return apkFile.exists()
        }
        catch (e: IOException) {
            Log.e(TAG, "Error copying asset to Download folder", e)
            return false
        }
    }

    fun copyFile(src: String, dst: String) {
        val srcFile = File(src)
        if (!srcFile.exists())
            return

        val dstFile = File(dst)
        srcFile.copyTo(dstFile, true)
    }

    fun deleteFile(path: String): Boolean {
        if (!checkFileExists(path))
            return false

        val file = File(path)
        return file.delete()
    }

    fun saveFile(path: String, content: String): Boolean {
        val file = File(path)

        try {
            FileOutputStream(file).use {
                it.write(content.toByteArray())
            }

            return true
        }
        catch (e: Exception) {
            return false
        }
    }

    fun getPathDownload(relativePath: String? = null): String {
        return "/storage/emulated/0/Download${relativePath ?: ""}"
    }

    fun getPathBackup(context: Context, relativePath: String? = null): String {
        val path = "${context.getExternalFilesDir(null)}${relativePath}"
        return path
    }

    fun getPathWorking(context: Context, relativePath: String? = null): String {
        val path = "${context.getExternalFilesDir(null)}${relativePath}"
        return path
    }

    fun getPathAppFiles(context: Context, relativePath: String? = null): String {
        val path = "${context.filesDir}/app${relativePath}"
        return path
    }

    fun getPathSupportFiles(context: Context, relativePath: String? = null): String {
        val path = "${context.filesDir}/app/support${relativePath}"
        return path
    }
}