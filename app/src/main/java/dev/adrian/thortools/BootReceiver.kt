package dev.adrian.thortools

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dev.adrian.thortools.utils.RootUtils

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_LOCKED_BOOT_COMPLETED) return
        if (!RootUtils.hasPServer() && !RootUtils.isDeviceRooted) return
        val prefs = AppSettings.getSharedPrefs(context)
        RootUtils.setDpi(context, AppSettings.getDpi(prefs))
        RootUtils.setAnimationSpeed(context, AppSettings.getAnimationSpeed(prefs))
        if (RootUtils.isDeviceRooted) AppSettings.save(context)
    }
}
