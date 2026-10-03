package com.ganjoor.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.ui.theme.readingStyle

/**
 * Opens the reading settings sheet. The sheet itself lives at the root of [GanjoorApp]: a
 * ModalBottomSheet composes in place rather than in its own window, so hanging it off the top
 * bar's action slot would clip it to the height of the app bar.
 */
val LocalOpenReadingSettings = staticCompositionLocalOf<() -> Unit> {
    error("No reading settings host")
}

/** Top-bar button that opens the reading settings sheet. */
@Composable
fun ReadingSettingsAction() {
    val open = LocalOpenReadingSettings.current
    IconButton(onClick = open) {
        Icon(Icons.Default.Settings, stringResource(R.string.reading_settings))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(onDismiss: () -> Unit) {
    val settings = LocalSettings.current
    val prefs = settings.value

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                // Inset first, then scroll: the viewport has to stop above the nav bar,
                // otherwise the content scrolls underneath it. The sheet opens half-height,
                // so everything below the fold has to be reachable.
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Label(R.string.theme)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = prefs.theme == mode,
                        onClick = { settings.update { it.copy(theme = mode) } },
                        label = { Text(stringResource(mode.label)) },
                    )
                }
            }

            Label(R.string.font)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReadingFont.entries.forEach { font ->
                    FilterChip(
                        selected = prefs.font == font,
                        onClick = { settings.update { it.copy(font = font) } },
                        label = { Text(stringResource(font.label)) },
                    )
                }
            }

            Label(R.string.text_size)
            Slider(
                value = prefs.fontSize,
                onValueChange = { size -> settings.update { it.copy(fontSize = size) } },
                valueRange = 14f..40f,
                steps = 12,
            )
            // Live preview, so the size and font choice can be judged before closing the sheet.
            Text(
                text = stringResource(R.string.font_preview),
                style = readingStyle(prefs.font, prefs.fontSize),
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.show_summaries))
                    Text(
                        text = stringResource(R.string.summary_ai_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = prefs.showSummaries,
                    onCheckedChange = { on -> settings.update { it.copy(showSummaries = on) } },
                )
            }
        }
    }
}

@Composable
private fun Label(resId: Int) {
    Text(
        text = stringResource(resId),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 12.dp),
    )
}
