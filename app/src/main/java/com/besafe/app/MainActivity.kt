package com.besafe.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.doOnPreDraw
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import android.app.Activity
import com.besafe.app.ui.requestPlayReview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.besafe.app.safety.SessionState
import com.besafe.app.ui.ContactsScreen
import com.besafe.app.ui.HomeScreen
import com.besafe.app.ui.MainViewModel
import com.besafe.app.ui.MessagesScreen
import com.besafe.app.ui.theme.BeSafeTheme

/**
 * Startup is staged so something is always moving on screen:
 * 1. System splash (navy + logo) while the process starts.
 * 2. A plain-View loading screen that looks identical but animates (heartbeat + spinner). It's
 *    cheap to draw, and both animations run on the render thread, so they keep moving while
 * 3. the Compose UI is built underneath; once it has drawn and the saved data has loaded, the
 *    loading screen fades away.
 */
class MainActivity : ComponentActivity() {

    private var loadingView: View? = null

    /** Bumped each time the launcher "SOS" shortcut opens the app. */
    private val sosShortcutTaps = MutableStateFlow(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Our loading screen is pixel-identical to the system splash, so swap it out instantly.
        installSplashScreen().setOnExitAnimationListener { it.remove() }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        val root = FrameLayout(this)
        val loading = buildLoadingView()
        root.addView(loading, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(root)
        loadingView = loading
        setLightSystemBars(false)

        // Let the loading screen reach the display first, then build the real UI behind it.
        loading.doOnPreDraw {
            root.post {
                val compose = ComposeView(this).apply {
                    setContent {
                        BeSafeTheme { BeSafeRoot(onReady = ::hideLoading, sosShortcutTaps = sosShortcutTaps) }
                    }
                }
                root.addView(compose, 0, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_OPEN_SOS) sosShortcutTaps.value++
    }

    private fun buildLoadingView(): View {
        val density = resources.displayMetrics.density
        fun dp(value: Int) = (value * density).toInt()

        return FrameLayout(this).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.brand_navy))
            isClickable = true // swallow taps until the real UI is ready
            // Same drawable and size as the system splash icon, so the hand-off is invisible.
            addView(
                ImageView(context).apply {
                    setImageResource(R.drawable.splash_logo_animated)
                    (drawable as? Animatable)?.start()
                },
                FrameLayout.LayoutParams(dp(SPLASH_ICON_DP), dp(SPLASH_ICON_DP), Gravity.CENTER),
            )
            addView(
                ProgressBar(context, null, android.R.attr.progressBarStyleSmall).apply {
                    isIndeterminate = true
                    indeterminateTintList = ColorStateList.valueOf(Color.WHITE)
                    alpha = 0.7f
                    translationY = dp(150).toFloat()
                },
                FrameLayout.LayoutParams(dp(28), dp(28), Gravity.CENTER),
            )
        }
    }

    private fun hideLoading() {
        val loading = loadingView ?: return
        loadingView = null
        val dark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        setLightSystemBars(!dark)
        loading.animate()
            .alpha(0f)
            .setDuration(LOADING_FADE_MS)
            .withEndAction { (loading.parent as? ViewGroup)?.removeView(loading) }
            .start()
    }

    /** White status/nav icons over the navy loading screen; theme-appropriate ones afterwards. */
    private fun setLightSystemBars(light: Boolean) {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = light
            isAppearanceLightNavigationBars = light
        }
    }

    private companion object {
        /** Size the system uses for a splash icon without an icon background. */
        const val SPLASH_ICON_DP = 288
        const val LOADING_FADE_MS = 200L
        const val ACTION_OPEN_SOS = "com.besafe.app.action.OPEN_SOS"
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    Home("Safety", Icons.Filled.Favorite),
    Messages("Messages", Icons.AutoMirrored.Filled.Chat),
    Contacts("Contacts", Icons.Filled.People),
}

private const val READY_TIMEOUT_MS = 2_000L
private const val REVIEW_DELAY_MS = 1_500L

/** Permissions the core features can't work without. */
private val requiredPermissions = arrayOf(
    Manifest.permission.SEND_SMS,
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

/** Requested alongside, but the app still works (just less visibly) if denied. */
private val optionalPermissions =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) arrayOf(Manifest.permission.POST_NOTIFICATIONS) else emptyArray()

private fun Context.has(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

/** SMS plus at least approximate location. */
private fun Context.corePermissionsGranted() =
    has(Manifest.permission.SEND_SMS) &&
        (has(Manifest.permission.ACCESS_FINE_LOCATION) || has(Manifest.permission.ACCESS_COARSE_LOCATION))

@Composable
private fun BeSafeRoot(
    onReady: () -> Unit,
    sosShortcutTaps: StateFlow<Int>,
    vm: MainViewModel = viewModel(),
) {
    val context = LocalContext.current
    val session by vm.session.collectAsState()
    val profile by vm.profile.collectAsState()
    val contacts = profile?.contacts.orEmpty()
    val userName = profile?.userName.orEmpty()
    val history by vm.history.collectAsState()

    // Reveal the UI once saved data is in (so nothing flickers from "empty" to real values) and
    // that frame has been drawn. The timeout is a backstop so a slow disk never traps the user.
    val currentOnReady by rememberUpdatedState(onReady)
    LaunchedEffect(profile != null) {
        if (profile != null) {
            withFrameNanos { }
            currentOnReady()
        }
    }
    LaunchedEffect(Unit) {
        delay(READY_TIMEOUT_MS)
        currentOnReady()
    }

    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var permissionsOk by remember { mutableStateOf(context.corePermissionsGranted()) }
    var askedOnce by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permissionsOk = context.corePermissionsGranted()
    }

    // Re-check when returning from system Settings.
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            permissionsOk = context.corePermissionsGranted()
        }
    }
    LaunchedEffect(profile != null) {
        if (profile != null && !askedOnce && !permissionsOk) {
            askedOnce = true
            permissionLauncher.launch(requiredPermissions + optionalPermissions)
        }
    }

    // An active session always takes over the screen, wherever the user was.
    LaunchedEffect(session) {
        if (session !is SessionState.Idle) tab = Tab.Home
    }

    // Launcher "SOS" shortcut: always land on the SOS button.
    val shortcutTaps by sosShortcutTaps.collectAsState()
    LaunchedEffect(shortcutTaps) {
        if (shortcutTaps > 0) tab = Tab.Home
    }

    // Ask for a Play rating after a check-in ended safely — only once things are calm again.
    val reviewDue by vm.reviewDue.collectAsState()
    LaunchedEffect(reviewDue, session) {
        if (reviewDue && session is SessionState.Idle) {
            delay(REVIEW_DELAY_MS)
            (context as? Activity)?.let { requestPlayReview(it) }
            vm.markReviewRequested()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Box {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                        Tab.entries.forEach { item ->
                            NavigationBarItem(
                                selected = tab == item,
                                onClick = { tab = item },
                                icon = { Icon(item.icon, contentDescription = null) },
                                label = { Text(item.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                ),
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
        ) { padding ->
            Crossfade(targetState = tab, label = "tab", modifier = Modifier.padding(padding)) { current ->
                when (current) {
                    Tab.Home -> HomeScreen(
                        state = session,
                        userName = userName,
                        contacts = contacts,
                        permissionsOk = permissionsOk,
                        onSos = vm::sos,
                        onStartCheckIn = vm::startCheckIn,
                        onSafe = vm::imSafe,
                        onExtend = vm::extend,
                        onDismiss = vm::dismissResult,
                        onGoToContacts = { tab = Tab.Contacts },
                        onFixPermissions = {
                            // After a permanent denial the system dialog no longer shows, so open app settings.
                            val activity = context as? ComponentActivity
                            val canAsk = activity != null && requiredPermissions.any {
                                !context.has(it) && activity.shouldShowRequestPermissionRationale(it)
                            }
                            if (canAsk || !askedOnce) {
                                askedOnce = true
                                permissionLauncher.launch(requiredPermissions + optionalPermissions)
                            } else {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.fromParts("package", context.packageName, null),
                                    ),
                                )
                            }
                        },
                    )
                    Tab.Messages -> MessagesScreen(messages = history, onClear = vm::clearHistory)
                    Tab.Contacts -> ContactsScreen(
                        contacts = contacts,
                        userName = userName,
                        onUserNameChange = vm::setUserName,
                        onAdd = vm::addContact,
                        onRemove = vm::removeContact,
                    )
                }
            }
        }
    }
}
