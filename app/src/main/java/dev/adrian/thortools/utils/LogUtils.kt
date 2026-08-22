package dev.adrian.thortools.utils

import android.content.Context
import android.util.Log
import java.io.File

private const val TAG = "ThorToolsLog"

fun getLogFile(context: Context): File? {
    val dir = context.getExternalFilesDir(null)
    if (dir == null) {
        Log.d(TAG, "Unable to obtain the app recovery directory")
        return null
    }

    if (!dir.exists() && !dir.mkdirs()) {
        Log.d(TAG, "Unable to create the app recovery directory")
        return null
    }

    return File(dir, "lastlog.txt")
}
