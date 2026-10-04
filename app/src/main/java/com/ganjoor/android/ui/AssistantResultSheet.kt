package com.ganjoor.android.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.Assistant
import com.ganjoor.android.data.AssistantLanguage
import com.ganjoor.android.data.LocalAssistant
import com.ganjoor.android.data.Prompts

/**
 * Asking the reader's own assistant, and showing what comes back.
 *
 * The answer isn't stored: it's a reading aid, and a model's paraphrase of a thousand-year-old
 * poem has no business being cached next to the poem itself. Sharing it on is one tap, for anyone
 * who wants to keep it.
 */

/**
 * The reply, in the page. A translation belongs under the thing it translates — leaving the poem
 * to read it, whether to a sheet over the top or to another app entirely, breaks the reading.
 *
 * With a server configured the answer arrives here. Without one there is nothing to ask, so the
 * question goes to whichever assistant is installed; that hand-off is the only path that leaves
 * the app, and only because the alternative is no answer at all.
 */
@Composable
fun AssistantInline(prompt: String, text: String, label: Int, instruction: Int) {
    val assistant = LocalAssistant.current
    val context = LocalContext.current
    val ask = stringResource(instruction)
    var asked by remember(text) { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        if (!asked) {
            TextButton(onClick = {
                if (assistant.serverReady) asked = true else context.shareText("$ask\n\n$text")
            }) {
                Icon(
                    painter = painterResource(R.drawable.ic_ask),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp).padding(end = 4.dp),
                )
                Text(stringResource(label))
            }
        }
        AnimatedVisibility(visible = asked) {
            AssistantReply(prompt = prompt, text = text)
        }
    }
}

/** The question and its answer, with nothing around them. */
@Composable
fun AssistantReply(prompt: String, text: String, modifier: Modifier = Modifier) {
    val settings = LocalAssistant.current
    val context = LocalContext.current
    val language = settings.language

    val key = Assistant.cacheKey(prompt, language, text)
    val answer by produceState<Result<String>?>(Assistant.cached(key)?.let(Result.Companion::success), key) {
        // Already answered once: an item scrolling back into view must not ask again.
        if (value != null) return@produceState
        val result = Assistant.ask(
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
        result.getOrNull()?.let { Assistant.remember(key, it) }
        value = result
    }

    Column(modifier = modifier.fillMaxWidth().padding(top = 4.dp)) {
        when (val result = answer) {
            null -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp).padding(end = 8.dp),
                )
                Text(
                    text = stringResource(R.string.assistant_working),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> if (result.isSuccess) {
                // A reply in Urdu or English needs its own direction, not the reader's.
                val reply = result.getOrDefault("")
                InDirectionOf(reply) {
                    Text(
                        text = reply,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                TextButton(onClick = { context.shareText(reply) }) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp).padding(end = 4.dp),
                    )
                    Text(stringResource(R.string.share))
                }
            } else {
                Text(
                    text = result.exceptionOrNull()?.message
                        ?: stringResource(R.string.assistant_not_set_up),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** The same reply as a sheet, for the selection-menu screen, which has no page to sit in. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantResultSheet(prompt: String, text: String, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
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
            AssistantReply(prompt = prompt, text = text)
        }
    }
}
