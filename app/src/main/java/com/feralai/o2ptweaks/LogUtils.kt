package com.feralai.o2ptweaks

import android.content.Context
import android.util.Log
import java.io.File

fun getLogFile(context: Context): File? {
    val TAG = "LogUtil" // Definizione della variabile tag
    val dir = context.getExternalFilesDir(null)
    if (dir == null) {
        Log.d(TAG, "Impossibile ottenere la directory dei documenti")
        return null
    }

    if (!dir.exists() && !dir.mkdirs()) {
        Log.d(TAG, "Impossibile creare la directory dei documenti")
        return null
    }

    return File(dir, "lastlog.txt")
}