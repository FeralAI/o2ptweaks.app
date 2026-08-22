package dev.adrian.thortools

import android.app.Presentation
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.adrian.thortools.screens.ThorControlScreen
import dev.adrian.thortools.screens.ThorDashboardScreen
import dev.adrian.thortools.ui.theme.ThorToolsTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var session: ThorSession
    private var displayManager: DisplayManager? = null
    private var secondaryPresentation: ThorPresentation? = null
    private var secondaryDisplayRetry: Job? = null
    private var secondaryDisplayRetryCount = 0
    private var activityResumed = false
    private var hasThorLowerDisplay by mutableStateOf(false)

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            requestSecondaryDisplay()
        }

        override fun onDisplayRemoved(displayId: Int) {
            if (secondaryPresentation?.display?.displayId == displayId) dismissSecondaryDisplay()
        }

        override fun onDisplayChanged(displayId: Int) {
            requestSecondaryDisplay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = ThorSession(this)
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        configureThorWindow(window)
        lifecycleScope.launch {
            session.load()
        }
        setContent {
            ThorToolsTheme {
                if (hasThorLowerDisplay) {
                    ThorDashboardScreen(session, this@MainActivity)
                } else {
                    ThorControlScreen(session, this@MainActivity)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        activityResumed = true
        displayManager?.registerDisplayListener(displayListener, null)
        requestSecondaryDisplay()
    }

    override fun onResume() {
        super.onResume()
        requestSecondaryDisplay()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        requestSecondaryDisplay()
    }

    override fun onStop() {
        activityResumed = false
        secondaryDisplayRetry?.cancel()
        secondaryDisplayRetry = null
        secondaryDisplayRetryCount = 0
        dismissSecondaryDisplay()
        displayManager?.unregisterDisplayListener(displayListener)
        super.onStop()
    }

    private fun requestSecondaryDisplay() {
        secondaryDisplayRetry?.cancel()
        secondaryDisplayRetry = null
        secondaryDisplayRetryCount = 0
        showSecondaryDisplay()
    }

    private fun showSecondaryDisplay() {
        if (!activityResumed || isFinishing) return
        val display = displayManager?.displays?.firstOrNull { candidate ->
            candidate.displayId != Display.DEFAULT_DISPLAY &&
                DeviceProfile.isThorLowerDisplay(candidate.mode.physicalWidth, candidate.mode.physicalHeight)
        }
        if (display == null) {
            dismissSecondaryDisplay()
            return
        }
        secondaryDisplayRetry?.cancel()
        secondaryDisplayRetry = null
        secondaryDisplayRetryCount = 0
        hasThorLowerDisplay = true
        secondaryPresentation?.let { existing ->
            if (existing.display.displayId == display.displayId && existing.isShowing) return
            existing.dismiss()
        }
        val presentation = ThorPresentation(this, display, session)
        secondaryPresentation = presentation
        presentation.setOnDismissListener {
            if (secondaryPresentation === presentation) {
                secondaryPresentation = null
                hasThorLowerDisplay = false
                scheduleSecondaryDisplayRetry()
            }
        }
        runCatching { presentation.show() }.onFailure {
            secondaryPresentation = null
            hasThorLowerDisplay = false
            scheduleSecondaryDisplayRetry()
        }
    }

    private fun scheduleSecondaryDisplayRetry() {
        if (!activityResumed || secondaryDisplayRetry?.isActive == true || secondaryDisplayRetryCount >= 5) return
        secondaryDisplayRetryCount += 1
        secondaryDisplayRetry = lifecycleScope.launch {
            delay(300L * secondaryDisplayRetryCount)
            secondaryDisplayRetry = null
            showSecondaryDisplay()
        }
    }

    private fun dismissSecondaryDisplay() {
        secondaryPresentation?.dismiss()
        secondaryPresentation = null
        hasThorLowerDisplay = false
    }
}

private class ThorPresentation(
    private val activity: ComponentActivity,
    display: Display,
    private val session: ThorSession,
) : Presentation(activity, display) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureThorWindow(window)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        val composeView = ComposeView(context).apply {
            setTag(androidx.lifecycle.runtime.R.id.view_tree_lifecycle_owner, activity)
            setTag(androidx.savedstate.R.id.view_tree_saved_state_registry_owner, activity)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            isFocusable = true
            isFocusableInTouchMode = true
            setContent {
                ThorToolsTheme {
                    ThorControlScreen(session, context, isLowerDisplay = true)
                }
            }
        }
        setContentView(composeView)
        window?.takeKeyEvents(true)
        composeView.requestFocusFromTouch()
    }
}

private fun configureThorWindow(window: android.view.Window?) {
    window ?: return
    WindowCompat.setDecorFitsSystemWindows(window, false)
    val insetsController = WindowCompat.getInsetsController(window, window.decorView)
    insetsController.hide(WindowInsetsCompat.Type.systemBars())
    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
}
