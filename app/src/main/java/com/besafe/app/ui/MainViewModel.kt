package com.besafe.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.besafe.app.BeSafeApp
import com.besafe.app.data.TrustedContact
import com.besafe.app.safety.CheckInService
import com.besafe.app.safety.PhoneNumbers
import com.besafe.app.safety.SessionState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = (app as BeSafeApp).container

    val session: StateFlow<SessionState> = container.session.asStateFlow()

    val contacts: StateFlow<List<TrustedContact>> = container.settings.contacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val userName: StateFlow<String> = container.settings.userName
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

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
        if (contacts.value.any { it.phone == phone }) return "That number is already a trusted contact."
        val displayName = name.trim().ifEmpty { phone }
        viewModelScope.launch { container.settings.addContact(TrustedContact(displayName, phone)) }
        return null
    }

    fun removeContact(phone: String) {
        viewModelScope.launch { container.settings.removeContact(phone) }
    }

    fun setUserName(name: String) {
        viewModelScope.launch { container.settings.setUserName(name) }
    }
}
