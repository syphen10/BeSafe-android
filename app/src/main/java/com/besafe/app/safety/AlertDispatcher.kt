package com.besafe.app.safety

import android.content.Context
import android.os.BatteryManager
import com.besafe.app.data.MessageKind
import com.besafe.app.data.SentMessage
import com.besafe.app.data.SettingsRepository
import com.besafe.app.data.TrustedContact
import kotlinx.coroutines.flow.first

/** Shown when the hardware can't send SMS at all (Wi-Fi tablets, Fire devices). */
const val NO_SMS_DEVICE =
    "This device can't send text messages, so Be Safe can't alert your contacts from it. Use Be Safe on a phone with a SIM card."

/** Resolves location, builds the message, texts every trusted contact and logs it to history. */
class AlertDispatcher(context: Context, private val settings: SettingsRepository) {

    private val appContext = context.applicationContext
    private val location = LocationProvider(context)
    private val sms = SmsSender(context)

    suspend fun sendAlert(reason: AlertReason, note: String = ""): SessionState {
        val contacts = settings.contacts.first()
        if (contacts.isEmpty()) return SessionState.Failed("No trusted contacts saved.")
        if (!sms.deviceCanSendSms()) return SessionState.Failed(NO_SMS_DEVICE)
        if (!sms.hasPermission()) return SessionState.Failed("SMS permission is missing.")

        val fix = location.currentLocation()
        val message = AlertMessages.alert(settings.userName.first(), fix, reason, note, batteryPercent())
        val kind = if (reason == AlertReason.SOS) MessageKind.SOS else MessageKind.MISSED_CHECK_IN
        val sent = send(contacts, message, kind, fix?.let(AlertMessages::mapsLink))

        return if (sent == 0) {
            SessionState.Failed("Couldn't send SMS. Check your signal, SIM and airplane mode, and call for help directly.")
        } else {
            SessionState.AlertSent(System.currentTimeMillis(), sent, reason, hadLocation = fix != null)
        }
    }

    /** Null where the device doesn't report it (e.g. no battery). */
    private fun batteryPercent(): Int? =
        appContext.getSystemService(BatteryManager::class.java)
            ?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            ?.takeIf { it in 0..100 }

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
        val delivered = sms.sendToAll(contacts.map { it.phone }, message)
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
