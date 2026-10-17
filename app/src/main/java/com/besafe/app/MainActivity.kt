package com.besafe.app

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.besafe.app.safety.SessionState
import com.besafe.app.ui.ContactsScreen
import com.besafe.app.ui.HomeScreen
import com.besafe.app.ui.MainViewModel
import com.besafe.app.ui.theme.BeSafeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BeSafeTheme { BeSafeRoot() } }
    }
}

private enum class Tab(val label: String) { Home("Safety"), Contacts("Contacts") }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BeSafeRoot(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val session by vm.session.collectAsState()
    val contacts by vm.contacts.collectAsState()
    val userName by vm.userName.collectAsState()

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
    LaunchedEffect(Unit) {
        if (!askedOnce && !permissionsOk) {
            askedOnce = true
            permissionLauncher.launch(requiredPermissions + optionalPermissions)
        }
    }

    // An active session always takes over the screen, wherever the user was.
    LaunchedEffect(session) {
        if (session !is SessionState.Idle) tab = Tab.Home
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Be Safe") }) },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = {
                            Icon(
                                if (item == Tab.Home) Icons.Filled.Shield else Icons.Filled.People,
                                contentDescription = null,
                            )
                        },
                        label = { Text(item.label) },
                    )
                }
            }
        },
    ) { padding ->
        when (tab) {
            Tab.Home -> HomeScreen(
                state = session,
                hasContacts = contacts.isNotEmpty(),
                permissionsOk = permissionsOk,
                onSos = vm::sos,
                onStartCheckIn = vm::startCheckIn,
                onSafe = vm::imSafe,
                onExtend = vm::extend,
                onDismiss = vm::dismissResult,
                onGoToContacts = { tab = Tab.Contacts },
                onFixPermissions = {
                    // After a permanent denial the system dialog no longer shows, so fall back to app settings.
                    val activity = context as? ComponentActivity
                    val canAsk = activity != null && requiredPermissions.any {
                        !context.has(it) && activity.shouldShowRequestPermissionRationale(it)
                    }
                    if (canAsk || !askedOnce) {
                        askedOnce = true
                        permissionLauncher.launch(requiredPermissions + optionalPermissions)
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
                        )
                    }
                },
                modifier = Modifier.padding(padding),
            )
            Tab.Contacts -> ContactsScreen(
                contacts = contacts,
                userName = userName,
                onUserNameChange = vm::setUserName,
                onAdd = vm::addContact,
                onRemove = vm::removeContact,
                modifier = Modifier.padding(padding),
            )
        }
    }
}
