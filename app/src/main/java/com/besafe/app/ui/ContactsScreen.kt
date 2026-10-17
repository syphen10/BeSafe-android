package com.besafe.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.besafe.app.data.TrustedContact

@Composable
fun ContactsScreen(
    contacts: List<TrustedContact>,
    userName: String,
    onUserNameChange: (String) -> Unit,
    onAdd: (name: String, phone: String) -> String?,
    onRemove: (phone: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var showManual by rememberSaveable { mutableStateOf(false) }
    var pickError by remember { mutableStateOf<String?>(null) }
    var pendingRemoval by remember { mutableStateOf<TrustedContact?>(null) }

    val picker = rememberLauncherForActivityResult(PickPhoneContact()) { uri ->
        if (uri != null) {
            val picked = readPickedContact(context, uri)
            pickError = if (picked == null) "Couldn't read that contact." else onAdd(picked.first, picked.second)
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Contacts", "The people who hear from you if something goes wrong") }

        item { YourNameCard(userName, onUserNameChange) }

        item {
            Spacer(Modifier.height(4.dp))
            Text("Trusted contacts", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        pickError = null
                        try {
                            picker.launch(Unit)
                        } catch (e: ActivityNotFoundException) {
                            showManual = true
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                ) {
                    Icon(Icons.Filled.ContactPhone, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("From contacts")
                }
                OutlinedButton(onClick = { showManual = true }, modifier = Modifier.weight(1f).height(48.dp)) {
                    Icon(Icons.Filled.Dialpad, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Enter number")
                }
            }
            pickError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }

        if (contacts.isEmpty()) {
            item {
                EmptyState(
                    Icons.Filled.People,
                    "No trusted contacts yet",
                    "Add family or close friends. Let them know you've added them, so an alert from you isn't a surprise.",
                )
            }
        }
        items(contacts, key = { it.phone }) { contact ->
            AppCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(contact.name)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(contact.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            contact.phone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { pendingRemoval = contact }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Remove ${contact.name}",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }

    if (showManual) {
        ManualContactDialog(onDismiss = { showManual = false }, onSave = onAdd)
    }
    pendingRemoval?.let { contact ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove ${contact.name}?") },
            text = { Text("They will no longer receive your alerts.") },
            confirmButton = {
                TextButton(onClick = {
                    onRemove(contact.phone)
                    pendingRemoval = null
                }) { Text("Remove") }
            },
            dismissButton = { TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun YourNameCard(userName: String, onUserNameChange: (String) -> Unit) {
    // Local copy so typing isn't fighting the DataStore round-trip; seeded once the saved value loads.
    var text by rememberSaveable { mutableStateOf(userName) }
    var seeded by rememberSaveable { mutableStateOf(userName.isNotEmpty()) }
    LaunchedEffect(userName) {
        if (!seeded && userName.isNotEmpty()) {
            text = userName
            seeded = true
        }
    }
    AppCard {
        Text("Your name", style = MaterialTheme.typography.titleMedium)
        Text(
            "Used in your alerts, so contacts know who's asking for help.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = text,
            onValueChange = {
                text = it
                seeded = true
                onUserNameChange(it)
            },
            placeholder = { Text("e.g. Sara Khan") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ManualContactDialog(onDismiss: () -> Unit, onSave: (String, String) -> String?) {
    var name by rememberSaveable { mutableStateOf("") }
    var phone by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add trusted contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        error = null
                    },
                    label = { Text("Phone (with country code)") },
                    singleLine = true,
                    isError = error != null,
                    supportingText = error?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                error = onSave(name, phone)
                if (error == null) onDismiss()
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

/** Opens the system phone-number picker. The returned URI is readable without READ_CONTACTS. */
private class PickPhoneContact : ActivityResultContract<Unit, Uri?>() {
    override fun createIntent(context: Context, input: Unit) =
        Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)

    override fun parseResult(resultCode: Int, intent: Intent?): Uri? =
        intent?.data?.takeIf { resultCode == Activity.RESULT_OK }
}

private fun readPickedContact(context: Context, uri: Uri): Pair<String, String>? = runCatching {
    val projection = arrayOf(
        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
    )
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        if (!cursor.moveToFirst()) return@use null
        val name = cursor.getString(0).orEmpty()
        val number = cursor.getString(1) ?: return@use null
        name to number
    }
}.getOrNull()
