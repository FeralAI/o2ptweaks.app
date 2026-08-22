package dev.adrian.thortools.utils

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import dev.adrian.thortools.DeviceProfile
import dev.adrian.thortools.DeviceProperties
import java.io.BufferedReader
import java.io.InputStreamReader

class RuntimeExecResult(
    var stdout: String = "",
    var stderr: String = "",
    var exitCode: Int? = null,
) {
    val success: Boolean
        get() = exitCode == 0
}

object SystemUtils {
    fun getDeviceProperties(): DeviceProperties = DeviceProperties(
        manufacturer = getProp("ro.product.manufacturer"),
        brand = getProp("ro.product.brand"),
        model = getProp("ro.product.model").ifBlank { getProp("ro.product.vendor.model") },
        device = getProp("ro.product.device"),
        product = getProp("ro.product.name"),
        board = getProp("ro.product.board"),
        hardware = getProp("ro.hardware").ifBlank { getProp("ro.boot.hardware") },
        soc = getProp("ro.soc.model").ifBlank { getProp("ro.board.platform") },
        platform = getProp("ro.fota.platform"),
        firmware = getProp("ro.fota.version").ifBlank { getProp("ro.build.version.release") },
        buildId = getProp("ro.build.id"),
        buildDisplayId = getProp("ro.build.display.id"),
        buildDate = getProp("ro.build.date"),
        serial = getProp("ro.serialno"),
        slot = DeviceProfile.normalizeSlot(
            getProp("ro.boot.slot_suffix").ifBlank { getProp("ro.boot.slot") },
        ),
    )

    fun getKernelVersion(context: Context): String =
        RootUtils.runRootCommand(context, "cat /proc/version") ?: runCommand(arrayOf("cat", "/proc/version")).stdout

    fun getPropBuildDate(): String = getProp("ro.build.date")
    fun getPropBuildDisplayId(): String = getProp("ro.build.display.id")
    fun getPropBuildId(): String = getProp("ro.build.id")
    fun getPropDeviceModel(): String = getDeviceProperties().model
    fun getPropFotaPlatform(): String = getProp("ro.fota.platform")
    fun getPropLcdDensity(): Int = getProp("ro.sf.lcd_density").toIntOrNull() ?: 0
    fun getPropFirmwareVersion(): String = getDeviceProperties().firmware
    fun getPropSerialNumber(): String = getProp("ro.serialno")
    fun getPropSlot(): String = getDeviceProperties().slot
    fun getPropVolumeSteps(): Int = getProp("ro.config.media_vol_steps").toIntOrNull() ?: 15

    fun getProp(propName: String): String = runCommand(arrayOf("getprop", propName)).stdout

    fun getSystemBatteryCapacity(context: Context): Int =
        (RootUtils.runRootCommand(context, "cat /sys/class/power_supply/battery/charge_full")?.toIntOrNull() ?: 0) / 1000

    fun getSystemBatteryChargeCounter(context: Context): Int =
        (RootUtils.runRootCommand(context, "cat /sys/class/power_supply/battery/charge_counter")?.toIntOrNull() ?: 0) / 1000

    fun getSystemBatteryCapacityFull(context: Context): Int =
        (RootUtils.runRootCommand(context, "cat /sys/class/power_supply/battery/charge_full_design")?.toIntOrNull() ?: 0) / 1000

    fun getSystemBatteryHealthPercent(context: Context): Int {
        val current = getSystemBatteryCapacity(context)
        val maximum = getSystemBatteryCapacityFull(context)
        return if (maximum > 0) (current * 100 / maximum).coerceIn(0, 100) else 0
    }

    fun getSystemBatteryHealthLabel(context: Context): String =
        RootUtils.runRootCommand(context, "cat /sys/class/power_supply/battery/health") ?: "unknown"

    fun getBatteryPercent(context: Context): Int? {
        val batteryStatus = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
        val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level >= 0 && scale > 0) (level * 100 / scale).coerceIn(0, 100) else null
    }

    fun runCommand(command: Array<String>): RuntimeExecResult {
        var process: Process? = null
        return try {
            process = Runtime.getRuntime().exec(command)
            val stdout = BufferedReader(InputStreamReader(process.inputStream)).readText().trim()
            val stderr = BufferedReader(InputStreamReader(process.errorStream)).readText().trim()
            val exitCode = process.waitFor()
            RuntimeExecResult(stdout, stderr, exitCode)
        } catch (_: Throwable) {
            RuntimeExecResult()
        } finally {
            process?.destroy()
        }
    }
}
