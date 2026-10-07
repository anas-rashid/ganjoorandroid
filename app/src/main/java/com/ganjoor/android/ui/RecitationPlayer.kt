package com.ganjoor.android.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.CompositionLocalProvider
import com.ganjoor.android.R
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.data.Recitation
import kotlinx.coroutines.delay

/** mm:ss, the only shape a reading's length ever needs. */
private fun clock(millis: Int): String {
    val total = (millis / 1000).coerceAtLeast(0)
    return "%d:%02d".format(total / 60, total % 60)
}

/**
 * Plays a reading of the poem, streamed from Ganjoor.
 *
 * Placed above the scrolling text rather than inside it, and deliberately so: as an item of the
 * LazyColumn it was disposed the moment it scrolled off, which released the MediaPlayer and cut
 * the reading off mid-line. Pinned here it stays in composition for as long as the poem is open,
 * and stays in reach — which is what a player is for while you are reading further down.
 *
 * ponytail: still the platform's MediaPlayer rather than ExoPlayer. One URL, and seeking within
 * it, is not a media library's worth of work. Nothing is cached, so this is the one part of the
 * app that needs a connection; it simply doesn't appear when there is no reading or no network.
 */
@Composable
fun RecitationPlayer(poemId: Int, modifier: Modifier = Modifier) {
    val recitations by produceState(emptyList<Recitation>(), poemId) {
        value = Ganjoor.recitations(poemId)
    }
    if (recitations.isEmpty()) return

    var chosen by remember(poemId) { mutableIntStateOf(0) }
    var playing by remember(poemId) { mutableStateOf(false) }
    var loading by remember(poemId) { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var duration by remember(poemId) { mutableIntStateOf(0) }
    var position by remember(poemId) { mutableIntStateOf(0) }
    // While a finger is on the slider the poll must not fight it for the handle.
    var scrubbing by remember(poemId) { mutableStateOf<Float?>(null) }

    val player = remember {
        MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
        }
    }
    // A reading left playing when the poem closes would keep the whole thing in memory.
    DisposableEffect(player) { onDispose { runCatching { player.release() } } }

    val recitation = recitations.getOrNull(chosen) ?: return
    // Changing reader stops whatever was playing, so the two never overlap.
    DisposableEffect(recitation.mp3Url) {
        runCatching { player.reset() }
        playing = false
        loading = false
        position = 0
        duration = 0
        onDispose { }
    }

    // The handle follows the audio only while it is actually moving.
    LaunchedEffect(playing) {
        while (playing) {
            runCatching { position = player.currentPosition }
            delay(250)
        }
    }

    fun toggle() {
        if (playing) {
            runCatching { player.pause() }
            playing = false
            return
        }
        if (duration > 0) {
            runCatching { player.start() }.onSuccess { playing = true }
            return
        }
        loading = true
        runCatching {
            player.reset()
            player.setDataSource(recitation.mp3Url)
            player.setOnPreparedListener {
                duration = it.duration
                it.start()
                playing = true
                loading = false
            }
            player.setOnCompletionListener { playing = false; position = duration }
            player.setOnErrorListener { _, _, _ -> playing = false; loading = false; true }
            player.prepareAsync()
        }.onFailure { loading = false }
    }

    Column(modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            IconButton(onClick = ::toggle) {
                when {
                    loading -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    // Core Material icons ship no pause glyph, and the extended set is 4 MB for one.
                    playing -> Icon(painterResource(R.drawable.ic_pause), stringResource(R.string.pause))
                    else -> Icon(Icons.Default.PlayArrow, stringResource(R.string.play_recitation))
                }
            }
            TextButton(
                onClick = { if (recitations.size > 1) picking = true },
                modifier = Modifier.weight(1f, fill = false),
            ) {
                Text(
                    text = recitation.audioArtist.ifBlank { stringResource(R.string.play_recitation) },
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (recitations.size > 1) {
                Text(
                    text = "${chosen + 1}/${recitations.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            DropdownMenu(expanded = picking, onDismissRequest = { picking = false }) {
                recitations.forEachIndexed { index, item ->
                    DropdownMenuItem(
                        text = { Text(item.audioArtist.ifBlank { item.audioTitle }) },
                        onClick = { chosen = index; picking = false },
                    )
                }
            }
        }

        // Only once the length is known: a bar that cannot be dragged anywhere is furniture.
        if (duration > 0) {
            // Time runs left to right whatever the script, so the bar and its two times are laid
            // out that way inside an otherwise right-to-left page.
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = clock(scrubbing?.toInt() ?: position),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Slider(
                        value = scrubbing ?: position.toFloat().coerceIn(0f, duration.toFloat()),
                        onValueChange = { scrubbing = it },
                        onValueChangeFinished = {
                            scrubbing?.let { target ->
                                runCatching { player.seekTo(target.toInt()) }
                                position = target.toInt()
                            }
                            scrubbing = null
                        },
                        valueRange = 0f..duration.toFloat(),
                        modifier = Modifier
                            .weight(1f)
                            .semantics { contentDescription = "seek" },
                    )
                    Text(
                        text = clock(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
