package dev.adrian.thortools

import android.content.Context
import android.content.SharedPreferences
import dev.adrian.thortools.utils.FileUtils
import dev.adrian.thortools.utils.RootUtils

object AppSettings {
    const val ANIMATION_SPEED_DEFAULT = 1.0f
    const val DPI_MIN = 290
    const val DPI_MAX = 400
    const val VOLUME_STEPS_MIN = 10
    const val VOLUME_STEPS_MAX = 50
    const val VOLUME_STEPS_DEFAULT = 15
    const val PREFS_NAME = "ThorToolsPrefs"
    const val APP_FIRST_RUN_KEY = "appFirstRun"
    const val ANIMATIONS_SPEED_KEY = "animationSpeed"
    const val DPI_KEY = "overrideDpi"
    const val SKIP_BOOT_ANIMATION_KEY = "skipBootAnimation"
    const val VOLUME_STEPS_KEY = "volumeSteps"
    const val PROP_LCD_DENSITY_KEY = "propLcdDensity"
    const val PROP_VOLUME_STEPS_KEY = "propMediaVolSteps"
    const val JOURNAL_OPERATION_KEY = "journalOperation"
    const val JOURNAL_MESSAGE_KEY = "journalMessage"

    fun getSharedPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(context: Context): Boolean {
        val sharedPrefs = getSharedPrefs(context)
        val properties = buildString {
            val dpi = getDpi(sharedPrefs)
            val volumeSteps = getVolumeSteps(sharedPrefs)
            if (dpi > 0) appendLine("ro.sf.lcd_density=$dpi")
            if (volumeSteps > 0) appendLine("ro.config.media_vol_steps=$volumeSteps")
            if (getSkipBootAnimation(sharedPrefs)) appendLine("debug.sf.nobootanimation=1")
        }
        val propFile = FileUtils.getPathSupportFiles(context, "/magisk/thortools/system.prop")
        if (!FileUtils.saveFile(propFile, properties)) return false
        return RootUtils.installThorToolsMagiskModule(context)
    }

    fun getPropLcdDensity(sharedPrefs: SharedPreferences, defaultValue: Int = 0): Int =
        sharedPrefs.getInt(PROP_LCD_DENSITY_KEY, defaultValue)

    fun setPropLcdDensity(sharedPrefs: SharedPreferences, value: Int) {
        if (getPropLcdDensity(sharedPrefs) == 0) {
            sharedPrefs.edit().putInt(PROP_LCD_DENSITY_KEY, value).apply()
        }
    }

    fun getPropVolumeSteps(sharedPrefs: SharedPreferences, defaultValue: Int = 0): Int =
        sharedPrefs.getInt(PROP_VOLUME_STEPS_KEY, defaultValue)

    fun setPropVolumeSteps(sharedPrefs: SharedPreferences, value: Int) {
        if (getPropVolumeSteps(sharedPrefs) == 0) {
            sharedPrefs.edit().putInt(PROP_VOLUME_STEPS_KEY, value).apply()
        }
    }

    fun getAppFirstRun(sharedPrefs: SharedPreferences, defaultValue: Boolean = false): Boolean =
        sharedPrefs.getBoolean(APP_FIRST_RUN_KEY, defaultValue)

    fun setAppFirstRun(sharedPrefs: SharedPreferences, value: Boolean) {
        sharedPrefs.edit().putBoolean(APP_FIRST_RUN_KEY, value).apply()
    }

    fun getAnimationSpeed(sharedPrefs: SharedPreferences, defaultValue: Float = ANIMATION_SPEED_DEFAULT): Float =
        sharedPrefs.getFloat(ANIMATIONS_SPEED_KEY, defaultValue)

    fun setAnimationSpeed(sharedPrefs: SharedPreferences, value: Float) {
        sharedPrefs.edit().putFloat(ANIMATIONS_SPEED_KEY, value.coerceIn(0f, 1f)).commit()
    }

    fun getDpi(sharedPrefs: SharedPreferences, defaultValue: Int? = null): Int =
        sharedPrefs.getInt(DPI_KEY, defaultValue ?: getPropLcdDensity(sharedPrefs))

    fun setDpi(sharedPrefs: SharedPreferences, value: Int) {
        sharedPrefs.edit().putInt(DPI_KEY, value.coerceIn(DPI_MIN, DPI_MAX)).commit()
    }

    fun getVolumeSteps(sharedPrefs: SharedPreferences, defaultValue: Int = VOLUME_STEPS_DEFAULT): Int =
        sharedPrefs.getInt(VOLUME_STEPS_KEY, defaultValue)

    fun setVolumeSteps(sharedPrefs: SharedPreferences, value: Int) {
        sharedPrefs.edit().putInt(VOLUME_STEPS_KEY, value.coerceIn(VOLUME_STEPS_MIN, VOLUME_STEPS_MAX)).commit()
    }

    fun getSkipBootAnimation(sharedPrefs: SharedPreferences): Boolean =
        sharedPrefs.getBoolean(SKIP_BOOT_ANIMATION_KEY, false)

    fun setSkipBootAnimation(sharedPrefs: SharedPreferences, value: Boolean) {
        sharedPrefs.edit().putBoolean(SKIP_BOOT_ANIMATION_KEY, value).commit()
    }

    var allowRootScreen = false
    var needsReboot = false
}
