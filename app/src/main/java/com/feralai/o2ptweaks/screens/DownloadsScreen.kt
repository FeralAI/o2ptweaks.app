package com.feralai.o2ptweaks.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.feralai.o2ptweaks.utils.DownloadInfo
import com.feralai.o2ptweaks.utils.DownloadUtils
import com.feralai.o2ptweaks.utils.FileUtils
import com.feralai.o2ptweaks.ui.theme.o2ptweaksTheme
import com.feralai.o2ptweaks.utils.ApkUtils

@Composable
fun DownloadsScreen(
    modifier: Modifier = Modifier,
    context: Context,
    downloads: DownloadInfo?,
    refresh: () -> Unit = { },
) {
    Column(
        modifier = modifier.verticalScroll(
            state = rememberScrollState(),
            flingBehavior = ScrollableDefaults.flingBehavior()
        ).fillMaxHeight().fillMaxWidth(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalDivider()
        when {
            downloads != null && downloads.apps.size > 0 -> {
                for (app in downloads.apps) {
                    Row(
                        modifier = modifier.height(60.dp).fillMaxWidth().padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            modifier = modifier.width(200.dp).padding(8.dp),
                            text = app.name,
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Button(
                            modifier = modifier.width(200.dp),
                            onClick = {
                                Toast.makeText(context, "Downloading ${app.name}...", Toast.LENGTH_SHORT).show()
                                val apkFileName = DownloadUtils.downloadFile(FileUtils.getPathDownload(), app.fileUrl, app.apkName)
                                if (apkFileName != "") {
                                    if (ApkUtils.installApkFromAssets(context, app.apkName, "app"))
                                        refresh()
                                }
                            },
                        ) {
                            Text("Install v${app.version}")
                        }
                    }

                    Row(
                        modifier = modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Spacer(modifier = modifier.width(24.dp))
                        Text(text = app.description, style = MaterialTheme.typography.bodySmall,)
                    }

                    HorizontalDivider()
                }
            }
        }

    }

}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=960dp,height=540dp,dpi=369,orientation=landscape"
)
@Composable
fun DownloadsScreenPreview() {
    val context = LocalContext.current
    o2ptweaksTheme {
        DownloadsScreen(context = context, downloads = null)
    }
}