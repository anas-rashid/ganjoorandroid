package com.ganjoor.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ganjoor.android.R
import com.ganjoor.android.data.NotDownloaded

/**
 * Fetches [block] whenever [key] changes and renders loading / error / content.
 *
 * ponytail: no ViewModel, so going back re-fetches — which the disk cache makes nearly free.
 * Promote to a ViewModel when a screen gains state worth surviving rotation.
 */
@Composable
fun <T> Load(key: Any?, block: suspend () -> T, content: @Composable (T) -> Unit) {
    var attempt by remember(key) { mutableIntStateOf(0) }
    val result by produceState<Result<T>?>(null, key, attempt) { value = runCatching { block() } }

    result.let { outcome ->
        when {
            outcome == null -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }

            outcome.isFailure -> Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(
                        // Offline mode failing on a page nobody downloaded isn't a network error,
                        // and telling someone to check their connection would be a dead end.
                        if (outcome.exceptionOrNull() is NotDownloaded) R.string.load_failed_offline
                        else R.string.load_failed
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                )
                TextButton(onClick = { attempt++ }) { Text(stringResource(R.string.retry)) }
            }

            else -> content(outcome.getOrThrow())
        }
    }
}
