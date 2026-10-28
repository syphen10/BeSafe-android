package com.besafe.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.besafe.app.data.MessageKind
import com.besafe.app.data.SentMessage
import com.besafe.app.data.TrustedContact
import com.besafe.app.safety.AlertMessages
import com.besafe.app.safety.AlertReason
import com.besafe.app.safety.LocationFix
import com.besafe.app.safety.SessionState
import com.besafe.app.ui.ContactsScreen
import com.besafe.app.ui.HomeScreen
import com.besafe.app.ui.MessagesScreen
import com.besafe.app.ui.theme.BeSafeTheme
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Renders the real app screens with sample data for the Play Store listing.
 * Skipped unless run with -PstoreShots=<output dir>.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w393dp-h852dp-xxhdpi")
class StoreScreenshots {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val outDir = File(System.getProperty("storeShotsDir").orEmpty())
    private val now = System.currentTimeMillis()

    // Fictional numbers (UK Ofcom drama range).
    private val contacts = listOf(
        TrustedContact("Mom", "+447700900111"),
        TrustedContact("Ayesha Khan", "+447700900222"),
        TrustedContact("Ali Raza", "+447700900333"),
    )

    @Before
    fun onlyWhenRequested() {
        assumeTrue("store screenshots not requested", outDir.path.isNotEmpty())
        outDir.mkdirs()
    }

    @Test
    fun home() = shot("home", tab = 0) { m -> home(SessionState.Idle, m) }

    @Test
    fun checkIn() = shot("checkin", tab = 0) { m ->
        home(SessionState.CheckIn(startedAtMillis = now - 14 * 60_000L, endsAtMillis = now + 16 * 60_000L + 12_000L), m)
    }

    @Test
    fun grace() = shot("grace", tab = 0) { m -> home(SessionState.Grace(alertAtMillis = now + 42_000L), m) }

    @Test
    fun messages() = shot("messages", tab = 1) { m ->
        val fix = LocationFix(51.507351, -0.127758, 9f, now - 26 * 60 * 60_000L)
        val history = listOf(
            SentMessage(
                timeMillis = now - 25 * 60 * 60_000L,
                kind = MessageKind.SAFE,
                text = AlertMessages.safe("Sara"),
                recipients = contacts.map { it.name },
                deliveredTo = 3,
            ),
            SentMessage(
                timeMillis = now - 26 * 60 * 60_000L,
                kind = MessageKind.MISSED_CHECK_IN,
                text = AlertMessages.alert("Sara", fix, AlertReason.MISSED_CHECK_IN),
                recipients = contacts.map { it.name },
                deliveredTo = 3,
                mapsLink = AlertMessages.mapsLink(fix),
            ),
            SentMessage(
                timeMillis = now - 4 * 24 * 60 * 60_000L,
                kind = MessageKind.SOS,
                text = AlertMessages.alert("Sara", fix.copy(timeMillis = now - 4 * 24 * 60 * 60_000L), AlertReason.SOS),
                recipients = contacts.take(2).map { it.name },
                deliveredTo = 2,
                mapsLink = AlertMessages.mapsLink(fix),
            ),
        )
        MessagesScreen(messages = history, onClear = {}, modifier = m)
    }

    @Test
    fun contacts() = shot("contacts", tab = 2) { m ->
        ContactsScreen(
            contacts = contacts,
            userName = "Sara Khan",
            onUserNameChange = {},
            onAdd = { _, _ -> null },
            onRemove = {},
            modifier = m,
        )
    }

    @Composable
    private fun home(state: SessionState, modifier: Modifier) {
        HomeScreen(
            state = state,
            userName = "Sara",
            contacts = contacts,
            permissionsOk = true,
            onSos = {},
            onStartCheckIn = {},
            onSafe = {},
            onExtend = {},
            onDismiss = {},
            onGoToContacts = {},
            onFixPermissions = {},
            modifier = modifier,
        )
    }

    private fun shot(name: String, tab: Int, body: @Composable (Modifier) -> Unit) {
        compose.mainClock.autoAdvance = false // SOS idle animation never settles
        compose.setContent {
            BeSafeTheme { AppFrame(selected = tab) { body(it) } }
        }
        compose.mainClock.advanceTimeBy(1_400)
        shadowOf(Looper.getMainLooper()).idle()
        // Draw the window directly: captureToImage() waits for an idle UI, which never comes
        // while the SOS ripple is animating.
        val root = compose.activity.window.decorView
        val bitmap = Bitmap.createBitmap(root.width, root.height, Bitmap.Config.ARGB_8888)
        root.draw(Canvas(bitmap))
        File(outDir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

/** Same bottom navigation as MainActivity's root, so the shots look like the real app. */
@Composable
private fun AppFrame(selected: Int, content: @Composable (Modifier) -> Unit) {
    val tabs = listOf(
        "Safety" to Icons.Filled.Favorite,
        "Messages" to Icons.AutoMirrored.Filled.Chat,
        "Contacts" to Icons.Filled.People,
    )
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                    tabs.forEachIndexed { i, (label, icon) ->
                        NavigationBarItem(
                            selected = i == selected,
                            onClick = {},
                            icon = { Icon(icon, contentDescription = null) },
                            label = { Text(label) },
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
    ) { padding -> content(Modifier.padding(padding)) }
}
