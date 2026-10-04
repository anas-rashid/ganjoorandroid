package com.ganjoor.android

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toDrawable
import com.ganjoor.android.data.AssistantSettings
import com.ganjoor.android.data.Dictionary
import com.ganjoor.android.data.LocalAssistant
import com.ganjoor.android.ui.AssistantResultSheet
import com.ganjoor.android.ui.LocalSettings
import com.ganjoor.android.ui.Settings
import com.ganjoor.android.ui.WordLookup
import com.ganjoor.android.ui.theme.readingStyle
import com.ganjoor.android.ui.shareText
import com.ganjoor.android.ui.theme.GanjoorTheme
import com.ganjoor.android.ui.theme.windowBackground
import java.util.Locale

/**
 * What the app offers on selected text, anywhere on the phone.
 *
 * One activity behind three aliases — LookUpText, AskAssistantText and ShareText — so the system
 * menu shows verbs rather than one Ganjoor entry that then asks what you meant. Which alias was
 * tapped arrives as the intent's component name.
 *
 * Compose stopped routing SelectionContainer through LocalTextToolbar, so an entry can't be added
 * to that menu from inside the reader. ACTION_PROCESS_TEXT goes round that: the system builds part
 * of the selection menu from activities that handle this intent. Registering one puts Ganjoor
 * there — and not only in this app. Select a Persian word in a browser or a messaging app and the
 * dictionary is one tap away.
 *
 * It is an ordinary opaque screen, not a transparent one with a sheet. The intent starts it in its
 * own task, so the app the text was selected in is not behind this window; a translucent activity
 * showed a grey void with the dim scrim over nothing.
 */
class ProcessTextActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale.forLanguageTag(Settings.language(newBase).tag))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Dictionary.init(applicationContext)

        val selected = (
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)
                ?: intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT_READONLY)
            )?.toString()?.trim().orEmpty()

        if (selected.isEmpty()) {
            finish()
            return
        }

        val sharing = intent.component?.className?.endsWith("ShareText") == true

        val settings = Settings(applicationContext)
        val systemInDark = resources.configuration.uiMode and
            Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        // Painted before the first frame, so this doesn't flash white on the way in.
        window.setBackgroundDrawable(
            windowBackground(settings.value.theme, systemInDark, settings.value.oled).toDrawable()
        )

        setContent {
            val assistant = remember { AssistantSettings(applicationContext) }
            CompositionLocalProvider(
                LocalSettings provides settings,
                LocalAssistant provides assistant,
                LocalLayoutDirection provides LayoutDirection.Rtl,
            ) {
                GanjoorTheme(settings.value.theme, settings.value.language, settings.value.oled) {
                    SelectionScreen(selected, sharing, onClose = ::finish)
                }
            }
        }
    }
}

/**
 * Share goes straight to the system chooser and finishes, so this screen is only ever seen for a
 * lookup. Asking an assistant is a button here rather than an entry of its own in the selection
 * menu: with no server configured it opens the same chooser Share does, and two menu entries for
 * one chooser is a menu that makes the reader choose twice.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionScreen(selected: String, sharing: Boolean, onClose: () -> Unit) {
    val context = LocalContext.current
    val assistant = LocalAssistant.current
    val prompt = stringResource(R.string.assistant_ask_prompt)
    var asking by remember { mutableStateOf(false) }

    LaunchedEffect(sharing) {
        if (sharing) {
            context.shareText(selected)
            onClose()
        }
    }
    if (sharing) return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selected, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, stringResource(R.string.back))
                    }
                },
                actions = {
                    // The dictionary knows single words; a whole line is better asked about. Your
                    // own server answers here; without one the question goes to whichever
                    // assistant is installed, which is what keeps this F-Droid-clean — no vendor
                    // SDK, no key, nothing but an intent you confirm.
                    IconButton(onClick = {
                        if (assistant.serverReady) asking = true
                        else context.shareText("$prompt\n\n$selected")
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_ask),
                            contentDescription = stringResource(R.string.assistant_ask),
                        )
                    }
                    IconButton(onClick = { context.shareText(selected) }) {
                        Icon(Icons.Default.Share, stringResource(R.string.share))
                    }
                },
            )
        },
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            WordLookup(selected)
        }
    }

    if (asking) {
        AssistantResultSheet(
            prompt = "explain",
            text = selected,
            onDismiss = { asking = false },
        )
    }
}
