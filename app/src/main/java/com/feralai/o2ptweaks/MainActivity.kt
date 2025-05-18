package com.feralai.o2ptweaks

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.feralai.o2ptweaks.components.ConfirmDialog
import com.feralai.o2ptweaks.screens.AboutScreen
import com.feralai.o2ptweaks.screens.DownloadsScreen
import com.feralai.o2ptweaks.screens.HomeScreen
import com.feralai.o2ptweaks.screens.RootScreen
import com.feralai.o2ptweaks.screens.SettingsScreen
import com.feralai.o2ptweaks.ui.theme.o2ptweaksTheme
import com.feralai.o2ptweaks.utils.DownloadUtils
import com.feralai.o2ptweaks.utils.JdspUtils
import com.feralai.o2ptweaks.utils.RootUtils
import com.feralai.o2ptweaks.utils.SystemUtils
import kotlinx.coroutines.launch
import kotlin.system.exitProcess


enum class Route {
    Home,
    Downloads,
    Settings,
    Root,
    About,
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        val canRun = RootUtils.hasPServer() || RootUtils.isDeviceRooted

        // Hide top bar
        //insetsController.hide(WindowInsetsCompat.Type.statusBars());
        // Hide bottom bar
        insetsController.hide(WindowInsetsCompat.Type.navigationBars());
        // Auto hide status and navigation bars when temporarily shown
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            o2ptweaksTheme {
                if (canRun) {
                    val context = LocalContext.current
                    val sharedPrefs = AppSettings.getSharedPrefs(context)

                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        ScreenManager(
                            modifier = Modifier.padding(innerPadding),
                            context = context,
                            sharedPrefs = sharedPrefs,
                        )
                    }
                }
                else {
                    ConfirmDialog(
                        modifier = Modifier,
                        onDismissRequest = { exitProcess(0) },
                        dialogTitle = "Missing System Requirements",
                        dialogText = "Your device does not support the APIs required to run this app.\n\n" +
                                "The application will close when this dialog is dismissed.",
                    )
                }
            }
        }
    }
}

@SuppressLint("RestrictedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenManager(
    modifier: Modifier = Modifier,
    context: Context,
    sharedPrefs: SharedPreferences,
//    content: @Composable (PaddingValues) -> Unit
) {
    val downloads = DownloadUtils.getDownloads(context)
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val deviceModel = SystemUtils.getPropDeviceModel()

    var title by remember { mutableStateOf("System Info") }
    var rebootRequired by remember { mutableStateOf(AppSettings.needsReboot) }

    var confirmRebootDialog by remember { mutableStateOf(false) }
    var confirmRootScreenDialog by remember { mutableStateOf(false) }
    var unsupportedDeviceDialog by remember { mutableStateOf(SupportedDevices.isSupportedDevice(deviceModel)) }
    val isRooted = RootUtils.isDeviceRooted
    val isO2P = deviceModel.lowercase() == SupportedDevices.AYN_ODIN2_PORTAL.lowercase()

    var jdspEnabled by remember { mutableStateOf(AppSettings.getJdspEnabled(sharedPrefs)) }
    var o2pVolumeFix by remember { mutableStateOf(AppSettings.getO2PVolumeFix(sharedPrefs)) }

    val versionName = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.1"
    } catch (e: PackageManager.NameNotFoundException) {
        e.printStackTrace()
        "0.1"
    }

    fun closeMenu() {
        scope.launch {
            drawerState.apply {
                close()
            }
        }
    }

    fun toggleMenu() {
        scope.launch {
            drawerState.apply {
                if (isClosed) open() else close()
            }
        }
    }

    fun navigateTo(route: String, routeTitle: String) {
        navController.popBackStack()
        navController.navigate(route = route)
        title = routeTitle
        closeMenu()
    }

    fun onRebootRequired(showPrompt: Boolean) {
        AppSettings.needsReboot = true
        rebootRequired = true
        confirmRebootDialog = showPrompt
    }


    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = modifier.width(240.dp),
                drawerShape = RectangleShape,
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                val menuTitleText = "O2P Tweaks"

                Row(
                    modifier = modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Image(
                        modifier = modifier.height(32.dp).padding(12.dp, 0.dp, 0.dp, 0.dp),
                        painter = painterResource(id = R.drawable.app_menu_icon),
                        contentDescription = "O2P Tweaks Icon",
                    )
                    Text(
                        text = "$menuTitleText v$versionName",
                        modifier = modifier.padding(PaddingValues(10.dp, 16.dp, 16.dp, 16.dp)),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                HorizontalDivider()

                NavigationDrawerItem(
                    label = { Text("System Info", style = MaterialTheme.typography.labelLarge) },
                    shape = RectangleShape,
                    icon = { Icon(Icons.Filled.Info, contentDescription = "") },
                    selected = currentDestination?.route == Route.Home.name,
                    onClick = { navigateTo(Route.Home.name, "System Info") },
                )

//                NavigationDrawerItem(
//                    label = { Text("Downloads", style = MaterialTheme.typography.labelLarge) },
//                    shape = RectangleShape
//                    selected = currentDestination?.route == Route.Downloads.name,
//                    onClick = { navigateTo(Route.Downloads.name, "Downloads") }, ,
//                )

                NavigationDrawerItem(
                    label = { Text("Tweaks", style = MaterialTheme.typography.labelLarge) },
                    shape = RectangleShape,
                    icon = { Icon(Icons.Filled.Build, contentDescription = "") },
                    selected = currentDestination?.route == Route.Settings.name,
                    onClick = { navigateTo(Route.Settings.name, "Tweaks") },
                )

                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("EZ Root", style = MaterialTheme.typography.labelLarge) },
                    shape = RectangleShape,
                    icon = { Icon(Icons.Filled.Lock, contentDescription = "") },
                    selected = currentDestination?.route == Route.Root.name,
                    onClick = {
                        if (AppSettings.allowRootScreen) {
                            navigateTo(Route.Root.name, "EZ Root")
                        }
                        else {
                            confirmRootScreenDialog = true
                        }
                    },
                )

                NavigationDrawerItem(
                    label = { Text("Reboot", style = MaterialTheme.typography.labelLarge) },
                    shape = RectangleShape,
                    icon = { Icon(Icons.Filled.Refresh, contentDescription = "") },
                    selected = false,
                    onClick = {
                        confirmRebootDialog = true
                        closeMenu()
                    },
                )

                HorizontalDivider()
                NavigationDrawerItem(
                    label = { Text("About", style = MaterialTheme.typography.labelLarge) },
                    shape = RectangleShape,
                    icon = { Icon(Icons.Filled.AccountCircle, contentDescription = "") },
                    selected = currentDestination?.route == Route.About.name,
                    onClick = { navigateTo(Route.About.name, "About") },
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            modifier = modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = title, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (isRooted) "ROOT" else "",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(200, 0, 0),
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            toggleMenu()
                        }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        when {
                            rebootRequired -> {
                                IconButton(onClick = {
                                    confirmRebootDialog = true
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reboot",
                                        tint = Color(0, 200, 0),
                                    )
                                }
                            }
                        }
                    },
                )
            },
        )
        {contentPadding ->
            NavHost(
                navController = navController,
                startDestination = Route.Home.name,
                modifier = modifier.padding(contentPadding),
            ) {
                composable(route = Route.Home.name) {
                    HomeScreen(modifier, context, sharedPrefs)
                }

                composable(route = Route.Downloads.name) {
                    DownloadsScreen(modifier, context, downloads)
                }

                composable(route = Route.Settings.name) {
                    SettingsScreen(modifier, context, isO2P, jdspEnabled, o2pVolumeFix, { newValue ->
                        jdspEnabled = newValue
                        AppSettings.setJdspEnabled(sharedPrefs, newValue)
                        if (isRooted) {
                            JdspUtils.installJdspMagiskModule(context)
                            onRebootRequired(false)
                        }
                        else {
                            if (newValue)
                                JdspUtils.enableJdsp(context)
                            else
                                JdspUtils.disableJdsp(context)
                        }

                    }, { newValue ->
                        o2pVolumeFix = newValue
                        AppSettings.setO2PVolumeFix(sharedPrefs, newValue)
                        if (isRooted)
                            AppSettings.save(context)

                        if (newValue)
                            RootUtils.enableO2PVolumeFix(context)
                        else
                            RootUtils.disableO2PVolumeFix(context)
                    }) { onRebootRequired(it) }
                }

                composable(route = Route.Root.name) {
                    RootScreen(modifier, context)
                }

                composable(route = Route.About.name) {
                    AboutScreen(modifier, context)
                }

//                composable(
//                    route = "${Route.Second.name}/{index}",
//                    arguments = listOf(
//                        navArgument("index") {
//                            type = NavType.IntType
//                            nullable = false
//                            defaultValue = 1
//                        }
//                    )
//                ) {entry->
//                    val index = entry.arguments?.getInt("index")
//                    SecondScreen(
//                        index = index ?: 1
//                    )
//                }
            }

            when {
                confirmRebootDialog -> {
                    ConfirmDialog(
                        modifier = modifier,
                        onDismissRequest = { confirmRebootDialog = false },
                        onConfirmation = { RootUtils.reboot(context) },
                        dialogTitle = "Reboot now?",
                    )
                }
            }

            when {
                confirmRootScreenDialog -> {
                    ConfirmDialog(
                        modifier = modifier,
                        onDismissRequest = { confirmRootScreenDialog = false },
                        onConfirmation = {
                            AppSettings.allowRootScreen = true
                            confirmRootScreenDialog = false
                            navigateTo(Route.Root.name, "EZ Root")
                        },
                        dialogTitle = "⛔ DANGER ⛔\nEZ Root Access",
                        dialogText = "Rooting your device may require technical knowledge or support if something goes wrong.\n\n" +
                                "Rooting could possibly void the device warranty.\n\n" +
                                "Please confirm you accept full responsibility for use of this utility.",
                    )
                }
            }

            when {
                unsupportedDeviceDialog -> {
                    ConfirmDialog(
                        modifier = modifier,
                        onDismissRequest = { exitProcess(0) },
                        onConfirmation = { unsupportedDeviceDialog = false },
                        dialogTitle = "⚠️ WARNING ⚠️\nUnsupported Device",
                        dialogText = "${SystemUtils.getPropDeviceModel()} is unsupported.\n\n" +
                                "Some features may not work properly.\n\n" +
                                "Are you sure you want to continue using the application?",
                    )
                }
            }

        }
    }
}
