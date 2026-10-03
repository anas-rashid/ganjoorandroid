package com.ganjoor.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ganjoor.android.R

/** Escape hatch back to the poet list, on every screen that isn't it. */
@Composable
fun HomeAction(onHome: () -> Unit) {
    IconButton(onClick = onHome) {
        Icon(Icons.Default.Home, stringResource(R.string.home))
    }
}
