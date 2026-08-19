package dev.adrian.thortools

import android.app.Activity
import android.app.Presentation
import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Bundle
import android.view.Display
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

class MainActivity : ComponentActivity() {
    private lateinit var session: ThorSession
    private var displayManager: DisplayManager? = null
    private var secondaryPresentation: ThorPresentation? = null
    private var activityResumed = false
    private var hasThorLowerDisplay by mutableStateOf(false)

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            showSecondaryDisplay()
        }

        override fun onDisplayRemoved(displayId: Int) {
            if (secondaryPresentation?.display?.displayId == displayId) dismissSecondaryDisplay()
        }

        override fun onDisplayChanged(displayId: Int) {
            showSecondaryDisplay()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        session = ThorSession(this)
        displayManager = getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
        showSecondaryDisplay()
    }

    override fun onStop() {
        activityResumed = false
        dismissSecondaryDisplay()
        displayManager?.unregisterDisplayListener(displayListener)
        super.onStop()
    }

    private fun showSecondaryDisplay() {
        if (!activityResumed || isFinishing) return
        val display = displayManager?.displays?.firstOrNull { candidate ->
            candidate.displayId != Display.DEFAULT_DISPLAY &&
                DeviceProfile.isThorLowerDisplay(candidate.mode.physicalWidth, candidate.mode.physicalHeight)
        }
        if (display == null) {
            hasThorLowerDisplay = false
            return
        }
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
            }
        }
        runCatching { presentation.show() }.onFailure {
            secondaryPresentation = null
            hasThorLowerDisplay = false
        }
    }

    private fun dismissSecondaryDisplay() {
        secondaryPresentation?.dismiss()
        secondaryPresentation = null
        hasThorLowerDisplay = false
    }
}

private class ThorPresentation(
    activity: Activity,
    display: Display,
    private val session: ThorSession,
) : Presentation(activity, display) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE)
        val composeView = ComposeView(context).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ThorToolsTheme {
                    ThorControlScreen(session, context)
                }
            }
        }
        setContentView(composeView)
        composeView.isFocusableInTouchMode = true
        composeView.requestFocus()
    }
}
