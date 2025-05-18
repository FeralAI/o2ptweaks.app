package com.feralai.o2ptweaks

import android.app.Application
import com.feralai.o2ptweaks.utils.RootUtils
import com.feralai.o2ptweaks.utils.SystemUtils
import com.feralai.o2ptweaks.utils.copyAssetFolderToFilesDir

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        val context = super.getApplicationContext()
        val sharedPrefs = AppSettings.getSharedPrefs(context)
        AppSettings.setPropLcdDensity(sharedPrefs, SystemUtils.getPropLcdDensity())
        AppSettings.setPropVolumeSteps(sharedPrefs, SystemUtils.getPropVolumeSteps())

        // Ensure app files are in place
        copyAssetFolderToFilesDir(context, "app")
    }
}