package com.ganjoor.android.ui

import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.data.Recitation

/**
 * Plays a reading of the poem, streamed from Ganjoor.
 *
 * ponytail: the platform's MediaPlayer rather than ExoPlayer — one URL, play and pause, no
 * playlist or seeking to justify a media library. Nothing is cached, so this is the one part of
 * the app that needs a connection; it simply doesn't appear when there is no reading or no
 * network.
 */
@Composable
fun RecitationPlayer(poemId: Int) {
    val recitations by produceState(emptyList<Recitation>(), poemId) {
        value = Ganjoor.recitations(poemId)
    }
    if (recitations.isEmpty()) return

    var chosen by remember(poemId) { mutableIntStateOf(0) }
    var playing by remember(poemId) { mutableStateOf(false) }
    var loading by remember(poemId) { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }

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
    // A reading left playing when the screen goes would keep the whole poem in memory.
    DisposableEffect(player) { onDispose { runCatching { player.release() } } }

    val recitation = recitations.getOrNull(chosen) ?: return
    // Changing reader stops whatever was playing, so the two never overlap.
    DisposableEffect(recitation.mp3Url) {
        runCatching { player.reset() }
        playing = false
        loading = false
        onDispose { }
    }

    fun toggle() {
        if (playing) {
            runCatching { player.pause() }
            playing = false
            return
        }
        if (player.currentPosition > 0) {
            runCatching { player.start() }.onSuccess { playing = true }
            return
        }
        loading = true
        runCatching {
            player.reset()
            player.setDataSource(recitation.mp3Url)
            player.setOnPreparedListener { it.start(); playing = true; loading = false }
            player.setOnCompletionListener { playing = false }
            player.setOnErrorListener { _, _, _ -> playing = false; loading = false; true }
            player.prepareAsync()
        }.onFailure { loading = false }
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
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
}
