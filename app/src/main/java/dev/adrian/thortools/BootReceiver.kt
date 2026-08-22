package dev.adrian.thortools

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.adrian.thortools.utils.RootUtils
import dev.adrian.thortools.utils.SystemUtils
import kotlin.concurrent.thread

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val pendingResult = goAsync()
        thread(name = "ThorToolsBoot") {
            try {
                if (!DeviceProfile.detect(SystemUtils.getDeviceProperties()).isThor || !RootUtils.hasPServer()) return@thread
                val prefs = AppSettings.getSharedPrefs(context)
                val dpi = AppSettings.getDpi(prefs)
                if (dpi in AppSettings.DPI_MIN..AppSettings.DPI_MAX) RootUtils.setDpi(context, dpi)
                val animationSpeed = AppSettings.getAnimationSpeed(prefs)
                if (animationSpeed.isFinite() && animationSpeed in 0f..1f) RootUtils.setAnimationSpeed(context, animationSpeed)
                AppSettings.save(context)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
