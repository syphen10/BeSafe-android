package com.besafe.app.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** A person who receives SMS alerts. [phone] is stored normalised (see [com.besafe.app.safety.PhoneNumbers]). */
@Serializable
data class TrustedContact(val name: String, val phone: String)

private val Context.dataStore by preferencesDataStore(name = "besafe")

/** Persists the user's name and trusted contacts on-device only — nothing leaves the phone except the alert SMS. */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    val contacts: Flow<List<TrustedContact>> = store.data.map { decodeContacts(it) }

    val userName: Flow<String> = store.data.map { it[USER_NAME].orEmpty() }

    /** Adds [contact] unless a contact with the same number already exists. */
    suspend fun addContact(contact: TrustedContact) {
        store.edit { prefs ->
            val current = decodeContacts(prefs)
            if (current.none { it.phone == contact.phone }) prefs.putContacts(current + contact)
        }
    }

    suspend fun removeContact(phone: String) {
        store.edit { prefs -> prefs.putContacts(decodeContacts(prefs).filterNot { it.phone == phone }) }
    }

    suspend fun setUserName(name: String) {
        // Saved as typed (called per keystroke); AlertMessages trims when building the SMS.
        store.edit { it[USER_NAME] = name }
    }

    private fun decodeContacts(prefs: Preferences): List<TrustedContact> =
        prefs[CONTACTS]?.let { raw ->
            runCatching { json.decodeFromString<List<TrustedContact>>(raw) }.getOrNull()
        }.orEmpty()

    private fun MutablePreferences.putContacts(list: List<TrustedContact>) {
        this[CONTACTS] = json.encodeToString(list)
    }

    private companion object {
        val CONTACTS = stringPreferencesKey("contacts")
        val USER_NAME = stringPreferencesKey("user_name")
    }
}
