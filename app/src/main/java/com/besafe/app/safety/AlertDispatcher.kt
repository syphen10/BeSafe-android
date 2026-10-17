package com.besafe.app.safety

import android.content.Context
import com.besafe.app.data.MessageKind
import com.besafe.app.data.SentMessage
import com.besafe.app.data.SettingsRepository
import com.besafe.app.data.TrustedContact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/** Resolves location, builds the message, texts every trusted contact and logs it to history. */
class AlertDispatcher(context: Context, private val settings: SettingsRepository) {

    private val location = LocationProvider(context)
    private val sms = SmsSender(context)

    suspend fun sendAlert(reason: AlertReason): SessionState {
        val contacts = settings.contacts.first()
        if (contacts.isEmpty()) return SessionState.Failed("No trusted contacts saved.")
        if (!sms.hasPermission()) return SessionState.Failed("SMS permission is missing.")

        val fix = location.currentLocation()
        val message = AlertMessages.alert(settings.userName.first(), fix, reason)
        val kind = if (reason == AlertReason.SOS) MessageKind.SOS else MessageKind.MISSED_CHECK_IN
        val sent = send(contacts, message, kind, fix?.let(AlertMessages::mapsLink))

        return if (sent == 0) {
            SessionState.Failed("Couldn't send SMS. Check your SIM and signal, and call for help directly.")
        } else {
            SessionState.AlertSent(System.currentTimeMillis(), sent, reason, hadLocation = fix != null)
        }
    }

    /** Follow-up so contacts know a previous alert can be stood down. */
    suspend fun sendSafe(): Int {
        val contacts = settings.contacts.first()
        if (contacts.isEmpty()) return 0
        return send(contacts, AlertMessages.safe(settings.userName.first()), MessageKind.SAFE, null)
    }

    private suspend fun send(
        contacts: List<TrustedContact>,
        message: String,
        kind: MessageKind,
        mapsLink: String?,
    ): Int {
        val delivered = withContext(Dispatchers.IO) { sms.sendToAll(contacts.map { it.phone }, message) }
        settings.recordMessage(
            SentMessage(
                timeMillis = System.currentTimeMillis(),
                kind = kind,
                text = message,
                recipients = contacts.map { it.name },
                deliveredTo = delivered.size,
                mapsLink = mapsLink,
            ),
        )
        return delivered.size
    }
}
