package com.ganjoor.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.Assistant
import com.ganjoor.android.data.AssistantLanguage
import com.ganjoor.android.data.LocalAssistant
import com.ganjoor.android.data.Prompts

/**
 * Asks the reader's own assistant and shows what comes back.
 *
 * The answer isn't stored: it's a reading aid, and a model's paraphrase of a thousand-year-old
 * poem has no business being cached next to the poem itself. Sharing it on is one tap, for anyone
 * who wants to keep it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantResultSheet(prompt: String, text: String, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        AssistantAnswer(text = text, prompt = prompt)
    }
}

/**
 * A button that asks the reader's own assistant, or hands the question to another app when no
 * server is configured. Both paths are one tap and neither requires any setting up, which is the
 * point: the feature is optional, so it cannot assume anyone switched it on.
 */
@Composable
fun AssistantAction(prompt: String, text: String, label: Int, instruction: Int) {
    val assistant = LocalAssistant.current
    val context = LocalContext.current
    val ask = stringResource(instruction)
    var open by remember { mutableStateOf(false) }

    TextButton(onClick = {
        if (assistant.serverReady) open = true else context.shareText("$ask\n\n$text")
    }) {
        Text(stringResource(label))
    }
    if (open) AssistantResultSheet(prompt = prompt, text = text, onDismiss = { open = false })
}

/** The same answer as a plain screen, for the selection-menu activity. */
@Composable
fun AssistantAnswer(text: String, prompt: String = "translate") {
    val settings = LocalAssistant.current
    val context = LocalContext.current
    val language = settings.language

    val answer by produceState<Result<String>?>(null, prompt, text, language) {
        value = Assistant.ask(
            settings = settings,
            system = Prompts.system(language),
            user = when (prompt) {
                "translate" -> Prompts.translate(text)
                "summarise" -> Prompts.summarise(text)
                "explain" -> Prompts.explain(text)
                "summary" -> Prompts.translateSummary(text)
                else -> text
            },
        )
    }

    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
    ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            )
            val result = answer
            when {
                result == null -> Column {
                    CircularProgressIndicator(modifier = Modifier.padding(bottom = 8.dp))
                    Text(stringResource(R.string.assistant_working))
                }

                result.isSuccess -> {
                    // A reply in Urdu or English needs its own direction, not the reader's.
                    val reply = result.getOrDefault("")
                    InDirectionOf(reply) {
                        Text(reply, style = MaterialTheme.typography.bodyLarge)
                    }
                    TextButton(onClick = { context.shareText(reply) }) {
                        Text(stringResource(R.string.share))
                    }
                }

                else -> Text(
                    text = result.exceptionOrNull()?.message
                        ?: stringResource(R.string.assistant_not_set_up),
                    color = MaterialTheme.colorScheme.error,
                )
            }
    }
}
