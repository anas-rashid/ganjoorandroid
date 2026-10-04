package com.ganjoor.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.ASSISTANT_PRESETS
import com.ganjoor.android.data.Assistant
import com.ganjoor.android.data.AssistantLanguage
import com.ganjoor.android.data.AssistantMode
import com.ganjoor.android.data.LocalAssistant
import com.ganjoor.android.data.Prompts
import kotlinx.coroutines.launch

/**
 * Everything to do with the optional language model, on a page of its own because none of it is
 * needed to read a poem.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantScreen(onUp: () -> Unit, onHome: () -> Unit) {
    val settings = LocalAssistant.current
    val scope = rememberCoroutineScope()
    var baseUrl by remember { mutableStateOf(settings.baseUrl) }
    var model by remember { mutableStateOf(settings.model) }
    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var testing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.assistant)) },
                navigationIcon = {
                    IconButton(onClick = onUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = { HomeAction(onHome) },
            )
        }
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.assistant_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp),
            )

            SectionLabel(R.string.assistant_mode)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistantMode.entries.forEach { option ->
                    FilterChip(
                        selected = settings.mode == option,
                        onClick = { settings.update(mode = option) },
                        label = { Text(stringResource(modeLabel(option))) },
                    )
                }
            }
            Text(
                text = stringResource(modeNote(settings.mode)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SectionLabel(R.string.assistant_language)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistantLanguage.entries.forEach { option ->
                    FilterChip(
                        selected = settings.language == option,
                        onClick = { settings.update(language = option) },
                        label = { Text(option.englishName) },
                    )
                }
            }

            if (settings.mode == AssistantMode.Server) {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SectionLabel(R.string.assistant_presets)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ASSISTANT_PRESETS.forEach { preset ->
                        SuggestionChip(
                            onClick = {
                                baseUrl = preset.baseUrl
                                model = preset.model
                                settings.update(baseUrl = preset.baseUrl, model = preset.model)
                            },
                            label = { Text(preset.name) },
                        )
                    }
                }

                OutlinedTextField(
                    value = baseUrl,
                    onValueChange = { baseUrl = it; settings.update(baseUrl = it) },
                    label = { Text(stringResource(R.string.assistant_base_url)) },
                    supportingText = { Text(stringResource(R.string.assistant_base_url_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it; settings.update(model = it) },
                    label = { Text(stringResource(R.string.assistant_model)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it; settings.update(apiKey = it) },
                    label = { Text(stringResource(R.string.assistant_api_key)) },
                    supportingText = { Text(stringResource(R.string.assistant_api_key_note)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                )

                Button(
                    onClick = {
                        testing = true
                        result = null
                        scope.launch {
                            val reply = Assistant.ask(
                                settings = settings,
                                system = Prompts.system(settings.language),
                                user = Prompts.translate("سلام"),
                            )
                            testing = false
                            result = reply.fold(
                                onSuccess = { it.take(160) },
                                onFailure = { it.message ?: "failed" },
                            )
                        }
                    },
                    enabled = !testing && settings.serverReady,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(if (testing) R.string.assistant_testing else R.string.assistant_test))
                }
                result?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            Text(
                text = stringResource(R.string.assistant_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun modeLabel(mode: AssistantMode) = when (mode) {
    AssistantMode.Off -> R.string.assistant_off
    AssistantMode.Server -> R.string.assistant_server
    AssistantMode.ShareToApp -> R.string.assistant_share
}

private fun modeNote(mode: AssistantMode) = when (mode) {
    AssistantMode.Off -> R.string.assistant_off_note
    AssistantMode.Server -> R.string.assistant_server_note
    AssistantMode.ShareToApp -> R.string.assistant_share_note
}

@Composable
private fun SectionLabel(resId: Int) {
    Text(
        text = stringResource(resId),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp),
    )
}
