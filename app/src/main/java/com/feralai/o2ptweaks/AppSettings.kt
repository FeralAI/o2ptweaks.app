package com.feralai.o2ptweaks

import android.content.Context
import android.content.SharedPreferences
import com.feralai.o2ptweaks.utils.FileUtils
import com.feralai.o2ptweaks.utils.RootUtils

object AppSettings {
    const val ANIMIATION_SPEED_DEFAULT = 1.0F

    const val DPI_MIN = 290 // Below 290 engages tablet mode UI on a lot of devices
    const val DPI_MAX = 400

    const val VOLUME_STEPS_MIN = 10
    const val VOLUME_STEPS_MAX = 50
    const val VOLUME_STEPS_DEFAULT = 15

    const val PREFS_NAME = "O2PTweaksPrefs"
    const val APP_FIRST_RUN_KEY = "appFirstRun"
    const val ANIMATIONS_SPEED_KEY = "animationSpeed"
    const val DPI_KEY = "overrideDpi"
    const val JDSP_ENABLED_KEY = "jdspEnabled"
    const val O2P_VOLUME_FIX_KEY = "o2pSpeakerVolumePatch"
    const val VOLUME_STEPS_KEY = "volumeSteps"

    const val PROP_LCD_DENSITY_KEY = "propLcdDensity"
    const val PROP_VOLUME_STEPS_KEY = "propMediaVolSteps"

    fun getSharedPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun save(context: Context) {
        if (!RootUtils.isDeviceRooted)
            return

        // Save off system.prop to the Magisk module folder
        val sharedPrefs = getSharedPrefs(context)
        val dpi = getDpi(sharedPrefs)
        val volSteps = getVolumeSteps(sharedPrefs)

        val sb = StringBuilder()
        if (dpi > 0) { sb.appendLine("ro.sf.lcd_density=$dpi") }
        if (volSteps > 0) { sb.appendLine("ro.config.media_vol_steps=$volSteps") }

        val propFile = "${context.filesDir}/app/support/magisk/odin2portal-tweaks/system.prop"
        FileUtils.saveFile(propFile, sb.toString())
        RootUtils.installO2PTweaksMagiskModule(context)

        if (getO2PVolumeFix(sharedPrefs))
            RootUtils.installO2PVolumeFixMagisk(context)
        else
            RootUtils.uninstallO2PVolumeFixMagisk(context)
    }

    //<editor-fold desc="Cached System Props">

    fun getPropLcdDensity(sharedPrefs: SharedPreferences, defaultValue: Int = 0): Int {
        return sharedPrefs.getInt(PROP_LCD_DENSITY_KEY, defaultValue)
    }
    fun setPropLcdDensity(sharedPrefs: SharedPreferences, value: Int) {
        val density = getPropLcdDensity(sharedPrefs)
        if (density == 0) {
            with(sharedPrefs.edit()) {
                putInt(PROP_LCD_DENSITY_KEY, value)
                apply()
            }
        }
    }

    fun getPropVolumeSteps(sharedPrefs: SharedPreferences, defaultValue: Int = 0): Int {
        return sharedPrefs.getInt(PROP_VOLUME_STEPS_KEY, defaultValue)
    }
    fun setPropVolumeSteps(sharedPrefs: SharedPreferences, value: Int) {
        val steps = getPropVolumeSteps(sharedPrefs)
        if (steps == 0) {
            with(sharedPrefs.edit()) {
                putInt(PROP_VOLUME_STEPS_KEY, value)
                apply()
            }
        }
    }

    //</editor-fold>

    //<editor-fold desc="App Settings Values">

    fun getAppFirstRun(sharedPrefs: SharedPreferences, defaultValue: Boolean = false): Boolean {
        return sharedPrefs.getBoolean(APP_FIRST_RUN_KEY, defaultValue)
    }
    fun setAppFirstRun(sharedPrefs: SharedPreferences, value: Boolean) {
        with(sharedPrefs.edit()) {
            putBoolean(APP_FIRST_RUN_KEY, value)
            apply()
        }
    }

    fun getAnimationSpeed(sharedPrefs: SharedPreferences, defaultValue: Float = ANIMIATION_SPEED_DEFAULT): Float {
        return sharedPrefs.getFloat(ANIMATIONS_SPEED_KEY, defaultValue)
    }
    fun setAnimationSpeed(sharedPrefs: SharedPreferences, value: Float) {
        var newValue = value
        if (newValue < 0.0f) newValue = 0.0f
        if (newValue > 1.0f) newValue = 1.0f

        with(sharedPrefs.edit()) {
            putFloat(ANIMATIONS_SPEED_KEY, newValue)
            apply()
        }
    }

    fun getDpi(sharedPrefs: SharedPreferences, defaultValue: Int? = null): Int {
        return sharedPrefs.getInt(DPI_KEY, defaultValue ?: getPropLcdDensity(sharedPrefs))
    }
    fun setDpi(sharedPrefs: SharedPreferences, value: Int) {
        var newValue = value
        if (newValue < DPI_MIN) newValue = DPI_MIN
        if (newValue > DPI_MAX) newValue = DPI_MAX

        with(sharedPrefs.edit()) {
            putInt(DPI_KEY, newValue)
            apply()
        }
    }

    fun getVolumeSteps(sharedPrefs: SharedPreferences, defaultValue: Int = VOLUME_STEPS_DEFAULT): Int {
        return sharedPrefs.getInt(VOLUME_STEPS_KEY, defaultValue)
    }
    fun setVolumeSteps(sharedPrefs: SharedPreferences, value: Int) {
        var newValue = value
        if (newValue < VOLUME_STEPS_MIN) newValue = VOLUME_STEPS_MIN
        if (newValue > VOLUME_STEPS_MAX) newValue = VOLUME_STEPS_MAX

        with(sharedPrefs.edit()) {
            putInt(VOLUME_STEPS_KEY, newValue)
            apply()
        }
    }

    fun getJdspEnabled(sharedPrefs: SharedPreferences): Boolean {
        return sharedPrefs.getBoolean(JDSP_ENABLED_KEY, false)
    }
    fun setJdspEnabled(sharedPrefs: SharedPreferences, value: Boolean) {
        with(sharedPrefs.edit()) {
            putBoolean(JDSP_ENABLED_KEY, value)
            apply()
        }
    }

    fun getO2PVolumeFix(sharedPrefs: SharedPreferences): Boolean {
        return sharedPrefs.getBoolean(O2P_VOLUME_FIX_KEY, false)
    }
    fun setO2PVolumeFix(sharedPrefs: SharedPreferences, value: Boolean) {
        with(sharedPrefs.edit()) {
            putBoolean(O2P_VOLUME_FIX_KEY, value)
            apply()
        }
    }

    //</editor-fold>

    //<editor-fold desc="App Instance Variables">

    var allowRootScreen = false
    var needsReboot = false

    //</editor-fold>
}