package net.anasrashid.khazeena.android

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
import androidx.core.graphics.drawable.toDrawable
import net.anasrashid.khazeena.android.data.Bookmarks
import net.anasrashid.khazeena.android.data.Dictionary
import net.anasrashid.khazeena.android.data.Ganjoor
import net.anasrashid.khazeena.android.data.LocalBookmarks
import net.anasrashid.khazeena.android.ui.GanjoorApp
import net.anasrashid.khazeena.android.data.AssistantSettings
import net.anasrashid.khazeena.android.data.LocalAssistant
import net.anasrashid.khazeena.android.data.LocalPinnedPoets
import net.anasrashid.khazeena.android.data.PinnedPoets
import net.anasrashid.khazeena.android.ui.LocalSettings
import net.anasrashid.khazeena.android.ui.Settings
import net.anasrashid.khazeena.android.ui.theme.GanjoorTheme
import net.anasrashid.khazeena.android.ui.theme.windowBackground
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
        Dictionary.init(applicationContext)

        // Before the first frame: otherwise the window keeps the platform's white through
        // startup and every screen transition, whatever theme is chosen.
        val settings = Settings(applicationContext)
        val systemInDark = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        window.setBackgroundDrawable(
            windowBackground(settings.value.theme, systemInDark, settings.value.oled).toDrawable()
        )

        enableEdgeToEdge()
        setContent {
            val settings = remember { settings }
            val bookmarks = remember { Bookmarks(applicationContext) }
            val assistant = remember { AssistantSettings(applicationContext) }
            val pinned = remember { PinnedPoets(applicationContext) }
            // The client reads this flag on every request, so keep it in step with the setting.
            Ganjoor.offline = settings.value.offline
            CompositionLocalProvider(
                LocalSettings provides settings,
                LocalBookmarks provides bookmarks,
                LocalAssistant provides assistant,
                LocalPinnedPoets provides pinned,
                // The poetry is Persian, Urdu and Arabic throughout, so the whole app reads and
                // navigates right-to-left whichever UI language is selected.
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                GanjoorTheme(settings.value.theme, settings.value.language, settings.value.oled) {
                    GanjoorApp()
                }
            }
        }
    }
}
