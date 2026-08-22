package dev.adrian.thortools.screens

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.adrian.thortools.AppSettings
import dev.adrian.thortools.DeviceProfile
import dev.adrian.thortools.OperationStatus
import dev.adrian.thortools.ThorCapability
import dev.adrian.thortools.ThorOperation
import dev.adrian.thortools.ThorOperationGuard
import dev.adrian.thortools.ThorSession
import dev.adrian.thortools.ThorSnapshot
import dev.adrian.thortools.ThorVariant
import dev.adrian.thortools.utils.PatchUtils
import dev.adrian.thortools.utils.getLogFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class ThorSection {
    STATUS,
    TWEAKS,
    ROOT,
    ABOUT,
}

@Composable
fun ThorControlScreen(
    session: ThorSession,
    context: Context,
    isLowerDisplay: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    var section by remember { mutableStateOf(ThorSection.STATUS) }
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            ControlHeader(session.snapshot, section) { section = it }
            when (section) {
                ThorSection.STATUS -> StatusPanel(session.snapshot, context)
                ThorSection.TWEAKS -> TweaksPanel(session, context)
                ThorSection.ROOT -> RootPanel(session, context, isLowerDisplay)
                ThorSection.ABOUT -> AboutPanel(context)
            }
            if (session.snapshot.operation.status == OperationStatus.RUNNING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = session.snapshot.operation.message,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (session.snapshot.operation.status == OperationStatus.INTERRUPTED) {
                TextButton(
                    onClick = { session.acknowledgeInterruptedOperation() },
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp),
                ) {
                    Text("Acknowledge recovery record")
                }
            }
            if (section == ThorSection.STATUS) {
                TextButton(
                    onClick = { scope.launch { session.refresh() } },
                    modifier = Modifier.align(Alignment.End).padding(horizontal = 12.dp),
                ) {
                    Text("Refresh")
                }
            }
        }
    }
}

@Composable
fun ThorDashboardScreen(session: ThorSession, context: Context, modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("ThorTools", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Upper display dashboard", style = MaterialTheme.typography.titleMedium)
            DashboardIdentity(session.snapshot)
            DashboardCapabilities(session.snapshot)
            DashboardOperations(session.snapshot)
            DashboardOperation(session.snapshot, context)
            if (session.snapshot.stockBackupSlots.isNotEmpty() || session.snapshot.patchedBackupSlots.isNotEmpty()) {
                DashboardHashes(context, session.snapshot)
            }
        }
    }
}

@Composable
private fun ControlHeader(snapshot: ThorSnapshot, section: ThorSection, onSection: (ThorSection) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("ThorTools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                text = if (snapshot.profile.isThor) snapshot.profile.displayName else "Diagnostics only",
                color = if (snapshot.profile.isThor) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            ThorSection.entries.forEach { candidate ->
                if (candidate == section) {
                    Button(onClick = { onSection(candidate) }, modifier = Modifier.weight(1f)) {
                        Text(candidate.label())
                    }
                } else {
                    OutlinedButton(onClick = { onSection(candidate) }, modifier = Modifier.weight(1f)) {
                        Text(candidate.label())
                    }
                }
            }
        }
        HorizontalDivider(modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun StatusPanel(snapshot: ThorSnapshot, context: Context) {
    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        DashboardIdentity(snapshot)
        DashboardCapabilities(snapshot)
        DashboardOperations(snapshot)
        DashboardOperation(snapshot, context)
        if (snapshot.stockBackupSlots.isNotEmpty() || snapshot.patchedBackupSlots.isNotEmpty()) DashboardHashes(context, snapshot)
    }
}

@Composable
private fun DashboardIdentity(snapshot: ThorSnapshot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Device", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            DataLine("Model", snapshot.profile.properties.model)
            DataLine("Variant", snapshot.profile.variant.label())
            DataLine("Manufacturer", snapshot.profile.properties.manufacturer)
            DataLine("Brand", snapshot.profile.properties.brand)
            DataLine("Device", snapshot.profile.properties.device)
            DataLine("Product", snapshot.profile.properties.product)
            DataLine("Board", snapshot.profile.properties.board)
            DataLine("Hardware", snapshot.profile.properties.hardware)
            DataLine("SoC", snapshot.profile.properties.soc)
            DataLine("Platform", snapshot.profile.properties.platform)
            DataLine("Firmware", snapshot.profile.properties.firmware)
            DataLine("Build", snapshot.profile.properties.buildDisplayId.ifBlank { snapshot.profile.properties.buildId })
            DataLine("Build date", snapshot.profile.properties.buildDate)
            DataLine("Build fingerprint", snapshot.profile.properties.buildFingerprint)
            DataLine("Serial", snapshot.profile.properties.serial)
            DataLine("Active slot", snapshot.activeSlot)
            DataLine("Root service", if (snapshot.rootServiceAvailable) "Available" else "Unavailable")
            DataLine("Root state", if (snapshot.rooted) "Rooted" else "Not rooted")
            DataLine("Magisk", if (snapshot.magiskInstalled) "Installed" else "Not installed")
            DataLine("Recovery target", snapshot.recoveryPartition)
            DataLine("Stock backups", "${snapshot.stockBackupSlots.size}/2 slots")
            DataLine("Stock restore source", if (snapshot.stockRestoreAvailable) "Available" else "Unavailable")
            DataLine("Patched backups", "${snapshot.patchedBackupSlots.size}/2 slots")
            DataLine("Battery", if (snapshot.batteryPercent > 0) "${snapshot.batteryPercent}%" else "Unavailable")
            DataLine("Kernel", snapshot.kernelVersion.ifBlank { "Unavailable" })
        }
    }
}

@Composable
private fun DashboardCapabilities(snapshot: ThorSnapshot) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Capabilities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            snapshot.capabilityRows.forEach { (label, available) ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label)
                    Text(if (available) "Ready" else "Unavailable", color = if (available) Color(0xff2e7d32) else MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun DashboardOperations(snapshot: ThorSnapshot) {
    val operations = listOf(
        ThorOperation.BACKUP to "Stock backup",
        ThorOperation.PATCH to "Prepare patch",
        ThorOperation.FLASH to "Flash patch",
        ThorOperation.RESTORE to "Restore stock",
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text("Operation readiness", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            operations.forEach { (operation, label) ->
                val reason = ThorOperationGuard.validate(snapshot, operation)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label)
                    Text(
                        text = if (reason == null) "Ready" else "Blocked",
                        color = if (reason == null) Color(0xff2e7d32) else MaterialTheme.colorScheme.error,
                    )
                }
                if (reason != null) Text(reason, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DashboardOperation(snapshot: ThorSnapshot, context: Context) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Latest operation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (snapshot.operation.status == OperationStatus.RUNNING) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Text(snapshot.operation.status.label())
            Text(snapshot.operation.message)
            Text("Recovery folder: ${context.getExternalFilesDir(null)?.absolutePath ?: "Unavailable"}", style = MaterialTheme.typography.bodySmall)
            Text("Latest operation log: ${getLogFile(context)?.absolutePath ?: "Unavailable"}", style = MaterialTheme.typography.bodySmall)
            Text("Download folder: /storage/emulated/0/Download", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DashboardHashes(context: Context, snapshot: ThorSnapshot) {
    val hashes by produceState<Map<String, String>>(emptyMap(), snapshot.stockBackupSlots, snapshot.patchedBackupSlots, snapshot.operation.status == OperationStatus.RUNNING) {
        value = withContext(Dispatchers.IO) { PatchUtils.imageHashes(context) }
    }
    val paths = remember(snapshot.stockBackupSlots, snapshot.patchedBackupSlots) { PatchUtils.imagePaths(context) }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Image hashes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            hashes.toSortedMap().forEach { (name, hash) ->
                Text(name, fontWeight = FontWeight.Bold)
                Text(hash, style = MaterialTheme.typography.bodySmall)
                Text(paths[name] ?: "Unavailable", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun TweaksPanel(session: ThorSession, context: Context) {
    val scope = rememberCoroutineScope()
    val prefs = AppSettings.getSharedPrefs(context)
    val snapshot = session.snapshot
    var dpi by remember(snapshot.lcdDensity) { mutableStateOf(snapshot.lcdDensity.toFloat().coerceIn(AppSettings.DPI_MIN.toFloat(), AppSettings.DPI_MAX.toFloat())) }
    var volumeSteps by remember(snapshot.volumeSteps) { mutableStateOf(snapshot.volumeSteps.toFloat().coerceIn(AppSettings.VOLUME_STEPS_MIN.toFloat(), AppSettings.VOLUME_STEPS_MAX.toFloat())) }
    var skipBootAnimation by remember { mutableStateOf(AppSettings.getSkipBootAnimation(prefs)) }
    val enabled = snapshot.profile.isThor && snapshot.rootServiceAvailable
    val actionReady = snapshot.operation.status !in setOf(OperationStatus.RUNNING, OperationStatus.INTERRUPTED)
    val moduleReady = actionReady && ThorOperationGuard.validate(snapshot, ThorOperation.SET_VOLUME_STEPS) == null

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Thor system tweaks", style = MaterialTheme.typography.headlineSmall)
        Text("Changes apply to both Thor panels through Android system settings. Reboot when the dashboard requests it.")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Display density: ${dpi.toInt()}")
                Slider(
                    value = dpi,
                    onValueChange = { dpi = it },
                    valueRange = AppSettings.DPI_MIN.toFloat()..AppSettings.DPI_MAX.toFloat(),
                    steps = AppSettings.DPI_MAX - AppSettings.DPI_MIN - 1,
                    enabled = enabled && actionReady,
                    onValueChangeFinished = { session.run(scope, ThorOperation.SET_DPI, dpi.toInt().toString()) },
                )
                Text("Default: ${AppSettings.getPropLcdDensity(prefs)}")
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Animation speed: ${snapshot.animationSpeed}x")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1f, 0.5f, 0f).forEach { value ->
                        OutlinedButton(enabled = enabled && actionReady, onClick = { session.run(scope, ThorOperation.SET_ANIMATION, value.toString()) }) {
                            Text(if (value == 0f) "Off" else "${value}x")
                        }
                    }
                }
            }
        }
        if (moduleReady) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Volume steps: ${volumeSteps.toInt()}")
                    Slider(
                        value = volumeSteps,
                        onValueChange = { volumeSteps = it },
                        valueRange = AppSettings.VOLUME_STEPS_MIN.toFloat()..AppSettings.VOLUME_STEPS_MAX.toFloat(),
                        steps = AppSettings.VOLUME_STEPS_MAX - AppSettings.VOLUME_STEPS_MIN - 1,
                        enabled = enabled && actionReady,
                        onValueChangeFinished = { session.run(scope, ThorOperation.SET_VOLUME_STEPS, volumeSteps.toInt().toString()) },
                    )
                }
            }
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Skip boot animation")
                        Text("Applied through the ThorTools Magisk module after reboot.")
                    }
                    Switch(
                        checked = skipBootAnimation,
                        enabled = enabled && actionReady,
                        onCheckedChange = {
                            skipBootAnimation = it
                            session.run(scope, ThorOperation.SET_BOOT_ANIMATION, it.toString())
                        },
                    )
                }
            }
        }
        if (!enabled) Text("The Thor privileged root service is required before system changes can be applied.", color = MaterialTheme.colorScheme.error)
        if (enabled && !moduleReady) Text("Install Magisk and complete root setup before changing volume steps or boot animation.", color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun RootPanel(session: ThorSession, context: Context, isLowerDisplay: Boolean) {
    val scope = rememberCoroutineScope()
    val snapshot = session.snapshot
    var pendingOperation by remember { mutableStateOf<ThorOperation?>(null) }
    val thorReady = snapshot.profile.isThor
    val actionReady = snapshot.operation.status !in setOf(OperationStatus.RUNNING, OperationStatus.INTERRUPTED)
    val rootReady = snapshot.profile.isThor && snapshot.rootServiceAvailable && snapshot.activeSlot in setOf("_a", "_b")
    fun operationReady(operation: ThorOperation): Boolean =
        actionReady && ThorOperationGuard.validate(snapshot, operation) == null
    val backupReason = if (rootReady) ThorOperationGuard.validate(snapshot, ThorOperation.BACKUP) else null

    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("EZ Root for AYN Thor", style = MaterialTheme.typography.headlineSmall)
        Text("ThorTools checks the active slot and partition layout again before each image operation. Backups are copied to the app folder and Download folder.")
        Button(enabled = operationReady(ThorOperation.INSTALL_MAGISK) && !snapshot.magiskInstalled, onClick = { session.run(scope, ThorOperation.INSTALL_MAGISK) }, modifier = Modifier.fillMaxWidth()) { Text(if (snapshot.magiskInstalled) "Magisk installed" else "Download Magisk") }
        Button(enabled = operationReady(ThorOperation.BACKUP) && snapshot.stockBackupSlots.size < 2, onClick = { pendingOperation = ThorOperation.BACKUP }, modifier = Modifier.fillMaxWidth()) { Text("Back up available slots (${snapshot.stockBackupSlots.size}/2 ready)") }
        Button(enabled = operationReady(ThorOperation.PATCH) && !snapshot.patchedBackupAvailable, onClick = { pendingOperation = ThorOperation.PATCH }, modifier = Modifier.fillMaxWidth()) { Text("Prepare root patch") }
        Button(enabled = operationReady(ThorOperation.FLASH), onClick = { pendingOperation = ThorOperation.FLASH }, modifier = Modifier.fillMaxWidth()) { Text("Flash active-slot patch") }
        Button(enabled = operationReady(ThorOperation.RESTORE), onClick = { pendingOperation = ThorOperation.RESTORE }, modifier = Modifier.fillMaxWidth()) { Text("Restore stock image") }
        OutlinedButton(enabled = operationReady(ThorOperation.REBOOT), onClick = { pendingOperation = ThorOperation.REBOOT }, modifier = Modifier.fillMaxWidth()) { Text("Reboot Thor") }
        OutlinedButton(enabled = operationReady(ThorOperation.CLEAR_CACHE) && snapshot.patchedBackupSlots.isNotEmpty(), onClick = { pendingOperation = ThorOperation.CLEAR_CACHE }, modifier = Modifier.fillMaxWidth()) { Text("Clear patched cache") }
        if (!thorReady) Text("This device is in diagnostics-only mode because it is not an AYN Thor.", color = MaterialTheme.colorScheme.error)
        if (thorReady && !rootReady) Text("This Thor is in diagnostics-only mode until the privileged service and active slot are available.", color = MaterialTheme.colorScheme.error)
        backupReason?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }

    pendingOperation?.let { operation ->
        val title = when (operation) {
            ThorOperation.BACKUP -> "Back up Thor images?"
            ThorOperation.PATCH -> "Prepare root patch?"
            ThorOperation.FLASH -> "Flash root patch?"
            ThorOperation.RESTORE -> "Restore stock image?"
            ThorOperation.REBOOT -> "Reboot Thor?"
            ThorOperation.CLEAR_CACHE -> "Clear patched cache?"
            else -> "Confirm operation?"
        }
        val message = when (operation) {
            ThorOperation.BACKUP -> "This reads both available Thor boot slots and stores stock images in the recovery folder."
            ThorOperation.PATCH -> "This asks Magisk to patch the current active-slot stock image."
            ThorOperation.FLASH -> "This writes the patched image to the current active Thor slot. Reboot after confirming that both stock backups are stored safely."
            ThorOperation.RESTORE -> "This writes the stock image to the current active Thor slot. Reboot the Thor after the operation completes."
            ThorOperation.REBOOT -> "This reboots the Thor without changing its partitions."
            ThorOperation.CLEAR_CACHE -> "This removes Magisk-patched images from the recovery folder but keeps all stock backups available for restore."
            else -> "Confirm this operation."
        }
        val confirmationDetails = buildString {
            append("\n\nActive slot: ")
            append(snapshot.activeSlot)
            append("\nRecovery folder: ")
            append(context.getExternalFilesDir(null)?.absolutePath ?: "Unavailable")
            if (operation == ThorOperation.RESTORE && snapshot.stockRestoreAvailable) {
                append("\nStock restore source is available locally or in Download.")
            }
            append(
                if (isLowerDisplay) {
                    "\nConfirm on the lower display to continue."
                } else {
                    "\nConfirm this operation to continue."
                },
            )
        }
        AlertDialog(
            onDismissRequest = { pendingOperation = null },
            title = { Text(title) },
            text = { Text(message + confirmationDetails) },
            confirmButton = {
                TextButton(onClick = { pendingOperation = null; session.run(scope, operation) }) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { pendingOperation = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AboutPanel(context: Context) {
    val version = context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.1.0-alpha.1"
    Column(
        modifier = Modifier.fillMaxWidth().fillMaxHeight().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("ThorTools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Version $version")
        Text("Thor-first root and system tweaks for the AYN Thor dual-screen handheld.")
        Text("Open source under the GPLv2 license.")
        Text("Forked from FeralAI/o2ptweaks.app with upstream history preserved.")
        Text("Support the project at github.com/sponsors/castdrian or ko-fi.com/castdrian.")
    }
}

@Composable
private fun DataLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Bold, modifier = Modifier.width(150.dp))
        Text(value.ifBlank { "Unavailable" }, modifier = Modifier.weight(1f))
    }
}

private fun ThorSection.label(): String = when (this) {
    ThorSection.STATUS -> "Status"
    ThorSection.TWEAKS -> "Tweaks"
    ThorSection.ROOT -> "EZ Root"
    ThorSection.ABOUT -> "About"
}

private fun ThorVariant.label(): String = when (this) {
    ThorVariant.LITE -> "Lite"
    ThorVariant.BASE -> "Base"
    ThorVariant.PRO -> "Pro"
    ThorVariant.MAX -> "Max"
    ThorVariant.UNKNOWN -> "Unknown"
}

private fun ThorCapability.label(): String = name.lowercase().replace('_', ' ')

private fun OperationStatus.label(): String = name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
