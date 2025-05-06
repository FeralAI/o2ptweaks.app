package com.feralai.o2ptweaks.screens

import android.annotation.SuppressLint
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
import com.feralai.o2ptweaks.utils.RootUtils
import com.feralai.o2ptweaks.SupportedDevices
import com.feralai.o2ptweaks.utils.SystemUtils
import com.feralai.o2ptweaks.components.DropdownField
import com.feralai.o2ptweaks.components.SettingsRow
import com.feralai.o2ptweaks.ui.theme.o2ptweaksTheme
import com.feralai.o2ptweaks.utils.JdspUtils
import kotlin.text.*

private val BUTTON_WIDTH = 250.dp
private val HEADING_PADDING = PaddingValues(16.dp, 0.dp, 0.dp, 0.dp)
private val ROW_PADDING = PaddingValues(0.dp, 4.dp)
private val SUBROW_PADDING = PaddingValues(0.dp, 8.dp, 0.dp, 0.dp)
private val LABEL_WIDTH = 250.dp
private val MENU_WIDTH_SM = 100.dp

private var jdspMagiskReceiver: BroadcastReceiver? = null

@SuppressLint("DefaultLocale")
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    context: Context,
    isO2P: Boolean,
    jdspEnabled: Boolean,
    o2pVolumeFix: Boolean,
    jdspEnabledChange: (Boolean) -> Unit,
    o2pVolumeFixChange: (Boolean) -> Unit,
    onRebootRequired: (Boolean) -> Unit = { },
) {
    val sharedPrefs = AppSettings.getSharedPrefs(context)

    val isRooted = RootUtils.isDeviceRooted
    val stockLcdDensity = AppSettings.getPropLcdDensity(sharedPrefs)
    val stockVolumeSteps = AppSettings.getPropVolumeSteps(sharedPrefs)

    val animSpeedOptions: List<String> = listOf("1.0x", "0.5x", "Off")
    var animSpeedValue = AppSettings.getAnimationSpeed(sharedPrefs)
    var animSpeedText by remember { mutableStateOf(if (animSpeedValue == 0F) "Off" else animSpeedValue.toString() + "x") }

    val dpiMin = AppSettings.DPI_MIN
    val dpiOptions: List<String> = List(400 - dpiMin + 1) { index -> (dpiMin + index).toString() }
    var dpiText by remember { mutableStateOf(AppSettings.getDpi(sharedPrefs).toString()) }

    val volStepOptions: List<String> = listOf("15", "20", "25", "40", "50")
    var volStepText by remember { mutableStateOf(AppSettings.getVolumeSteps(sharedPrefs).toString()) }

    val jdspDownloadId by remember { mutableLongStateOf(0) }
    val jdspDownloadFile by remember { mutableStateOf("") }

    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    if (jdspMagiskReceiver == null) {
        jdspMagiskReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val action = intent.action
                if (DownloadManager.ACTION_DOWNLOAD_COMPLETE == action) {
                    val query = DownloadManager.Query()
                    query.setFilterById(jdspDownloadId)
                    val cursor: Cursor = downloadManager.query(query)
                    if (cursor.moveToFirst()) {
                        val columnIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        val status = cursor.getInt(columnIndex)
                        val columnReason = cursor.getColumnIndex(DownloadManager.COLUMN_REASON)
                        val reason = cursor.getInt(columnReason)
                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            if (jdspDownloadFile != "") {
                                val success = MagiskUtil.installMagiskModule(context, jdspDownloadFile)
                                Toast.makeText(context, if (success) "JamesDSP Magisk Module Installed!" else "JamesDSP Magisk Module Install Failed!", Toast.LENGTH_SHORT).show()
                            }
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
            jdspMagiskReceiver,
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    Column(
        modifier = modifier.verticalScroll(
            state = rememberScrollState(),
            flingBehavior = ScrollableDefaults.flingBehavior()
        ).fillMaxHeight(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = modifier.fillMaxWidth().padding(ROW_PADDING),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start,
        ) {
            Text(
                modifier = modifier.width(LABEL_WIDTH).padding(HEADING_PADDING),
                text = "Display",
                color = Color(100, 100, 200),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        SettingsRow(
            label = "DPI",
            detail = "Adjust the size and density of screen elements." +
                    "\n\nDefault value: $stockLcdDensity",
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        try {
                            AppSettings.setDpi(sharedPrefs, stockLcdDensity)
                            RootUtils.setDpi(context, stockLcdDensity)
                            AppSettings.save(context)
                            dpiText = stockLcdDensity.toString()
                            if (RootUtils.isDeviceRooted)
                                onRebootRequired(false)
                        }
                        catch (_: Exception) { }
                    },
                ) {
                    Text("Reset", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(modifier = modifier.padding(8.dp))
                DropdownField(
                    context = context,
                    options = dpiOptions,
                    value = dpiText,
                    width = MENU_WIDTH_SM,
                    onSelected = {
                        try {
                            val dpi = it.toInt()
                            AppSettings.setDpi(sharedPrefs, dpi)
                            AppSettings.save(context)
                            dpiText = AppSettings.getDpi(sharedPrefs).toString()
                            RootUtils.setDpi(context, dpi)
                            if (RootUtils.isDeviceRooted)
                                onRebootRequired(false)
                        }
                        catch (_: Exception) { }
                    },
                )

            }

        }

        SettingsRow(
            label = "Animation Speed",
            detail = "Adjust the speed of system animations." +
                     "\n\nDefault value: 1.0x",
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        try {
                            AppSettings.setAnimationSpeed(sharedPrefs, AppSettings.ANIMIATION_SPEED_DEFAULT)
                            RootUtils.setAnimationSpeed(context, AppSettings.ANIMIATION_SPEED_DEFAULT)
                            animSpeedText = AppSettings.ANIMIATION_SPEED_DEFAULT.toString() + "x"
                        }
                        catch (_: Exception) { }
                    },
                ) {
                    Text("Reset", style = MaterialTheme.typography.titleSmall)
                }
                Spacer(modifier = modifier.padding(8.dp))
                DropdownField(
                    context = context,
                    options = animSpeedOptions,
                    value = animSpeedText,
                    width = MENU_WIDTH_SM,
                    onSelected = {
                        try {
                            var animSpeed = AppSettings.ANIMIATION_SPEED_DEFAULT
                            when (it) {
                                "1.0x" -> animSpeed = 1.0F
                                "0.5x" -> animSpeed = 0.5F
                                "Off" -> animSpeed = 0.0F
                            }

                            AppSettings.setAnimationSpeed(sharedPrefs, animSpeed)
                            AppSettings.save(context)
                            animSpeedValue = AppSettings.getAnimationSpeed(sharedPrefs)
                            animSpeedText = if (animSpeedValue == 0F) "Off" else animSpeedValue.toString() + "x"
                            RootUtils.setAnimationSpeed(context, animSpeed)
                        }
                        catch (_: Exception) { }
                    },
                )

            }
        }

        Spacer(modifier = modifier.padding(16.dp))

        if (isO2P || isRooted) {
            Row(
                modifier = modifier.fillMaxWidth().padding(ROW_PADDING),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
            ) {
                Text(
                    modifier = modifier.width(LABEL_WIDTH).padding(HEADING_PADDING),
                    text = "Sound",
                    color = Color(100, 100, 200),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (isO2P || isRooted) {
            SettingsRow(
                label = "JamesDSP",
                detail = "DSP library providing various EQ options and audio effects. " +
                        "Install the JamesDSP Manager application before enabling this feature." +
                        (if (isRooted) "\n\nMagisk module requires a restart to take effect." else ""),
            ) {
                if (isRooted) {
                    Row(
                        modifier = modifier.padding(SUBROW_PADDING),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Button(
                            modifier = modifier.width(BUTTON_WIDTH),
                            onClick = {
                                JdspUtils.installJdspMagiskModule(context)
                                Toast.makeText(context, "JamesDSP Magisk Module Installed, Please Reboot", Toast.LENGTH_SHORT).show()
                                onRebootRequired(false)
                            },
                        ) {
                            Text("Install JamesDSP Module", style = MaterialTheme.typography.titleSmall)
                        }
                    }
                }
                else {
                    Switch(
                        checked = jdspEnabled,
                        onCheckedChange = jdspEnabledChange
                    )
                }

                Row(
                    modifier = modifier.padding(SUBROW_PADDING),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start,
                ) {
                    Button(
                        modifier = modifier.width(BUTTON_WIDTH),
                        onClick = {
                            JdspUtils.installJdspManager(context)
                            if (isO2P) {
                                JdspUtils.copyBackupFile(context)
                            }
                        },
                    ) {
                        Text("Install JamesDSP Manager", style = MaterialTheme.typography.titleSmall)
                    }
                }

            }

        }

        if (isO2P) {
            SettingsRow(
                label = "O2P Volume Fix",
                detail = "Applies an updated volume curve to the internal speakers for lower minimum volume.",
            ) {
                Switch(
                    checked = o2pVolumeFix,
                    onCheckedChange = o2pVolumeFixChange
                )
            }
        }

        if (isRooted) {
            SettingsRow(
                label = "Volume Steps",
                detail = "Higher volume steps gives more granular volume control.\n\nDefault value: $stockVolumeSteps",
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            try {
                                AppSettings.setVolumeSteps(sharedPrefs, stockVolumeSteps)
                                AppSettings.save(context)
                                volStepText =
                                    AppSettings.getVolumeSteps(sharedPrefs, stockVolumeSteps)
                                        .toString()
                                onRebootRequired(false)
                            } catch (_: Exception) {
                            }
                        },
                    ) {
                        Text("Reset", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(modifier = modifier.padding(8.dp))
                    DropdownField(
                        context = context,
                        options = volStepOptions,
                        value = volStepText,
                        width = MENU_WIDTH_SM,
                        onSelected = {
                            try {
                                AppSettings.setVolumeSteps(sharedPrefs, it.toInt())
                                AppSettings.save(context)
                                volStepText = AppSettings.getVolumeSteps(sharedPrefs).toString()
                                onRebootRequired(false)
                            } catch (_: Exception) {
                            }
                        },
                    )

                }
            }
        }

        Spacer(modifier = modifier.padding(16.dp))
    }

}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=960dp,height=540dp,dpi=369,orientation=landscape"
)
@Composable
fun SettingsScreenPreview() {
    val context = LocalContext.current
    o2ptweaksTheme {
        SettingsScreen(context = context, isO2P = true, jdspEnabled = false, o2pVolumeFix = false,
            jdspEnabledChange = { _ -> },
            o2pVolumeFixChange = { _ -> }
        )
    }
}