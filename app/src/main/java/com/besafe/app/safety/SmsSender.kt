package com.besafe.app.safety

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat

/** Sends plain SMS through the default SIM. Works with no mobile data or Wi-Fi. */
class SmsSender(context: Context) {

    private val appContext = context.applicationContext

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /** @return how many numbers the message was handed to the radio for (not confirmed delivery). */
    fun sendToAll(numbers: List<String>, message: String): Int {
        if (!hasPermission()) return 0
        val sms = smsManager() ?: return 0
        val parts = sms.divideMessage(message)
        return numbers.count { number ->
            runCatching { sms.sendMultipartTextMessage(number, null, parts, null, null) }.isSuccess
        }
    }

    private fun smsManager(): SmsManager? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }
}
