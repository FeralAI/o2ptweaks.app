package dev.adrian.thortools

import android.app.Application
import dev.adrian.thortools.utils.SystemUtils
import dev.adrian.thortools.utils.copyAssetFolderToFilesDir

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = AppSettings.getSharedPrefs(this)
        AppSettings.setPropLcdDensity(prefs, SystemUtils.getPropLcdDensity())
        AppSettings.setPropVolumeSteps(prefs, SystemUtils.getPropVolumeSteps())
        copyAssetFolderToFilesDir(this, "app")
    }
}
