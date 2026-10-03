package com.ganjoor.android.ui

import android.app.Activity
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
import com.ganjoor.android.R

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
                    if (!selected) {
                        settings.update { it.copy(language = language) }
                        // Resources are picked in attachBaseContext, so the activity restarts.
                        (context as? Activity)?.recreate()
                    }
                },
            )
        }
    }
}
