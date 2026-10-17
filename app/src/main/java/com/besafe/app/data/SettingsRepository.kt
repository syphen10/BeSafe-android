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

enum class MessageKind { SOS, MISSED_CHECK_IN, SAFE }

/** One alert as it was sent, for the Messages tab. */
@Serializable
data class SentMessage(
    val timeMillis: Long,
    val kind: MessageKind,
    val text: String,
    /** Display names of everyone the message was addressed to. */
    val recipients: List<String>,
    val deliveredTo: Int,
    val mapsLink: String? = null,
) {
    val allSent get() = deliveredTo == recipients.size
}

private val Context.dataStore by preferencesDataStore(name = "besafe")

/** Persists the user's name, trusted contacts and message history on-device only. */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore
    private val json = Json { ignoreUnknownKeys = true }

    val contacts: Flow<List<TrustedContact>> = store.data.map { decodeContacts(it) }

    val userName: Flow<String> = store.data.map { it[USER_NAME].orEmpty() }

    /** Newest first. */
    val history: Flow<List<SentMessage>> = store.data.map { decodeHistory(it) }

    /** Adds [contact] unless a contact with the same number already exists. */
    suspend fun addContact(contact: TrustedContact) {
        store.edit { prefs ->
            val current = decodeContacts(prefs)
            if (current.none { it.phone == contact.phone }) prefs[CONTACTS] = json.encodeToString(current + contact)
        }
    }

    suspend fun removeContact(phone: String) {
        store.edit { prefs ->
            prefs[CONTACTS] = json.encodeToString(decodeContacts(prefs).filterNot { it.phone == phone })
        }
    }

    suspend fun setUserName(name: String) {
        // Saved as typed (called per keystroke); AlertMessages trims when building the SMS.
        store.edit { it[USER_NAME] = name }
    }

    suspend fun recordMessage(message: SentMessage) {
        store.edit { prefs -> prefs.putHistory((listOf(message) + decodeHistory(prefs)).take(MAX_HISTORY)) }
    }

    suspend fun clearHistory() {
        store.edit { it.remove(HISTORY) }
    }

    private fun decodeContacts(prefs: Preferences): List<TrustedContact> =
        prefs[CONTACTS]?.let { raw ->
            runCatching { json.decodeFromString<List<TrustedContact>>(raw) }.getOrNull()
        }.orEmpty()

    private fun decodeHistory(prefs: Preferences): List<SentMessage> =
        prefs[HISTORY]?.let { raw ->
            runCatching { json.decodeFromString<List<SentMessage>>(raw) }.getOrNull()
        }.orEmpty()

    private fun MutablePreferences.putHistory(list: List<SentMessage>) {
        this[HISTORY] = json.encodeToString(list)
    }

    private companion object {
        val CONTACTS = stringPreferencesKey("contacts")
        val USER_NAME = stringPreferencesKey("user_name")
        val HISTORY = stringPreferencesKey("history")
        const val MAX_HISTORY = 200
    }
}
