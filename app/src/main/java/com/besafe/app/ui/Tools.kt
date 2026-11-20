package com.besafe.app.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.TelephonyManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.LocalPhone
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

/** Full-screen tools that sit on top of the tabs. */
enum class SafetyTool { FakeCall, Siren }

/** Local emergency number, from the SIM/network country, falling back to the phone's region. */
object EmergencyNumbers {
    private val byCountry = mapOf(
        "us" to "911", "ca" to "911", "mx" to "911", "pr" to "911",
        "gb" to "999", "ie" to "112",
        "pk" to "15", "in" to "112", "bd" to "999", "ae" to "999", "sa" to "911",
        "au" to "000", "nz" to "111", "za" to "112", "ng" to "112", "ke" to "999",
        "jp" to "110", "cn" to "110", "kr" to "112", "ph" to "911", "my" to "999", "sg" to "999",
        "br" to "190", "ar" to "911", "tr" to "112", "eg" to "122",
    )

    fun forDevice(context: Context): String {
        val tm = context.getSystemService(TelephonyManager::class.java)
        val country = listOfNotNull(tm?.networkCountryIso, tm?.simCountryIso, Locale.getDefault().country)
            .map { it.lowercase(Locale.ROOT) }
            .firstOrNull { it.isNotBlank() }
        return byCountry[country] ?: "112"
    }

    /** Opens the dialer with the number typed in; the user presses call. Needs no permission. */
    fun dial(context: Context, number: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            // No dialer (some tablets): nothing sensible to open.
        }
    }
}

/** Row of quick tools on the Safety screen. */
@Composable
fun QuickTools(onOpen: (SafetyTool) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val emergency = remember { EmergencyNumbers.forDevice(context) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Quick tools", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ToolTile(
                "Fake call",
                Icons.Filled.PhoneInTalk,
                MaterialTheme.colorScheme.tertiaryContainer,
                MaterialTheme.colorScheme.onTertiaryContainer,
                Modifier.weight(1f),
            ) { onOpen(SafetyTool.FakeCall) }
            ToolTile(
                "Siren",
                Icons.Filled.Campaign,
                MaterialTheme.colorScheme.primaryContainer,
                MaterialTheme.colorScheme.onPrimaryContainer,
                Modifier.weight(1f),
            ) { onOpen(SafetyTool.Siren) }
            ToolTile(
                "Call $emergency",
                Icons.Filled.LocalPhone,
                MaterialTheme.colorScheme.errorContainer,
                MaterialTheme.colorScheme.onErrorContainer,
                Modifier.weight(1f),
            ) { EmergencyNumbers.dial(context, emergency) }
        }
    }
}

@Composable
private fun ToolTile(
    label: String,
    icon: ImageVector,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = container, contentColor = content, modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(26.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

// ---- Fake call ------------------------------------------------------------------------------

private enum class CallPhase { Waiting, Ringing, Talking }

private val fakeCallDelays = listOf(5, 30, 60)

/**
 * A believable incoming call to get out of an uncomfortable situation: pick a delay, put the phone
 * away, and it rings with the phone's own ringtone. The screen is kept on while waiting so the
 * call can appear on time.
 */
@Composable
fun FakeCallScreen(callerName: String, onClose: () -> Unit) {
    var phase by rememberSaveable { mutableStateOf(CallPhase.Waiting) }
    var delaySeconds by rememberSaveable { mutableIntStateOf(fakeCallDelays.first()) }
    var started by rememberSaveable { mutableStateOf(false) }
    KeepScreenOn()

    when (phase) {
        CallPhase.Waiting -> FakeCallSetup(
            callerName = callerName,
            delaySeconds = delaySeconds,
            started = started,
            onDelay = { delaySeconds = it },
            onStart = { started = true },
            onCancel = onClose,
            onDue = { phase = CallPhase.Ringing },
        )
        CallPhase.Ringing -> RingingScreen(callerName, onAnswer = { phase = CallPhase.Talking }, onDecline = onClose)
        CallPhase.Talking -> TalkingScreen(callerName, onEnd = onClose)
    }
}

@Composable
private fun FakeCallSetup(
    callerName: String,
    delaySeconds: Int,
    started: Boolean,
    onDelay: (Int) -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit,
    onDue: () -> Unit,
) {
    var left by remember(started) { mutableIntStateOf(delaySeconds) }
    if (started) {
        LaunchedEffect(Unit) {
            while (left > 0) {
                delay(1_000)
                left--
            }
            onDue()
        }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        IconBadge(
            Icons.Filled.PhoneInTalk,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            content = MaterialTheme.colorScheme.onTertiaryContainer,
            size = 80.dp,
        )
        Text("Fake call", style = MaterialTheme.typography.headlineMedium)
        if (!started) {
            Text(
                "Your phone will ring with a call from $callerName, so you have a reason to step away.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                fakeCallDelays.forEach { s ->
                    FilterChip(
                        selected = s == delaySeconds,
                        onClick = { onDelay(s) },
                        label = { Text(if (s < 60) "In $s sec" else "In 1 min") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary,
                        ),
                    )
                }
            }
            Button(onClick = onStart, modifier = Modifier.fillMaxWidth().height(56.dp)) { Text("Start") }
        } else {
            Text("Ringing in $left s", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Text(
                "Keep this screen open. You can put the phone in your pocket.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onCancel) { Text("Cancel") }
    }
}

/** Dark, phone-app-like incoming call screen. */
@Composable
private fun RingingScreen(callerName: String, onAnswer: () -> Unit, onDecline: () -> Unit) {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val stopRinging = startRinging(context)
        onDispose { stopRinging() }
    }
    CallBackdrop {
        Text("Incoming call", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(16.dp))
        Avatar(callerName, size = 112.dp)
        Spacer(Modifier.height(16.dp))
        Text(callerName, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
        Text("Mobile", color = Color.White.copy(alpha = 0.7f))
        Spacer(Modifier.weight(1f))
        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
            RoundCallButton(Icons.Filled.CallEnd, Color(0xFFDC2626), "Decline", onDecline)
            RoundCallButton(Icons.Filled.Call, Color(0xFF16A34A), "Answer", onAnswer)
        }
    }
}

@Composable
private fun TalkingScreen(callerName: String, onEnd: () -> Unit) {
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            seconds++
        }
    }
    CallBackdrop {
        Spacer(Modifier.height(16.dp))
        Avatar(callerName, size = 112.dp)
        Spacer(Modifier.height(16.dp))
        Text(callerName, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.SemiBold)
        Text("%d:%02d".format(seconds / 60, seconds % 60), color = Color.White.copy(alpha = 0.8f), fontSize = 18.sp)
        Spacer(Modifier.weight(1f))
        RoundCallButton(Icons.Filled.CallEnd, Color(0xFFDC2626), "End call", onEnd)
    }
}

@Composable
private fun CallBackdrop(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF14181F))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 48.dp),
        content = content,
    )
}

@Composable
private fun RoundCallButton(icon: ImageVector, color: Color, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(72.dp).clip(CircleShape).background(color).clickable(onClick = onClick),
        ) { Icon(icon, contentDescription = label, tint = Color.White, modifier = Modifier.size(32.dp)) }
        Text(label, color = Color.White, style = MaterialTheme.typography.labelLarge)
    }
}

/** Plays the phone's ringtone on the ring stream (silent mode stays silent) and vibrates. */
private fun startRinging(context: Context): () -> Unit {
    val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    val player = runCatching {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(context, uri)
            isLooping = true
            prepare()
            start()
        }
    }.getOrNull()
    val vibrator = vibrator(context)
    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 900, 700), 0))
    return {
        runCatching { player?.stop() }
        player?.release()
        vibrator?.cancel()
    }
}

// ---- Siren ----------------------------------------------------------------------------------

/**
 * Loud alarm to draw attention. Plays the alarm sound on the alarm stream at full volume (restored
 * afterwards) until the user taps stop.
 */
@Composable
fun SirenScreen(onClose: () -> Unit) {
    val context = LocalContext.current
    KeepScreenOn()
    DisposableEffect(Unit) {
        val stop = startSiren(context)
        onDispose { stop() }
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.error)
            .clickable(onClick = onClose)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
    ) {
        Icon(Icons.Filled.Campaign, contentDescription = null, tint = Color.White, modifier = Modifier.size(96.dp))
        Text("Siren on", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
        Text("Tap anywhere to stop", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onClose,
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MaterialTheme.colorScheme.error),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text("Stop siren", fontWeight = FontWeight.Bold) }
    }
}

private fun startSiren(context: Context): () -> Unit {
    val audio = context.getSystemService(AudioManager::class.java)
    val previousVolume = audio.getStreamVolume(AudioManager.STREAM_ALARM)
    runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, audio.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0) }
    val uri = RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
    val player = runCatching {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(context, uri)
            isLooping = true
            prepare()
            start()
        }
    }.getOrNull()
    val vibrator = vibrator(context)
    vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 250), 0))
    return {
        runCatching { player?.stop() }
        player?.release()
        vibrator?.cancel()
        runCatching { audio.setStreamVolume(AudioManager.STREAM_ALARM, previousVolume, 0) }
    }
}

// ---- Shared ---------------------------------------------------------------------------------

@Composable
private fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
}

private fun vibrator(context: Context): Vibrator? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Vibrator::class.java)
    }
