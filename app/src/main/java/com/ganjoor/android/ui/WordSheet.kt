package com.ganjoor.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.Definition
import com.ganjoor.android.data.Bookmark
import com.ganjoor.android.data.Dictionary
import com.ganjoor.android.data.LocalAssistant
import com.ganjoor.android.data.Pronunciation
import com.ganjoor.android.ui.theme.readingStyle

/** English prose inside an otherwise right-to-left sheet. */
@Composable
private fun LeftToRight(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr, content = content)
}

/** Lays a definition out the way its own script reads. */
@Composable
internal fun InDirectionOf(text: String, content: @Composable () -> Unit) {
    val arabicScript = text.count { it in '\u0600'..'\u06FF' }
    val latin = text.count { it in 'A'..'Z' || it in 'a'..'z' }
    CompositionLocalProvider(
        LocalLayoutDirection provides
            if (arabicScript > latin) LayoutDirection.Rtl else LayoutDirection.Ltr,
        content = content,
    )
}

/** Which dictionary answered, and in which language pair. */
private fun sourceLabel(source: String) = when (source) {
    "wiktionary-fa" -> R.string.source_wiktionary
    "wiktionary-ur" -> R.string.source_wiktionary_ur
    "urwiktionary" -> R.string.source_urwiktionary
    "wiktionary-ar" -> R.string.source_wiktionary_ar
    else -> R.string.source_daneshjoo
}

/**
 * What the dictionary knows about a tapped word, as a sheet over the poem.
 *
 * When the word came from a couplet, that couplet's own actions sit at the foot. Tapping a word
 * is the one gesture every reader finds; saving, copying or sharing the line used to be behind a
 * tap that landed between words, which on a full line of poetry almost never happens.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WordSheet(word: String, onDismiss: () -> Unit, passage: Bookmark? = null) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        // Inside the lookup's own scroll, not after it: the lookup scrolls, so anything placed
        // below it is pushed past the bottom of the sheet with no way to reach it.
        WordLookup(word) {
            if (passage == null) return@WordLookup
            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
            Text(
                text = stringResource(R.string.this_couplet),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            PassageActions(passage)
            if (LocalAssistant.current.serverReady) {
                AssistantInline(
                    prompt = "explain",
                    text = passage.excerpt.orEmpty(),
                    label = R.string.assistant_explain,
                    instruction = R.string.assistant_ask_prompt,
                )
            }
        }
    }
}

/**
 * The lookup itself. Split out from the sheet because the selection-menu activity shows the same
 * thing as a plain screen: it launches in its own task, so there is nothing behind it for a sheet
 * to sit over, and a scrim with no content under it is just a grey window.
 */
@Composable
fun WordLookup(word: String, footer: @Composable ColumnScope.() -> Unit = {}) {
    val prefs = LocalSettings.current.value
    // A suggestion replaces what is being looked up, so the sheet can be followed like a trail.
    var current by remember(word) { mutableStateOf(word) }
    val definitions by produceState<List<Definition>?>(null, current) {
        value = Dictionary.lookup(current)
    }
    val sounds by produceState(emptyList<Pronunciation>(), current) {
        value = Dictionary.pronunciations(current)
    }
    val suggestions by produceState(emptyList<String>(), current, definitions) {
        value = if (definitions?.isEmpty() == true) Dictionary.suggest(current) else emptyList()
    }

    Column(
            modifier = Modifier
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // The headword in the reading font, at reading size: it is a line of poetry, after all.
            Text(
                text = current,
                style = readingStyle(prefs.font, prefs.fontSize, prefs.fontWeight.weight),
                modifier = Modifier.fillMaxWidth(),
            )
            if (sounds.isNotEmpty()) {
                // One direction for the whole block, and one pronunciation per line. Flowing
                // them side by side put Latin IPA and Urdu spelling in the same right-to-left
                // run, which reordered the chips and left each label under someone else's value.
                LeftToRight {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        sounds.forEach { sound ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = sound.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                if (sound.label.isNotBlank()) {
                                    Text(
                                        text = sound.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            HorizontalDivider()

            when {
                definitions == null -> CircularProgressIndicator(Modifier.padding(vertical = 16.dp))

                definitions!!.isEmpty() -> Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LeftToRight {
                        Text(
                            text = stringResource(R.string.no_definition),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    if (suggestions.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.did_you_mean),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            suggestions.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = { current = suggestion },
                                    label = { Text(suggestion) },
                                )
                            }
                        }
                    }
                }

                else -> definitions!!.forEach { definition ->
                    Column(Modifier.fillMaxWidth()) {
                        // The headword actually matched, which may be the lemma rather than the
                        // word as it appears in the line. Persian, so it stays right-to-left.
                        if (definition.word != current) {
                            Text(
                                text = definition.word,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        // Most definitions are English and right-aligning them reads badly;
                        // the Urdu ones are right-to-left like the rest of the app.
                        InDirectionOf(definition.gloss) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(definition.gloss, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = stringResource(sourceLabel(definition.source)),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        
        footer()
    }

}
