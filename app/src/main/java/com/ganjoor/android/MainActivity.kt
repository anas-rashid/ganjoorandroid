package com.ganjoor.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.ui.GanjoorApp
import com.ganjoor.android.ui.LocalSettings
import com.ganjoor.android.ui.Settings
import com.ganjoor.android.ui.theme.GanjoorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ganjoor.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val settings = remember { Settings(applicationContext) }
            CompositionLocalProvider(
                LocalSettings provides settings,
                // The content is Persian/Urdu/Arabic throughout, so the whole app is RTL.
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                GanjoorTheme(settings.value.theme) {
                    GanjoorApp()
                }
            }
        }
    }
}
