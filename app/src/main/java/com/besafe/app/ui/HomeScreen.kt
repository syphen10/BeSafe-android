package com.besafe.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.besafe.app.safety.AlertReason
import com.besafe.app.safety.SessionState
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date

private val durations = listOf(15, 30, 45, 60, 120)

@Composable
fun HomeScreen(
    state: SessionState,
    hasContacts: Boolean,
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
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (!permissionsOk) {
            WarningCard(
                "Be Safe needs SMS and location permission to alert your contacts.",
                "Grant permissions",
                onFixPermissions,
            )
        } else if (!hasContacts) {
            WarningCard("Add at least one trusted contact to receive your alerts.", "Add contacts", onGoToContacts)
        }
        val ready = permissionsOk && hasContacts

        when (state) {
            SessionState.Idle -> IdleContent(ready, onSos, onStartCheckIn)
            is SessionState.CheckIn -> CheckInContent(state.endsAtMillis, onSafe, onExtend, onSos)
            is SessionState.Grace -> GraceContent(state.alertAtMillis, onSafe, onExtend)
            is SessionState.Sending -> SendingContent()
            is SessionState.AlertSent -> SentContent(state, onSafe, onDismiss)
            is SessionState.Failed -> FailedContent(state.message, onDismiss)
        }

        Text(
            "In immediate danger? Call your local emergency number first.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdleContent(ready: Boolean, onSos: () -> Unit, onStartCheckIn: (Int) -> Unit) {
    var minutes by rememberSaveable { mutableIntStateOf(30) }

    SosButton(enabled = ready, onTriggered = onSos)
    Text(
        "Texts your trusted contacts with your live location.",
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Check-in timer", style = MaterialTheme.typography.titleMedium)
            Text(
                "Walking home or meeting someone new? If you don't tap \"I'm safe\" in time, " +
                    "your contacts are alerted automatically.",
                style = MaterialTheme.typography.bodyMedium,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                durations.forEach { option ->
                    FilterChip(
                        selected = minutes == option,
                        onClick = { minutes = option },
                        label = { Text(if (option < 60) "$option min" else "${option / 60} h") },
                    )
                }
            }
            Button(onClick = { onStartCheckIn(minutes) }, enabled = ready, modifier = Modifier.fillMaxWidth()) {
                Text("Start $minutes-minute check-in")
            }
        }
    }
}

@Composable
private fun CheckInContent(endsAt: Long, onSafe: () -> Unit, onExtend: () -> Unit, onSos: () -> Unit) {
    val remaining = rememberRemaining(endsAt)
    Text("Check-in active", style = MaterialTheme.typography.titleLarge)
    Text(formatDuration(remaining), fontSize = 64.sp, fontWeight = FontWeight.Bold)
    Text("Your contacts will be alerted when this reaches zero.", textAlign = TextAlign.Center)
    SafeButton(onSafe)
    OutlinedButton(onClick = onExtend, modifier = Modifier.fillMaxWidth()) { Text("Add 10 minutes") }
    Spacer(Modifier.height(8.dp))
    SosButton(enabled = true, onTriggered = onSos)
}

@Composable
private fun GraceContent(alertAt: Long, onSafe: () -> Unit, onExtend: () -> Unit) {
    val remaining = rememberRemaining(alertAt)
    Text(
        "Are you safe?",
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.error,
        fontWeight = FontWeight.Bold,
    )
    Text("Alerting your contacts in", textAlign = TextAlign.Center)
    Text(
        formatDuration(remaining),
        fontSize = 64.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.error,
    )
    SafeButton(onSafe)
    OutlinedButton(onClick = onExtend, modifier = Modifier.fillMaxWidth()) { Text("I need 10 more minutes") }
}

@Composable
private fun SendingContent() {
    CircularProgressIndicator()
    Text("Getting your location and texting your contacts…", textAlign = TextAlign.Center)
}

@Composable
private fun SentContent(state: SessionState.AlertSent, onSafe: () -> Unit, onDismiss: () -> Unit) {
    val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(state.sentAtMillis))
    val why = if (state.reason == AlertReason.SOS) "SOS" else "Missed check-in"
    StatusCard(
        title = "Alert sent",
        body = "$why alert texted to ${state.recipients} contact(s) at $time." +
            if (state.hadLocation) "" else "\nYour location couldn't be determined, so it wasn't included.",
    )
    Button(
        onClick = onSafe,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
    ) { Text("I'm safe now — tell my contacts") }
    TextButton(onClick = onDismiss) { Text("Close without messaging") }
}

@Composable
private fun FailedContent(message: String, onDismiss: () -> Unit) {
    StatusCard(title = "Alert NOT sent", body = message, isError = true)
    Button(onClick = onDismiss) { Text("OK") }
}

@Composable
private fun SafeButton(onSafe: () -> Unit) {
    Button(
        onClick = onSafe,
        modifier = Modifier.fillMaxWidth().height(64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
    ) { Text("I'M SAFE", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun WarningCard(text: String, action: String, onAction: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text, color = MaterialTheme.colorScheme.onErrorContainer)
            Button(onClick = onAction) { Text(action) }
        }
    }
}

@Composable
private fun StatusCard(title: String, body: String, isError: Boolean = false) {
    val colors = if (isError) {
        CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    } else {
        CardDefaults.cardColors()
    }
    Card(colors = colors, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body)
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

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms + 999) / 1_000
    val h = totalSeconds / 3_600
    val m = (totalSeconds % 3_600) / 60
    val s = totalSeconds % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
