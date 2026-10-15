package net.anasrashid.khazeena.android.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import net.anasrashid.khazeena.android.R

/** Top-bar language picker. The same choice also lives in the reading settings sheet. */
@Composable
fun LanguageAction() {
    val settings = LocalSettings.current
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }

    IconButton(onClick = { open = true }) {
        Icon(painterResource(R.drawable.ic_language), stringResource(R.string.language))
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        Language.entries.forEach { language ->
            val selected = settings.value.language == language
            DropdownMenuItem(
                text = {
                    Text(
                        text = language.label,
                        color = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
                    )
                },
                onClick = {
                    open = false
                    if (!selected) switchLanguage(settings, context, language)
                },
            )
        }
    }
}

/**
 * Resources are picked in attachBaseContext, so the activity restarts. From inside a bottom sheet
 * or menu the context is that window's wrapper, not the activity, so unwrap until one turns up;
 * a plain cast found nothing there and the new language waited for the next launch.
 */
fun switchLanguage(settings: Settings, context: Context, language: Language) {
    settings.update { it.copy(language = language) }
    generateSequence(context) { (it as? ContextWrapper)?.baseContext }
        .firstNotNullOfOrNull { it as? Activity }
        ?.recreate()
}
