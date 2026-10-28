package com.besafe.app.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.TimerOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.besafe.app.ads.InlineBannerAd
import com.besafe.app.data.MessageKind
import com.besafe.app.data.SentMessage
import java.util.Calendar

/** Log of every alert and "I'm safe" message Be Safe has sent, newest first, grouped by day. */
@Composable
fun MessagesScreen(messages: List<SentMessage>, onClear: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    val grouped = remember(messages) { messages.groupBy { dayLabel(context, it.timeMillis) } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ScreenHeader(
                "Messages",
                "Every alert Be Safe has sent for you",
                trailing = if (messages.isNotEmpty()) {
                    { TextButton(onClick = { confirmClear = true }) { Text("Clear") } }
                } else {
                    null
                },
            )
        }
        if (messages.isEmpty()) {
            item {
                EmptyState(
                    Icons.AutoMirrored.Filled.Chat,
                    "No messages yet",
                    "When you send an SOS, miss a check-in or tell your contacts you're safe, it'll be listed here.",
                )
            }
        }
        grouped.forEach { (day, dayMessages) ->
            item(key = "header-$day") {
                Text(
                    day,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            items(dayMessages, key = { it.timeMillis }) { MessageCard(it) }
        }
        item(key = "ad") { InlineBannerAd(horizontalPadding = 20.dp) }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear message history?") },
            text = { Text("This only removes the list in Be Safe. Messages already sent can't be unsent.") },
            confirmButton = {
                TextButton(onClick = {
                    onClear()
                    confirmClear = false
                }) { Text("Clear") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MessageCard(message: SentMessage) {
    val context = LocalContext.current
    var expanded by rememberSaveable(message.timeMillis) { mutableStateOf(false) }
    val style = kindStyle(message.kind)

    AppCard(modifier = Modifier.animateContentSize()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(style.icon, container = style.container, content = style.content, size = 40.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(style.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    clockTime(message.timeMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DeliveryLabel(message)
        }

        Text(
            "To " + message.recipients.joinToString(),
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(
            message.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = if (expanded) Int.MAX_VALUE else 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                .clickable { expanded = !expanded }
                .padding(12.dp),
        )

        message.mapsLink?.let { link ->
            TextButton(
                onClick = {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)))
                    } catch (e: ActivityNotFoundException) {
                        // No browser or maps app; nothing sensible to fall back to.
                    }
                },
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                Icon(Icons.Filled.Map, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("View shared location")
            }
        }
    }
}

@Composable
private fun DeliveryLabel(message: SentMessage) {
    val (text, color) = when {
        message.deliveredTo == 0 -> "Not sent" to MaterialTheme.colorScheme.error
        message.allSent -> "Sent" to MaterialTheme.colorScheme.secondary
        else -> "Sent ${message.deliveredTo}/${message.recipients.size}" to MaterialTheme.colorScheme.tertiary
    }
    Text(text, style = MaterialTheme.typography.labelLarge, color = color)
}

private data class KindStyle(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val container: Color,
    val content: Color,
)

@Composable
private fun kindStyle(kind: MessageKind): KindStyle {
    val c = MaterialTheme.colorScheme
    return when (kind) {
        MessageKind.SOS -> KindStyle("SOS alert", Icons.Filled.NotificationsActive, c.errorContainer, c.error)
        MessageKind.MISSED_CHECK_IN -> KindStyle("Missed check-in", Icons.Filled.TimerOff, c.tertiaryContainer, c.tertiary)
        MessageKind.SAFE -> KindStyle("I'm safe", Icons.Filled.Favorite, c.secondaryContainer, c.secondary)
    }
}

private fun dayLabel(context: android.content.Context, millis: Long): String {
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val now = Calendar.getInstance()
    fun sameDay(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    val yesterday = (now.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        sameDay(then, now) -> "Today"
        sameDay(then, yesterday) -> "Yesterday"
        else -> DateUtils.formatDateTime(
            context,
            millis,
            DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_ALL,
        )
    }
}
