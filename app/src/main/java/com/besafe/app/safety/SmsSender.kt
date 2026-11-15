package com.besafe.app.safety

import android.Manifest
import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicInteger

/** Sends plain SMS through the default SIM. Works with no mobile data or Wi-Fi. */
class SmsSender(context: Context) {

    private val appContext = context.applicationContext

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /** False on Wi-Fi-only tablets, Fire devices and other hardware that can't send texts at all. */
    fun deviceCanSendSms(): Boolean {
        val feature = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PackageManager.FEATURE_TELEPHONY_MESSAGING
        } else {
            PackageManager.FEATURE_TELEPHONY
        }
        return appContext.packageManager.hasSystemFeature(feature)
    }

    /**
     * Sends [message] to every number and waits for the radio to report each result, so a phone in
     * airplane mode or without signal is reported as a failure instead of "sent".
     *
     * @return the numbers the message was sent to. A number whose result doesn't arrive within
     * [RESULT_TIMEOUT_MS] counts as sent: it's queued with the carrier and nothing reported an error.
     */
    suspend fun sendToAll(numbers: List<String>, message: String): Set<String> {
        if (!hasPermission() || !deviceCanSendSms()) return emptySet()
        val sms = smsManager() ?: return emptySet()
        val parts = sms.divideMessage(message)
        val action = "${appContext.packageName}.SMS_SENT.${nextSendId.incrementAndGet()}"

        // One pending result per message part; a number succeeds only if all of its parts do.
        val results = numbers.associateWith { List(parts.size) { CompletableDeferred<Boolean>() } }
        val byRequestCode = HashMap<Int, CompletableDeferred<Boolean>>()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                val code = intent.getIntExtra(EXTRA_REQUEST_CODE, -1)
                synchronized(byRequestCode) { byRequestCode[code] }?.complete(resultCode == Activity.RESULT_OK)
            }
        }
        ContextCompat.registerReceiver(appContext, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)

        try {
            val handedOver = withContext(Dispatchers.IO) {
                numbers.filter { number ->
                    val sentIntents = ArrayList(
                        results.getValue(number).map { deferred ->
                            val requestCode = nextRequestCode.incrementAndGet()
                            synchronized(byRequestCode) { byRequestCode[requestCode] = deferred }
                            sentIntent(action, requestCode)
                        },
                    )
                    runCatching { sms.sendMultipartTextMessage(number, null, parts, sentIntents, null) }
                        .onFailure { results.getValue(number).forEach { it.complete(false) } }
                        .isSuccess
                }
            }
            if (handedOver.isEmpty()) return emptySet()
            withTimeoutOrNull(RESULT_TIMEOUT_MS) { results.values.flatten().awaitAll() }
            return handedOver.filterTo(mutableSetOf()) { number ->
                results.getValue(number).all { !it.isCompleted || it.await() }
            }
        } finally {
            appContext.unregisterReceiver(receiver)
        }
    }

    private fun sentIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            requestCode,
            Intent(action).setPackage(appContext.packageName).putExtra(EXTRA_REQUEST_CODE, requestCode),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT,
        )

    private fun smsManager(): SmsManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

    private companion object {
        const val EXTRA_REQUEST_CODE = "request_code"
        const val RESULT_TIMEOUT_MS = 30_000L
        val nextSendId = AtomicInteger()
        val nextRequestCode = AtomicInteger()
    }
}
