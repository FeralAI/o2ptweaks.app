package com.feralai.o2ptweaks

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.feralai.o2ptweaks.ui.theme.o2ptweaksTheme

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "O2PTweaksPrefs"
    private val JDSP_ENABLED_KEY = "jdspEnabled"
    private val O2P_VOLUME_FIX_KEY = "o2pSpeakerVolumePatch"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            o2ptweaksTheme {
                val context = LocalContext.current
                val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                var jdspEnabled by remember {
                    mutableStateOf(sharedPrefs.getBoolean(JDSP_ENABLED_KEY, false))
                }
                var o2pVolumeFix by remember {
                    mutableStateOf(sharedPrefs.getBoolean(O2P_VOLUME_FIX_KEY, false))
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(Modifier.padding(innerPadding), jdspEnabled, o2pVolumeFix, context, { newValue ->
                        jdspEnabled = newValue
                        with(sharedPrefs.edit()) {
                            putBoolean(JDSP_ENABLED_KEY, newValue)
                            apply()
                        }
                    }, { newValue ->
                        o2pVolumeFix = newValue
                        with(sharedPrefs.edit()) {
                            putBoolean(O2P_VOLUME_FIX_KEY, newValue)
                            apply()
                        }
                    })
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, jdspEnabled: Boolean, o2pVolumeFix: Boolean, context: Context, jdspEnabledChange: (Boolean) -> Unit, o2pVolumeFixChange: (Boolean) -> Unit) {
    Row(
        modifier = modifier.fillMaxSize()
    ) {

        Column(
            modifier = modifier.fillMaxHeight().weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text("Enable JamesDSP on boot?")
                Spacer(modifier.width(16.dp))
                Switch(
                    checked = jdspEnabled,
                    onCheckedChange = jdspEnabledChange
                )
            }

            Button(
                modifier = modifier.fillMaxWidth().padding(8.dp),
                onClick = {
                    RootUtils.enableJdsp(context) // Azione direttamente qui
                    Toast.makeText(context, "Enabling JamesDSP...", Toast.LENGTH_SHORT).show()
                },
            ) {
                Text("Enable JamesDSP")
            }

            Button(
                modifier = modifier.fillMaxWidth().padding(8.dp),
                onClick = {
                    RootUtils.disableJdsp(context) // Azione direttamente qui
                    Toast.makeText(context, "Disabling JamesDSP...", Toast.LENGTH_SHORT).show()
                },
            ) {
                Text("Disable JamesDSP")
            }

            Button(
                modifier = modifier.fillMaxWidth().padding(8.dp),
                onClick = {
                    Toast.makeText(context, "Install JamesDsp...", Toast.LENGTH_SHORT).show()
                    RootUtils.installJdsp(context)
                },
            ) {
                Text("Install JamesDSP Manager")
            }
        }

        Column(
            modifier = modifier.fillMaxSize().weight(1f).padding(16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Enable O2P volume fix on boot?")
                Spacer(modifier.width(16.dp))
                Switch(
                    checked = o2pVolumeFix,
                    onCheckedChange = o2pVolumeFixChange
                )
            }

            Button(
                modifier = modifier.fillMaxWidth().padding(8.dp),
                onClick = {
                    RootUtils.enableO2PVolumeFix(context)
                    Toast.makeText(context, "Enabling O2P Volume Fix...", Toast.LENGTH_SHORT).show()
                },
            ) {
                Text("Enable O2P Volume Fix")
            }

            Button(
                modifier = modifier.fillMaxWidth().padding(8.dp),
                onClick = {
                    RootUtils.disableO2PVolumeFix(context)
                    Toast.makeText(context, "Disabling O2P Volume Fix...", Toast.LENGTH_SHORT).show()
                },
            ) {
                Text("Disable O2P Volume Fix (reboot required)")
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
fun DefaultPreview() {
    val context = LocalContext.current
    o2ptweaksTheme {
        MainScreen(jdspEnabled = false, o2pVolumeFix = false, context = context,
            jdspEnabledChange = { _ -> },
            o2pVolumeFixChange = { _ -> }
        )
    }
}