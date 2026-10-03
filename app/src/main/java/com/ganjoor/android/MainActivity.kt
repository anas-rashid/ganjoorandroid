package com.ganjoor.android

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.ganjoor.android.data.Bookmarks
import com.ganjoor.android.data.Ganjoor
import com.ganjoor.android.data.LocalBookmarks
import com.ganjoor.android.ui.GanjoorApp
import com.ganjoor.android.ui.LocalSettings
import com.ganjoor.android.ui.Settings
import com.ganjoor.android.ui.theme.GanjoorTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    /**
     * The UI language is the app's own setting, not the phone's — Farsi unless chosen otherwise.
     * Applied here because resources are resolved before anything else runs; changing the
     * setting calls [recreate], which comes back through this method.
     */
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale.forLanguageTag(Settings.language(newBase).tag))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ganjoor.init(applicationContext)
        enableEdgeToEdge()
        setContent {
            val settings = remember { Settings(applicationContext) }
            val bookmarks = remember { Bookmarks(applicationContext) }
            // The client reads this flag on every request, so keep it in step with the setting.
            Ganjoor.offline = settings.value.offline
            CompositionLocalProvider(
                LocalSettings provides settings,
                LocalBookmarks provides bookmarks,
                // The poetry is Persian, Urdu and Arabic throughout, so the whole app reads and
                // navigates right-to-left whichever UI language is selected.
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                GanjoorTheme(settings.value.theme, settings.value.language) {
                    GanjoorApp()
                }
            }
        }
    }
}
