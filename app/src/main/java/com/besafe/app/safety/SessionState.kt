package com.besafe.app.safety

/** Why an alert was sent — changes the wording contacts receive. */
enum class AlertReason { SOS, MISSED_CHECK_IN }

/** App-wide safety session, owned by [CheckInService] and observed by the UI. */
sealed interface SessionState {
    data object Idle : SessionState

    /** Check-in timer running from [startedAtMillis]; alert flow starts at [endsAtMillis]. */
    data class CheckIn(val startedAtMillis: Long, val endsAtMillis: Long) : SessionState

    /** Timer expired; the user has until [alertAtMillis] to confirm they're safe. */
    data class Grace(val alertAtMillis: Long) : SessionState

    data class Sending(val reason: AlertReason) : SessionState

    data class AlertSent(
        val sentAtMillis: Long,
        val recipients: Int,
        val reason: AlertReason,
        val hadLocation: Boolean,
    ) : SessionState

    data class Failed(val message: String) : SessionState
}
