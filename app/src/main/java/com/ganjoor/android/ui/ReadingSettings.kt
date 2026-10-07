package com.ganjoor.android.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.ui.theme.readingStyle

/**
 * Opens the reading settings sheet. The sheet itself lives at the root of [GanjoorApp]: a
 * ModalBottomSheet composes in place rather than in its own window, so hanging it off the top
 * bar's action slot would clip it to the height of the app bar.
 */
val LocalOpenReadingSettings = staticCompositionLocalOf<() -> Unit> {
    error("No reading settings host")
}

/** Opens the About and licences screen; the sheet is the only place that needs it. */
val LocalOpenAbout = staticCompositionLocalOf<() -> Unit> { error("No about host") }
val LocalOpenAssistant = staticCompositionLocalOf<() -> Unit> { error("No assistant host") }

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
    ModalBottomSheet(onDismissRequest = onDismiss) {
        ReadingSettingsContent()
    }
}

/**
 * The same settings as [ReadingSettingsSheet], as a panel on the left of a large screen — the
 * place the dictionary opens too. The page stays in view beside it, so a change of theme, font
 * or size shows on the poem itself as it is made. The cross, or Back, closes it.
 */
@Composable
fun ReadingSettingsPanel(onDismiss: () -> Unit) {
    BackHandler(onBack = onDismiss)
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.width(SidePanelWidth).fillMaxHeight(),
    ) {
        Column(Modifier.statusBarsPadding()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .padding(start = 20.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.reading_settings),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, stringResource(R.string.close))
                }
            }
            HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
            ReadingSettingsContent()
        }
    }
}

@Composable
private fun ReadingSettingsContent() {
    val settings = LocalSettings.current
    val prefs = settings.value
    val context = LocalContext.current

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
            Chips(ThemeMode.entries, prefs.theme, { stringResource(it.label) }) { mode ->
                settings.update { it.copy(theme = mode) }
            }

            // Only means anything on a dark theme, so it sits with them and says so.
            Toggle(
                title = R.string.oled,
                note = R.string.oled_note,
                checked = prefs.oled,
            ) { on ->
                settings.update { it.copy(oled = on) }
            }

            Label(R.string.font)
            Chips(ReadingFont.entries, prefs.font, { stringResource(it.label) }) { font ->
                settings.update { it.copy(font = font) }
            }

            Label(R.string.weight)
            Chips(ReadingWeight.entries, prefs.fontWeight, { stringResource(it.label) }) { weight ->
                settings.update { it.copy(fontWeight = weight) }
            }

            Label(R.string.text_size)
            Slider(
                value = prefs.fontSize,
                onValueChange = { size -> settings.update { it.copy(fontSize = size) } },
                valueRange = 14f..40f,
                steps = 12,
            )
            // Live preview, so size, weight and font can be judged before closing the sheet.
            Text(
                text = stringResource(R.string.font_preview),
                style = readingStyle(prefs.font, prefs.fontSize, prefs.fontWeight.weight),
                modifier = Modifier.fillMaxWidth(),
            )

            Label(R.string.language)
            Chips(Language.entries, prefs.language, { it.label }) { language ->
                if (language != prefs.language) {
                    settings.update { it.copy(language = language) }
                    // Resources are picked in attachBaseContext, so the activity has to restart.
                    (context as? Activity)?.recreate()
                }
            }

            Toggle(
                title = R.string.offline_mode,
                note = R.string.offline_mode_note,
                checked = prefs.offline,
            ) { on ->
                settings.update { it.copy(offline = on) }
                Ganjoor.offline = on
            }

            Toggle(
                title = R.string.show_summaries,
                note = R.string.summary_ai_note,
                checked = prefs.showSummaries,
            ) { on ->
                settings.update { it.copy(showSummaries = on) }
            }

            val openAssistant = LocalOpenAssistant.current
            TextButton(onClick = openAssistant, modifier = Modifier.padding(top = 16.dp)) {
                Text(stringResource(R.string.assistant_title))
            }

            val openAbout = LocalOpenAbout.current
            TextButton(onClick = openAbout) {
                Text(stringResource(R.string.about))
            }
        }
}

@Composable
private fun <T> Chips(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { onSelect(option) },
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
private fun Toggle(title: Int, note: Int, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(title))
            Text(
                text = stringResource(note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onChange)
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
