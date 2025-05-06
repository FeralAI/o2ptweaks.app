package com.feralai.o2ptweaks.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import android.widget.Toast
import androidx.core.net.toUri
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.URL


@Serializable
class DownloadItem {
    var name: String = ""
    var description: String = ""
    var version: String = ""
    var packageName: String = ""
    var fileUrl: String = ""
    var apkName: String = ""
    var releaseDate: String = ""
}

@Serializable
class DownloadInfo {
    var lastUpdate: String = ""
    var apps: MutableList<DownloadItem> = mutableListOf()
    var files: MutableList<DownloadItem> = mutableListOf()
}

object DownloadUtils {
    fun downloadFile(url: String, downloadPath: String, filename: String? = null): String {
        val gfgPolicy = ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(gfgPolicy)

        try {
            val request = URL(url)
            val inputStream: InputStream = request.openStream()
            val outputStream = ByteArrayOutputStream()

            var nextByte = inputStream.read()
            while (nextByte != -1) {
                outputStream.write(nextByte)
                nextByte = inputStream.read()
            }

            val newFile = File("$downloadPath/${filename ?: File(request.path).name}")
            newFile.writeBytes(outputStream.toByteArray())
            return newFile.path
        } catch (e: java.lang.Exception) {
            //e.printStackTrace()
            return ""
        }
    }

    fun downloadFile(context: Context, title: String?, url: String?, filename: String? = null): Pair<String, Long> {
        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val uri = Uri.parse(url)
        val request = DownloadManager.Request(uri)
        val newFilename = filename ?: File(uri.path ?: "").name
        val fullFileName = FileUtils.getPathDownload("/$newFilename")
        val newFile = File(fullFileName)
        if (newFile.exists())
            newFile.delete()

        val dlUri = File(fullFileName).toUri()
        request.setDestinationUri(dlUri)
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
        request.setTitle("O2P Tweaks - Downloading $title")
        Toast.makeText(context, "Downloading $newFilename..", Toast.LENGTH_SHORT).show()

        return Pair(FileUtils.getPathDownload("/$newFilename"), dm.enqueue(request))
    }

    fun getDownloads(context: Context): DownloadInfo? {
        try {
            val downloadText = try {
                URL("https://github.com/FeralAI/o2ptweaks.app/blob/main/downloads.json").readText()
            } catch (_: Exception) {
                val downloadFile = FileUtils.getPathSupportFiles(context, "/downloads.json")
                File(downloadFile).readText()
            }

            val downloadObj = Json.decodeFromString<DownloadInfo>(downloadText)
            return downloadObj
        }
        catch (e: Exception) {
            return null
        }
    }

}
