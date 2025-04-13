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
import androidx.compose.ui.unit.dp
import com.feralai.o2ptweaks.ui.theme.o2ptweaksTheme

class MainActivity : ComponentActivity() {

    private val PREFS_NAME = "O2PTweaksPrefs"
    private val JDSP_ENABLED_KEY = "jdspEnabled"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            o2ptweaksTheme {
                val context = LocalContext.current
                val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                var isEnabled by remember {
                    mutableStateOf(sharedPrefs.getBoolean(JDSP_ENABLED_KEY, false))
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(Modifier.padding(innerPadding), isEnabled, context) { newValue ->
                        isEnabled = newValue
                        with(sharedPrefs.edit()) {
                            putBoolean(JDSP_ENABLED_KEY, newValue)
                            apply()
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier = Modifier, isEnabled: Boolean, context: Context, onCheckedChange: (Boolean) -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = {
                Toast.makeText(context, "Install JamesDsp...", Toast.LENGTH_SHORT).show()
                JdspUtils.installJdsp(context)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Install JamesDSPManagerThePBone v1.6.8")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                JdspUtils.enableJdsp(context) // Azione direttamente qui
                Toast.makeText(context, "Enabling JamesDsp...", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Enable JamesDSP")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                JdspUtils.disableJdsp(context) // Azione direttamente qui
                Toast.makeText(context, "Disabling JamesDsp...", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Disable JamesDSP")
        }
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Enable O2P Tweaks at boot?")
            Spacer(Modifier.width(8.dp))
            Switch(
                checked = isEnabled,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    val context = LocalContext.current
    o2ptweaksTheme {
        MainScreen(isEnabled = false, context = context) { _ -> }
    }
}