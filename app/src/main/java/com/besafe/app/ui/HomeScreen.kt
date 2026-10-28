package com.besafe.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.besafe.app.data.TrustedContact
import com.besafe.app.safety.AlertReason
import com.besafe.app.safety.CheckInService
import com.besafe.app.safety.SessionState
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

@Composable
fun HomeScreen(
    state: SessionState,
    userName: String,
    contacts: List<TrustedContact>,
    permissionsOk: Boolean,
    onSos: () -> Unit,
    onStartCheckIn: (Int) -> Unit,
    onSafe: () -> Unit,
    onExtend: () -> Unit,
    onDismiss: () -> Unit,
    onGoToContacts: () -> Unit,
    onFixPermissions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        AnimatedContent(
            targetState = state,
            contentKey = { it::class },
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "session",
        ) { s ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                when (s) {
                    SessionState.Idle -> IdleContent(
                        userName, contacts, permissionsOk, onSos, onStartCheckIn, onGoToContacts, onFixPermissions,
                    )
                    is SessionState.CheckIn -> CheckInContent(s, onSafe, onExtend, onSos)
                    is SessionState.Grace -> GraceContent(s.alertAtMillis, onSafe, onExtend)
                    is SessionState.Sending -> SendingContent()
                    is SessionState.AlertSent -> SentContent(s, onSafe, onDismiss)
                    is SessionState.Failed -> FailedContent(s.message, onDismiss)
                }
            }
        }

        Text(
            "In immediate danger? Call your local emergency number first.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun IdleContent(
    userName: String,
    contacts: List<TrustedContact>,
    permissionsOk: Boolean,
    onSos: () -> Unit,
    onStartCheckIn: (Int) -> Unit,
    onGoToContacts: () -> Unit,
    onFixPermissions: () -> Unit,
) {
    var minutes by rememberSaveable { mutableIntStateOf(30) }
    val ready = permissionsOk && contacts.isNotEmpty()
    val firstName = userName.trim().substringBefore(' ')

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            if (firstName.isEmpty()) greeting() else "${greeting()}, $firstName",
            style = MaterialTheme.typography.headlineMedium,
        )
        // When something is missing the status itself is the fix: a flat tappable row.
        when {
            !permissionsOk -> ActionRow(
                "Allow SMS & location access",
                Icons.Filled.LocationOn,
                container = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                onClick = onFixPermissions,
            )
            contacts.isEmpty() -> ActionRow(
                "Add a trusted contact",
                Icons.Filled.PersonAdd,
                container = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onGoToContacts,
            )
            else -> StatusLine(
                "Ready · ${contacts.size} trusted contact${if (contacts.size == 1) "" else "s"}",
                Icons.Filled.CheckCircle,
                tint = MaterialTheme.colorScheme.secondary,
            )
        }
    }

    SosButton(enabled = ready, onTriggered = onSos, modifier = Modifier.padding(vertical = 4.dp))
    Text(
        when {
            !permissionsOk -> "SOS turns on once Be Safe can send SMS and see your location."
            contacts.isEmpty() -> "SOS turns on once you've added someone to alert."
            else -> "Hold to text your trusted contacts where you are."
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )

    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(
                Icons.Filled.Timer,
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer,
                size = 40.dp,
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Check-in timer", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Walking home, a date, a late taxi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        DurationPicker(selectedMinutes = minutes, onSelect = { minutes = it })
        Text(
            "If you haven't tapped \"I'm safe\" by ${clockTime(System.currentTimeMillis() + minutes * 60_000L)}, " +
                "your contacts are alerted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = { onStartCheckIn(minutes) },
            enabled = ready,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) { Text("Start ${formatMinutes(minutes)} check-in") }
    }

    if (contacts.isNotEmpty()) {
        AppCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Who gets alerted", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onGoToContacts) { Text("Manage") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                contacts.take(5).forEach { Avatar(it.name, size = 40.dp) }
            }
            Text(
                contacts.joinToString { it.name.substringBefore(' ') },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CheckInContent(state: SessionState.CheckIn, onSafe: () -> Unit, onExtend: () -> Unit, onSos: () -> Unit) {
    val remaining = rememberRemaining(state.endsAtMillis)
    val total = (state.endsAtMillis - state.startedAtMillis).coerceAtLeast(1L)

    ScreenHeader("Check-in active", "Arrive by ${clockTime(state.endsAtMillis)}")
    CountdownRing(
        fraction = remaining.toFloat() / total,
        label = formatDuration(remaining),
        caption = "until your contacts are alerted",
        color = MaterialTheme.colorScheme.primary,
    )
    SafeButton(onSafe)
    OutlinedButton(onClick = onExtend, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Add 10 minutes") }
    Text(
        "Feeling unsafe right now?",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    SosButton(enabled = true, onTriggered = onSos, size = 120.dp)
}

@Composable
private fun GraceContent(alertAt: Long, onSafe: () -> Unit, onExtend: () -> Unit) {
    val remaining = rememberRemaining(alertAt)
    ScreenHeader("Are you safe?", "Your time is up. Let your contacts know you're OK.")
    CountdownRing(
        fraction = remaining.toFloat() / CheckInService.GRACE_MS,
        label = formatDuration(remaining),
        caption = "until alert is sent",
        color = MaterialTheme.colorScheme.error,
    )
    SafeButton(onSafe)
    OutlinedButton(onClick = onExtend, modifier = Modifier.fillMaxWidth().height(48.dp)) {
        Text("I need 10 more minutes")
    }
}

@Composable
private fun SendingContent() {
    Spacer(Modifier.height(48.dp))
    CircularProgressIndicator(strokeWidth = 4.dp, modifier = Modifier.size(56.dp))
    Text("Sending your alert", style = MaterialTheme.typography.titleLarge)
    Text(
        "Finding your location and texting your contacts…",
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SentContent(state: SessionState.AlertSent, onSafe: () -> Unit, onDismiss: () -> Unit) {
    val why = if (state.reason == AlertReason.SOS) "SOS" else "Missed check-in"
    Spacer(Modifier.height(16.dp))
    IconBadge(
        Icons.Filled.CheckCircle,
        container = MaterialTheme.colorScheme.primaryContainer,
        content = MaterialTheme.colorScheme.onPrimaryContainer,
        size = 72.dp,
    )
    Text("Your contacts have been alerted", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
    Text(
        "$why alert sent to ${state.recipients} contact${if (state.recipients == 1) "" else "s"} " +
            "at ${clockTime(state.sentAtMillis)}." +
            if (state.hadLocation) "" else "\nYour location couldn't be found, so it wasn't included.",
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Button(
        onClick = onSafe,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
    ) { Text("I'm safe now: tell my contacts") }
    TextButton(onClick = onDismiss) { Text("Close without messaging") }
}

@Composable
private fun FailedContent(message: String, onDismiss: () -> Unit) {
    Spacer(Modifier.height(16.dp))
    IconBadge(
        Icons.Filled.ErrorOutline,
        container = MaterialTheme.colorScheme.errorContainer,
        content = MaterialTheme.colorScheme.error,
        size = 72.dp,
    )
    Text("Alert not sent", style = MaterialTheme.typography.headlineSmall)
    Text(message, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("OK") }
}

@Composable
private fun SafeButton(onSafe: () -> Unit) {
    Button(
        onClick = onSafe,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
    ) { Text("I'm safe", fontSize = 20.sp, fontWeight = FontWeight.Bold) }
}

/** Large ring that drains as time runs out, with the remaining time in the middle. */
@Composable
private fun CountdownRing(fraction: Float, label: String, caption: String, color: Color) {
    val track = MaterialTheme.colorScheme.surfaceVariant
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = Stroke(width = 14.dp.toPx(), cap = StrokeCap.Round)
            val inset = stroke.width / 2
            val arcSize = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
            drawArc(track, 0f, 360f, false, topLeft, arcSize, style = stroke)
            drawArc(color, -90f, 360f * fraction.coerceIn(0f, 1f), false, topLeft, arcSize, style = stroke)
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 52.sp, fontWeight = FontWeight.Bold, color = color)
            Text(
                caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.offset(y = (-4).dp),
            )
        }
    }
}

/** Milliseconds left until [deadline], refreshed every second. */
@Composable
private fun rememberRemaining(deadline: Long): Long {
    val remaining by produceState(deadline - System.currentTimeMillis(), deadline) {
        while (true) {
            value = (deadline - System.currentTimeMillis()).coerceAtLeast(0L)
            delay(1_000)
        }
    }
    return remaining
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    else -> "Good evening"
}

fun clockTime(millis: Long): String = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(millis))

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms + 999) / 1_000
    val h = totalSeconds / 3_600
    val m = (totalSeconds % 3_600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
