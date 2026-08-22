package dev.adrian.thortools.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FileUtils {
    private const val TAG = "FileUtils"

    fun checkFileExists(path: String): Boolean {
        val file = File(path)
        return file.exists()
    }

    fun copyAsset(context: Context, assetFile: String, dstFile: String): Boolean {
        return try {
            val apkFile = File(dstFile)
            if (apkFile.exists()) apkFile.delete()
            context.assets.open(assetFile).use { input ->
                FileOutputStream(apkFile).use { output -> input.copyTo(output) }
            }
            apkFile.isFile
        } catch (e: IOException) {
            Log.e(TAG, "Error copying asset to Download folder", e)
            false
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

        return try {
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { it.write(content.toByteArray()) }
            file.isFile
        } catch (_: Exception) {
            false
        }
    }

    fun getPathDownload(relativePath: String? = null): String {
        return "/storage/emulated/0/Download${relativePath ?: ""}"
    }

    fun getPathBackup(context: Context, relativePath: String? = null): String {
        return appendPath(context.getExternalFilesDir(null), relativePath)
    }

    fun isBackupDestinationWritable(context: Context): Boolean {
        val directory = context.getExternalFilesDir(null) ?: return false
        if (!directory.exists() && !directory.mkdirs()) return false
        return directory.isDirectory && directory.canWrite()
    }

    fun getPathWorking(context: Context, relativePath: String? = null): String {
        return appendPath(context.getExternalFilesDir(null), relativePath)
    }

    fun getPathAppFiles(context: Context, relativePath: String? = null): String {
        return appendPath(File(context.filesDir, "app"), relativePath)
    }

    fun getPathSupportFiles(context: Context, relativePath: String? = null): String {
        return appendPath(File(context.filesDir, "app/support"), relativePath)
    }

    private fun appendPath(base: File?, relativePath: String?): String {
        if (base == null) return ""
        val relative = relativePath?.trimStart('/') ?: return base.absolutePath
        return File(base, relative).absolutePath
    }
}
