package com.ganjoor.android.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.AssistantLanguage
import com.ganjoor.android.data.DescriptionTranslation
import com.ganjoor.android.data.DescriptionTranslator
import com.ganjoor.android.data.LocalAssistant

/**
 * One of Ganjoor's descriptions — its AI commentary on a couplet or a poem, written in Persian.
 *
 * A reader whose app is in Urdu or English gets it in their language, in the page, with a line
 * saying who translated it: a translation of a machine's commentary must never pass for Ganjoor's
 * own words, still less the poet's. A Persian reader gets Ganjoor's text as it is. Whenever there
 * is nothing to translate with, the Persian shows and [untranslated] is offered under it.
 */
@Composable
fun GanjoorDescription(
    summary: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    untranslated: @Composable () -> Unit = {},
) {
    val assistant = LocalAssistant.current
    val prefs = LocalSettings.current.value
    val target = when (prefs.language) {
        Language.Ur -> AssistantLanguage.Urdu
        Language.En -> AssistantLanguage.English
        Language.Fa -> null
    }
    val by = assistant.descriptionTranslator
    val original = @Composable {
        Text(
            text = summary,
            style = style,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    if (target == null || !DescriptionTranslation.ready(by, assistant, prefs.offline)) {
        Column(modifier) {
            original()
            untranslated()
        }
        return
    }

    val key = DescriptionTranslation.key(by, target, summary)
    val result by produceState(
        DescriptionTranslation.cached(key)?.let(Result.Companion::success),
        key,
    ) {
        if (value == null) {
            value = DescriptionTranslation.translate(by, assistant, target, summary, prefs.offline)
        }
    }
    var showOriginal by remember(summary) { mutableStateOf(false) }

    Column(modifier.fillMaxWidth()) {
        val translation = result?.getOrNull()
        when {
            // Ganjoor's text while the translation is on its way, so nothing jumps into an empty gap.
            result == null -> {
                original()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(14.dp).padding(end = 6.dp),
                    )
                    Note(stringResource(R.string.description_translating))
                }
            }

            translation == null -> {
                original()
                Note(
                    text = result?.exceptionOrNull()?.message.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                )
            }

            else -> {
                if (showOriginal) {
                    original()
                } else {
                    InDirectionOf(translation) {
                        Text(
                            text = translation,
                            style = style,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Note(
                        text = if (by == DescriptionTranslator.Google) {
                            stringResource(R.string.description_by_google)
                        } else {
                            stringResource(R.string.description_by_ai, assistant.model)
                        },
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { showOriginal = !showOriginal }) {
                        Text(
                            text = stringResource(
                                if (showOriginal) R.string.description_show_translation
                                else R.string.description_show_original
                            ),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Note(
    text: String,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant,
    modifier: Modifier = Modifier,
) {
    Text(text = text, style = MaterialTheme.typography.labelSmall, color = color, modifier = modifier)
}
