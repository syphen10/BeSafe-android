package com.besafe.app.safety

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.besafe.app.BeSafeApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Foreground service that owns the safety session: runs the check-in countdown, the grace
 * period, and sends alerts. Being a foreground service (type "location") with a bounded wake
 * lock keeps the timer accurate with the screen off and lets it read location in the background.
 */
class CheckInService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var timerJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private val container get() = (application as BeSafeApp).container
    private val session get() = container.session

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startCheckIn(intent.getLongExtra(EXTRA_DURATION_MS, 0L))
            ACTION_EXTEND -> extend()
            ACTION_SAFE -> markSafe()
            ACTION_SOS -> triggerSos()
            else -> if (timerJob == null) stopSelf()
        }
        // A restarted service would have lost its deadline; never resurrect silently.
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    private fun startCheckIn(durationMs: Long) {
        val now = System.currentTimeMillis()
        val endsAt = now + durationMs.coerceIn(MIN_DURATION_MS, MAX_DURATION_MS)
        if (!goForeground(Notifications.checkIn(this, endsAt))) return
        runTimer(now, endsAt)
    }

    private fun extend() {
        val now = System.currentTimeMillis()
        val (start, newEnd) = when (val state = session.value) {
            is SessionState.CheckIn -> state.startedAtMillis to state.endsAtMillis + EXTEND_MS
            is SessionState.Grace -> now to now + EXTEND_MS
            else -> return
        }
        notify(Notifications.SESSION_ID, Notifications.checkIn(this, newEnd))
        runTimer(start, newEnd)
    }

    private fun markSafe() {
        val previous = session.value
        timerJob?.cancel()
        timerJob = null
        session.value = SessionState.Idle
        scope.launch {
            when (previous) {
                // Contacts were already alerted — tell them to stand down.
                is SessionState.AlertSent -> container.alerts.sendSafe()
                // A check-in that ended well; feeds the "ask for a rating" moment.
                is SessionState.CheckIn, is SessionState.Grace -> container.settings.recordSafeCheckIn()
                else -> Unit
            }
            finish(null)
        }
    }

    private fun triggerSos() {
        if (!goForeground(Notifications.sending(this))) return
        timerJob?.cancel()
        acquireWakeLock(SEND_WAKE_MS)
        timerJob = scope.launch { sendAlert(AlertReason.SOS) }
    }

    private fun runTimer(startedAt: Long, endsAt: Long) {
        timerJob?.cancel()
        acquireWakeLock(endsAt - System.currentTimeMillis() + GRACE_MS + SEND_WAKE_MS)
        session.value = SessionState.CheckIn(startedAt, endsAt)
        timerJob = scope.launch {
            delayUntil(endsAt)
            val alertAt = System.currentTimeMillis() + GRACE_MS
            session.value = SessionState.Grace(alertAt)
            notify(Notifications.SESSION_ID, Notifications.grace(this@CheckInService, alertAt))
            delayUntil(alertAt)
            sendAlert(AlertReason.MISSED_CHECK_IN)
        }
    }

    private suspend fun sendAlert(reason: AlertReason) {
        session.value = SessionState.Sending(reason)
        notify(Notifications.SESSION_ID, Notifications.sending(this))
        val result = container.alerts.sendAlert(reason)
        session.value = result
        finish(Notifications.result(this, result))
    }

    private fun finish(resultNotification: Notification?) {
        timerJob = null
        releaseWakeLock()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        resultNotification?.let { notify(Notifications.RESULT_ID, it) }
        stopSelf()
    }

    /** @return false if Android refused (e.g. location permission revoked); the session is marked failed. */
    private fun goForeground(notification: Notification): Boolean = try {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(this, Notifications.SESSION_ID, notification, type)
        true
    } catch (e: SecurityException) {
        failToStart("Location permission is required: ${e.message}")
    } catch (e: IllegalStateException) {
        failToStart("Android blocked the safety service from starting: ${e.message}")
    }

    private fun failToStart(message: String): Boolean {
        session.value = SessionState.Failed(message)
        stopSelf()
        return false
    }

    private fun notify(id: Int, notification: Notification) {
        getSystemService(NotificationManager::class.java).notify(id, notification)
    }

    private suspend fun delayUntil(timeMillis: Long) {
        delay((timeMillis - System.currentTimeMillis()).coerceAtLeast(0L))
    }

    private fun acquireWakeLock(timeoutMs: Long) {
        releaseWakeLock()
        wakeLock = getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "BeSafe:session")
            .apply { acquire(timeoutMs.coerceAtLeast(SEND_WAKE_MS)) }
    }

    private fun releaseWakeLock() {
        wakeLock?.takeIf { it.isHeld }?.release()
        wakeLock = null
    }

    companion object {
        const val ACTION_START = "com.besafe.app.action.START"
        const val ACTION_EXTEND = "com.besafe.app.action.EXTEND"
        const val ACTION_SAFE = "com.besafe.app.action.SAFE"
        const val ACTION_SOS = "com.besafe.app.action.SOS"
        private const val EXTRA_DURATION_MS = "duration_ms"

        const val GRACE_MS = 60_000L
        private const val EXTEND_MS = 10 * 60_000L
        private const val MIN_DURATION_MS = 60_000L
        const val MAX_MINUTES = 24 * 60
        private const val MAX_DURATION_MS = MAX_MINUTES * 60_000L
        private const val SEND_WAKE_MS = 2 * 60_000L

        fun startCheckIn(context: Context, minutes: Int) {
            val intent = intent(context, ACTION_START).putExtra(EXTRA_DURATION_MS, minutes * 60_000L)
            ContextCompat.startForegroundService(context, intent)
        }

        fun sos(context: Context) {
            ContextCompat.startForegroundService(context, intent(context, ACTION_SOS))
        }

        /** For EXTEND / SAFE, which never need to promote the service to foreground. */
        fun command(context: Context, action: String) {
            context.startService(intent(context, action))
        }

        private fun intent(context: Context, action: String) =
            Intent(context, CheckInService::class.java).setAction(action)
    }
}
