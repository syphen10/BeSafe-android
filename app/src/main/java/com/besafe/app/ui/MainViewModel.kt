package com.besafe.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.besafe.app.BeSafeApp
import com.besafe.app.data.SentMessage
import com.besafe.app.data.TrustedContact
import com.besafe.app.safety.CheckInService
import com.besafe.app.safety.PhoneNumbers
import com.besafe.app.safety.SessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as BeSafeApp).container
    private val settings = container.settings

    val session: StateFlow<SessionState> = container.session.asStateFlow()

    /** Who the user is and who they alert. Null until the first read from disk completes. */
    data class Profile(val userName: String, val contacts: List<TrustedContact>)

    // Started eagerly so the disk read overlaps with building the UI instead of following it.
    val profile: StateFlow<Profile?> = combine(settings.userName, settings.contacts, ::Profile)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val history: StateFlow<List<SentMessage>> = settings.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val reviewDue: StateFlow<Boolean> = settings.reviewDue
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun markReviewRequested() {
        viewModelScope.launch { settings.markReviewRequested() }
    }

    fun startCheckIn(minutes: Int) = CheckInService.startCheckIn(getApplication(), minutes)

    fun sos() = CheckInService.sos(getApplication())

    fun imSafe() = CheckInService.command(getApplication(), CheckInService.ACTION_SAFE)

    fun extend() = CheckInService.command(getApplication(), CheckInService.ACTION_EXTEND)

    /** Clears a finished result (alert sent / failed) without messaging anyone. */
    fun dismissResult() {
        val state = container.session.value
        if (state is SessionState.AlertSent || state is SessionState.Failed) {
            container.session.value = SessionState.Idle
        }
    }

    /** @return an error message, or null if the contact was saved. */
    fun addContact(name: String, rawPhone: String): String? {
        val phone = PhoneNumbers.normalize(rawPhone)
        if (!PhoneNumbers.isValid(phone)) return "That doesn't look like a valid phone number."
        if (profile.value?.contacts.orEmpty().any { it.phone == phone }) return "That number is already a trusted contact."
        val displayName = name.trim().ifEmpty { phone }
        viewModelScope.launch { settings.addContact(TrustedContact(displayName, phone)) }
        return null
    }

    fun removeContact(phone: String) {
        viewModelScope.launch { settings.removeContact(phone) }
    }

    fun setUserName(name: String) {
        viewModelScope.launch { settings.setUserName(name) }
    }

    fun clearHistory() {
        viewModelScope.launch { settings.clearHistory() }
    }
}
