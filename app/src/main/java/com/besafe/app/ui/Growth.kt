package com.besafe.app.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory

/**
 * Shows Google Play's in-app rating sheet. Play decides whether it actually appears (it's
 * quota-limited and silent outside Play installs), so failures are ignored.
 */
suspend fun requestPlayReview(activity: Activity) {
    runCatching {
        val manager = ReviewManagerFactory.create(activity)
        val info = manager.requestReview()
        manager.launchReview(activity, info)
    }
}

fun playStoreLink(context: Context) = "https://play.google.com/store/apps/details?id=${context.packageName}"

/** Text sent to a newly added trusted contact so an alert from the user is never a surprise. */
fun inviteMessage(context: Context, contactName: String): String {
    val first = contactName.trim().substringBefore(' ').takeIf { it.isNotEmpty() && !it.first().isDigit() }
    val greeting = if (first != null) "Hi $first, " else "Hi, "
    return greeting + "I've added you as a trusted contact on Be Safe. If I ever need help, " +
        "you'll get a text from me with my location. You can get the app too: ${playStoreLink(context)}"
}

/** Opens the user's own SMS app with the invite pre-filled; they choose whether to send it. */
fun openInviteSms(context: Context, phone: String, contactName: String) {
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(phone)}"))
        .putExtra("sms_body", inviteMessage(context, contactName))
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // No SMS app on this device (e.g. a tablet); nothing to fall back to.
    }
}
