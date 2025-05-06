package com.feralai.o2ptweaks.screens

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.widget.Toast
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.feralai.o2ptweaks.AppSettings
import com.feralai.o2ptweaks.utils.MagiskUtil
import com.feralai.o2ptweaks.utils.PatchUtils
import com.feralai.o2ptweaks.utils.RootUtils
import com.feralai.o2ptweaks.utils.SystemUtils
import com.feralai.o2ptweaks.components.ConfirmDialog
import com.feralai.o2ptweaks.ui.theme.o2ptweaksTheme
import com.feralai.o2ptweaks.utils.ApkUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


private val BUTTON_PADDING = PaddingValues(0.dp, 2.dp)
private val CONTENT_PADDING = PaddingValues(16.dp, 0.dp)
private val LABEL_PADDING = PaddingValues( 0.dp, 0.dp, 4.dp, 0.dp)

private var magiskReceiver: BroadcastReceiver? = null

@Composable
fun RootScreen(
    modifier: Modifier = Modifier,
    context: Context,
) {
    val scope = rememberCoroutineScope()

    var isBusy by remember { mutableStateOf(false) }
    val hasRoot by remember { mutableStateOf(RootUtils.isDeviceRooted) }
    var isMagiskInstalled by remember { mutableStateOf(MagiskUtil.hasMagiskPackage(context)) }
    var hasBootBackup by remember { mutableStateOf(PatchUtils.checkBootBackupExists(context)) }
    var hasBootPatched by remember { mutableStateOf(PatchUtils.checkBootMagiskExists(context)) }
    var confirmRebootFlashDialog by remember { mutableStateOf(false) }
    var confirmRebootRestoreDialog by remember { mutableStateOf(false) }
    var showIsFlashing by remember { mutableStateOf(false) }
    var magiskDownloadId by remember { mutableLongStateOf(0) }
    var magiskDownloadFile by remember { mutableStateOf("") }

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    if (magiskReceiver == null) {
        magiskReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                    val query = DownloadManager.Query()
                    query.setFilterById(magiskDownloadId)
                    val cursor: Cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(columnIndex)
                        val columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = cursor.getInt(columnReason)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            Toast.makeText(context, "Downloading finished!", Toast.LENGTH_SHORT).show()
                            if (magiskDownloadFile != "") {
                                ApkUtils.installApk(context, magiskDownloadFile)
                                isMagiskInstalled = true
                            }
                            isBusy = false

                        } else if (status == DownloadManager.STATUS_FAILED) {
                            Toast.makeText(context, "Downloading failed: $reason", Toast.LENGTH_SHORT).show()
                        }
                    }
                    cursor.close()
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            magiskReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    Column(
        modifier = modifier
            .verticalScroll(
                state = rememberScrollState(),
                flingBehavior = ScrollableDefaults.flingBehavior()
            )
            .fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = modifier
                    .weight(3.0f)
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight()
                    .padding(CONTENT_PADDING),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "EZ Root uses system and Magisk command line utilities to root your device with just a few button taps."
                )
                Spacer(modifier = modifier.padding(8.dp))
                Text(
                    text = "Backup files are cached in the app data folder, and also archived to the internal storage Download folder. " +
                           "Restore option is only available if a previous backup file is present in the app data folder."
                )
                Spacer(modifier = modifier.padding(8.dp))
                Text(
                    fontWeight = FontWeight.Bold,
                    text = "To prevent errors, it is highly recommended to restore to stock prior to attempting any OTA updates.",
                )
                Spacer(modifier = modifier.padding(8.dp))
                Text(
                    color = Color(200, 0, 0),
                    fontWeight = FontWeight.Bold,
                    text = "CAUTION: Uninstalling this app will prompt to remove data, which would clear the backup cache. " +
                           "Archived copies in the Download folder are not affected.",
                )
            }

            Column(
                modifier = modifier
                    .weight(2.0f)
                    .fillMaxWidth(0.8f)
                    .padding(CONTENT_PADDING),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row {
                        Text(
                            modifier = modifier.padding(LABEL_PADDING),
                            fontWeight = FontWeight.Bold,
                            text = "Firmware Version:"
                        )
                        Text(SystemUtils.getPropFirmwareVersion())
                    }

                    Spacer(modifier = modifier.padding(0.dp))

                    Row {
                        Text(
                            modifier = modifier.padding(LABEL_PADDING),
                            fontWeight = FontWeight.Bold,
                            text = "Active Boot Slot:"
                        )
                        Text(SystemUtils.getPropSlot())
                    }
                }
                Button(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(BUTTON_PADDING),
                    enabled = !isBusy && !isMagiskInstalled,
                    onClick = {
                        isBusy = true

                        val job = scope.launch {
                            delay(50)
                            val result = MagiskUtil.downloadMagisk(context)
                            magiskDownloadFile = result.component1()
                            magiskDownloadId = result.component2()
                        }
                    },
                ) {
                    Text(if (isMagiskInstalled) "Magisk installed" else "Install Magisk", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(BUTTON_PADDING),
                    enabled = !isBusy && !hasRoot && isMagiskInstalled && !hasBootBackup,
                    onClick = {
                        isBusy = true

                        val job = scope.launch {
                            delay(50)
                            PatchUtils.backupBoot(context)
                            hasBootBackup = PatchUtils.checkBootBackupExists(context)
                        }

                        job.invokeOnCompletion {
                            isBusy = false
                            Toast.makeText(
                                context,
                                if (hasBootBackup) "Backup completed" else "Backup failed, check log",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                ) {
                    Text("Create backup", style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(BUTTON_PADDING),
                    enabled = !isBusy && !hasRoot && isMagiskInstalled && hasBootBackup && !hasBootPatched,
                    onClick = {
                        isBusy = true

                        val job = scope.launch {
                            delay(50)
                            PatchUtils.patchBoot(context)
                            hasBootPatched = PatchUtils.checkBootMagiskExists(context)
                        }

                        job.invokeOnCompletion {
                            isBusy = false
                            Toast.makeText(
                                context,
                                if (hasBootPatched) "Patching completed" else "Patching failed, check log",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                ) {
                    Text("Prepare patch", style = MaterialTheme.typography.titleMedium)
                }

                Button(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(BUTTON_PADDING),
                    enabled = !isBusy && !hasRoot && isMagiskInstalled && hasBootBackup && hasBootPatched,
                    colors = ButtonDefaults.buttonColors().copy(containerColor = Color(0, 200, 0)),
                    onClick = { confirmRebootFlashDialog = true },
                ) {
                    Text("Flash patch", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(BUTTON_PADDING),
                    enabled = !isBusy && hasRoot && hasBootBackup,
                    colors = ButtonDefaults.buttonColors().copy(containerColor = Color(0, 200, 0)),
                    onClick = { confirmRebootRestoreDialog = true },
                ) {
                    Text("Restore stock", style = MaterialTheme.typography.titleMedium)
                }
                Button(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(BUTTON_PADDING),
                    enabled = !isBusy && !hasRoot && (hasBootBackup || hasBootPatched),
                    colors = ButtonDefaults.buttonColors().copy(containerColor = Color(200, 0, 0)),
                    onClick = {
                        isBusy = true

                        val job = scope.launch {
                            delay(50)
                            PatchUtils.clearBootCache(context)
                            hasBootBackup = PatchUtils.checkBootBackupExists(context)
                            hasBootPatched = PatchUtils.checkBootMagiskExists(context)
                        }

                        job.invokeOnCompletion {
                            isBusy = false
                            Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                        }
                    },
                ) {
                    Text("Clear cache", style = MaterialTheme.typography.titleMedium)
                }
            }

        }
    }

    when {
        confirmRebootFlashDialog -> {
            ConfirmDialog(
                modifier = modifier,
                onDismissRequest = { confirmRebootFlashDialog = false },
                onConfirmation = {
                    if (!isBusy) {
                        isBusy = true
                        confirmRebootFlashDialog = false
                        showIsFlashing = true

                        val job = scope.launch {
                            delay(50)
                            AppSettings.save(context)
                            PatchUtils.flashBoot(context)
                        }

                        job.invokeOnCompletion {
                            RootUtils.reboot(context)
                        }
                    }
                },
                dialogTitle = "💾\nConfirm Flash Root",
                dialogText = "Are you sure you want to flash the rooted file?\n\nYour device will reboot after the flashing process is complete.",
            )
        }
    }

    when {
        confirmRebootRestoreDialog -> {
            ConfirmDialog(
                modifier = modifier,
                onDismissRequest = { confirmRebootRestoreDialog = false },
                onConfirmation = {
                    if (!isBusy) {
                        isBusy = true
                        confirmRebootRestoreDialog = false
                        showIsFlashing = true

                        val job = scope.launch {
                            delay(50)
                            AppSettings.save(context)
                            PatchUtils.restoreBoot(context)
                        }

                        job.invokeOnCompletion {
                            RootUtils.reboot(context)
                        }
                    }
                },
                dialogTitle = "💾\nConfirm Flash Restore",
                dialogText = "Are you sure you want to flash the restore file?\n\nYour device will reboot after the flashing process is complete.",
            )
        }
    }

    when {
        showIsFlashing -> {
            ConfirmDialog(
                modifier = modifier,
                dialogTitle = "Flashing in progress",
                dialogText = "Please do not interrupt the process. Your device will restart once complete.",
            )
        }
    }

}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=960dp,height=540dp,dpi=369,orientation=landscape"
)
@Composable
fun RootScreenPreview() {
    val context = LocalContext.current
    o2ptweaksTheme {
        RootScreen(context = context)
    }
}