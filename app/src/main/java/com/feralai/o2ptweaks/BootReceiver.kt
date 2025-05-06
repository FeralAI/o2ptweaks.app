package com.feralai.o2ptweaks

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.feralai.o2ptweaks.utils.JdspUtils
import com.feralai.o2ptweaks.utils.RootUtils

fun getApplicationName(context: Context): String {
    val applicationInfo = context.applicationInfo
    val stringId = applicationInfo.labelRes
    return if (stringId == 0) applicationInfo.nonLocalizedLabel.toString() else context.getString(stringId)
}

class BootReceiver : BroadcastReceiver() {

    private val tag = "BootReceiver"

    override fun onReceive(context: Context, intent: Intent) {
        if (!RootUtils.hasPServer())
            return

        Log.d(tag, "BootReceiver.onReceive() called")

        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            Log.d(tag, "Received BOOT_COMPLETED or LOCKED_BOOT_COMPLETED intent")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                    Log.d(tag, "Permission POST_NOTIFICATIONS not granted")
                    return
                }
            }

            val sharedPrefs = AppSettings.getSharedPrefs(context)

            // Set DPI
            val dpi = AppSettings.getDpi(sharedPrefs)
            RootUtils.setDpi(context, dpi)

            // Set animation speed
            val animSpeed = AppSettings.getAnimationSpeed(sharedPrefs)
            RootUtils.setAnimationSpeed(context, animSpeed)

            if (!RootUtils.isDeviceRooted) {
                // Enable JDSP at boot?
                val jdspEnabled = AppSettings.getJdspEnabled(sharedPrefs)
                if (jdspEnabled) {
                    createNotificationChannel(context)
                    showNotification(context)
                    Log.d(tag, "Enabling JamesDSP at boot...")
                    JdspUtils.enableJdsp(context)
                } else {
                    Log.d(tag, "Not enabling JamesDSP at boot...")
                }

                // Enable O2P volume fix?
                val o2pVolumeFix = AppSettings.getO2PVolumeFix(sharedPrefs)
                if (o2pVolumeFix) {
                    createNotificationChannel(context)
                    showNotification(context)
                    Log.d(tag, "Enabling O2P volume fix at boot...")
                    RootUtils.enableO2PVolumeFix(context)
                } else {
                    Log.d(tag, "Not enabling O2P volume fix at boot...")
                }
            }
        } else {
            Log.d(tag, "Intent received: ${intent.action}")
        }
    }

    private fun createNotificationChannel(context: Context) {
        val channelId = "boot_channel"
        val name = "Boot Notification Channel"
        val descriptionText = "Channel for boot notifications"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(channelId, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        Log.d(tag, "Notify channel created")
    }

    private fun showNotification(context: Context) {
        val builder = NotificationCompat.Builder(context, "boot_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getApplicationName(context))
            .setContentText("App started${if (RootUtils.isDeviceRooted) " (ROOT MODE)" else "" }")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            with(NotificationManagerCompat.from(context)) {
                val notificationId = 123
                notify(notificationId, builder.build())
                Log.d(tag, "Notifica mostrata con ID: $notificationId")
            }
        } else {
            Log.d(tag, "Unable to show notification it seems permission POST_NOTIFICATIONS is missing")
        }
    }
}