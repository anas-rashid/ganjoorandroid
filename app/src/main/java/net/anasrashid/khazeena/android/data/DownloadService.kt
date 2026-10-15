package net.anasrashid.khazeena.android.data

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.anasrashid.khazeena.android.MainActivity
import net.anasrashid.khazeena.android.R
import net.anasrashid.khazeena.android.ui.Settings
import java.util.Locale

/**
 * Keeps [Downloads] going once the reader leaves the app. It does no fetching itself: a
 * foreground service is what stops Android from killing the process mid-poet, and its
 * notification shows who is downloading, how far along, and how many are waiting.
 */
class DownloadService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var watcher: Job? = null

    // Same language as the app, not the phone's: the notification is part of the app.
    override fun attachBaseContext(newBase: Context) {
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale.forLanguageTag(Settings.language(newBase).tag))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onBind(intent: Intent?) = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) Downloads.cancelAll()

        NotificationManagerCompat.from(this).createNotificationChannel(
            NotificationChannelCompat.Builder(CHANNEL, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(getString(R.string.downloads))
                .build()
        )
        // Android requires this within seconds of the start, even if the queue is already empty.
        ServiceCompat.startForeground(
            this, NOTIFICATION, notification(null), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
        )

        if (watcher?.isActive != true) watcher = scope.launch {
            val names = runCatching { Ganjoor.manifest().poets.associate { it.slug to it.nickname } }
                .getOrDefault(emptyMap())
            // ponytail: polled once a second rather than observed. Snapshot writes from the
            // download threads only reach observers while some Compose UI is running, and here
            // the whole point is that none is.
            while (Downloads.running.isNotEmpty()) {
                NotificationManagerCompat.from(this@DownloadService).run {
                    if (areNotificationsEnabled()) notify(NOTIFICATION, notification(names))
                }
                delay(1000)
            }
            ServiceCompat.stopForeground(this@DownloadService, ServiceCompat.STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun notification(names: Map<String, String>?): android.app.Notification {
        val active = Downloads.active
        val progress = active?.let { Downloads.running[it] }
        val waiting = Downloads.running.size - (if (active != null) 1 else 0)
        val open = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val cancel = PendingIntent.getService(
            this, 0,
            Intent(this, DownloadService::class.java).setAction(ACTION_CANCEL),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_download)
            .setContentTitle(active?.let { names?.get(it) ?: it } ?: getString(R.string.downloads))
            .setContentText(
                if (progress != null && progress.total > 0)
                    getString(R.string.downloading, progress.done, progress.total)
                else getString(R.string.download_preparing)
            )
            .setSubText(if (waiting > 0) getString(R.string.downloads_waiting, waiting) else null)
            .setProgress(progress?.total ?: 0, progress?.done ?: 0, (progress?.total ?: 0) == 0)
            .setContentIntent(open)
            .addAction(0, getString(R.string.cancel_all), cancel)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .build()
    }

    companion object {
        private const val CHANNEL = "downloads"
        private const val NOTIFICATION = 1
        private const val ACTION_CANCEL = "cancel"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context, Intent(context, DownloadService::class.java),
            )
        }
    }
}
