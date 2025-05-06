
package com.feralai.o2ptweaks.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import java.io.BufferedReader
import java.io.InputStreamReader
import java.lang.Math.round

class RuntimeExecResult {
    var stdout: String = ""
    var stderr: String = ""
    var exitCode: Int? = null

    val success: Boolean
        get() = exitCode == 0
}

object SystemUtils {
    fun getKernelVersion(context: Context): String {
        return RootUtils.runRootCommand(context, "cat /proc/version") ?: ""
    }

    fun getPropBuildDate(): String {
        return getProp("ro.build.date")
    }

    fun getPropBuildDisplayId(): String {
        return getProp("ro.build.display.id")
    }

    fun getPropBuildId(): String {
        return getProp("ro.build.id")
    }

    fun getPropDeviceModel(): String {
        return getProp("ro.product.vendor.model")
    }

    fun getPropFotaPlatform(): String {
        return getProp("ro.fota.platform")
    }

    fun getPropLcdDensity(): Int {
        return getProp("ro.sf.lcd_density").toInt()
    }

    fun getPropFirmwareVersion(): String {
        return getProp("ro.fota.version")
    }

    fun getPropSerialNumber(): String {
        return getProp("ro.serialno")
    }

    fun getPropSlot(): String {
        return getProp( "ro.boot.slot_suffix")
    }

    fun getPropVolumeSteps(): Int {
        return getProp("ro.config.media_vol_steps").toIntOrNull() ?: 15
    }

    fun getProp(propName: String): String {
        val result = runCommand("getprop $propName")
        return if (result.success) {
            result.stdout
        } else {
            ""
        }
    }

    fun getSystemBatteryCapacity(context: Context): Int {
        val value = RootUtils.runRootCommand(context, "cat /sys/class/power_supply/battery/charge_full")?.toInt() ?: 0
        return value / 1000
    }

    fun getSystemBatteryChargeCounter(context: Context): Int {
        val value = RootUtils.runRootCommand(context, "cat /sys/class/power_supply/battery/charge_counter")?.toInt() ?: 0
        return value / 1000
    }

    fun getSystemBatteryCapacityFull(context: Context): Int {
        val value = RootUtils.runRootCommand(context, "cat /sys/class/power_supply/battery/charge_full_design")?.toInt() ?: 0
        return value / 1000
    }

    fun getSystemBatteryHealthPercent(context: Context): Int {
        val value = getSystemBatteryCapacity(context)
        val max = getSystemBatteryCapacityFull(context)
        return (round(value / max.toFloat()) * 100)
    }

    fun getSystemBatteryHealthLabel(context: Context): String {
        val value = RootUtils.runRootCommand(context, "cat /sys/class/power_supply/battery/health") ?: ""
        return value
    }

    fun getSystemBatteryPercent(context: Context): Float? {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let {
            context.registerReceiver(null, it)
        }

        val batteryPct: Float? = batteryStatus?.let { intent ->
            val level: Int = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale: Int = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            level * 100 / scale.toFloat()
        }

        return batteryPct
    }

    fun runCommand(command: String): RuntimeExecResult {
        var process: Process? = null
        val result = RuntimeExecResult()

        return try {
            process = Runtime.getRuntime().exec(command)

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            result.stdout = stdoutReader.readText().trim()
            result.stderr = stderrReader.readText().trim()
            result.exitCode = process.waitFor()

            result
        } catch (t: Throwable) {
            result
        } finally {
            process?.destroy()
        }
    }

    fun runCommand(command: Array<String>): RuntimeExecResult {
        var process: Process? = null
        val result = RuntimeExecResult()

        return try {
            process = Runtime.getRuntime().exec(command)

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            result.stdout = stdoutReader.readText().trim()
            result.stderr = stderrReader.readText().trim()
            result.exitCode = process.waitFor()

            result
        } catch (t: Throwable) {
            result
        } finally {
            process?.destroy()
        }
    }

}
