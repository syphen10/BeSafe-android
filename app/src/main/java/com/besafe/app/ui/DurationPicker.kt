package com.besafe.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.besafe.app.safety.CheckInService

/** Preset check-in lengths, in minutes. */
val presetDurations = listOf(15, 30, 60, 120, 240, 360)

/** "45 min", "2 h", "7 h 30 min". */
fun formatMinutes(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "$m min"
        m == 0 -> "$h h"
        else -> "$h h $m min"
    }
}

/** Preset chips plus a "Custom" chip that opens an hours/minutes picker (up to 24 h). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DurationPicker(selectedMinutes: Int, onSelect: (Int) -> Unit) {
    var showCustom by rememberSaveable { mutableStateOf(false) }
    val isCustom = selectedMinutes !in presetDurations
    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = MaterialTheme.colorScheme.primary,
        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
    )

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        presetDurations.forEach { option ->
            FilterChip(
                selected = selectedMinutes == option,
                onClick = { onSelect(option) },
                label = { Text(formatMinutes(option)) },
                colors = chipColors,
            )
        }
        FilterChip(
            selected = isCustom,
            onClick = { showCustom = true },
            label = { Text(if (isCustom) "Custom · ${formatMinutes(selectedMinutes)}" else "Custom") },
            colors = chipColors,
        )
    }

    if (showCustom) {
        CustomDurationDialog(
            initialMinutes = selectedMinutes,
            onDismiss = { showCustom = false },
            onConfirm = {
                onSelect(it)
                showCustom = false
            },
        )
    }
}

@Composable
private fun CustomDurationDialog(initialMinutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var hours by rememberSaveable { mutableIntStateOf(initialMinutes / 60) }
    var minutes by rememberSaveable { mutableIntStateOf((initialMinutes % 60) / 5 * 5) }
    val maxHours = CheckInService.MAX_MINUTES / 60
    val total = hours * 60 + minutes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Custom check-in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "How long until you expect to be safe? Up to $maxHours hours.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Stepper(
                    label = "Hours",
                    value = hours,
                    onDecrease = { if (hours > 0) hours-- },
                    onIncrease = {
                        if (hours < maxHours) hours++
                        if (hours == maxHours) minutes = 0
                    },
                )
                Stepper(
                    label = "Minutes",
                    value = minutes,
                    onDecrease = { minutes = (minutes - 5).coerceAtLeast(0) },
                    onIncrease = { if (hours < maxHours) minutes = (minutes + 5).coerceAtMost(55) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(total) }, enabled = total >= 5) {
                Text(if (total >= 5) "Use ${formatMinutes(total)}" else "Use")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun Stepper(label: String, value: Int, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        FilledTonalIconButton(onClick = onDecrease) { Icon(Icons.Filled.Remove, contentDescription = "Less $label") }
        Text(
            value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(56.dp),
        )
        FilledTonalIconButton(onClick = onIncrease) { Icon(Icons.Filled.Add, contentDescription = "More $label") }
    }
}
