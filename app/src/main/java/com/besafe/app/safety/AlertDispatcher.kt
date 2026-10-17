package com.besafe.app.safety

import android.content.Context
import com.besafe.app.data.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Resolves location, builds the message and texts every trusted contact. */
class AlertDispatcher(context: Context, private val settings: SettingsRepository) {

    private val location = LocationProvider(context)
    private val sms = SmsSender(context)

    suspend fun sendAlert(reason: AlertReason): SessionState {
        val contacts = settings.contacts.first()
        if (contacts.isEmpty()) return SessionState.Failed("No trusted contacts saved.")
        if (!sms.hasPermission()) return SessionState.Failed("SMS permission is missing.")

        val fix = location.currentLocation()
        val message = AlertMessages.alert(settings.userName.first(), fix, reason)
        val sent = withContext(Dispatchers.IO) { sms.sendToAll(contacts.map { it.phone }, message) }

        return if (sent == 0) {
            SessionState.Failed("Couldn't send SMS. Check your SIM and signal, and call for help directly.")
        } else {
            SessionState.AlertSent(System.currentTimeMillis(), sent, reason, hadLocation = fix != null)
        }
    }

    /** Follow-up so contacts know a previous alert can be stood down. */
    suspend fun sendSafe(): Int {
        val contacts = settings.contacts.first()
        val message = AlertMessages.safe(settings.userName.first())
        return withContext(Dispatchers.IO) { sms.sendToAll(contacts.map { it.phone }, message) }
    }
}
