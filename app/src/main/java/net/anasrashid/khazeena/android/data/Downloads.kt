package net.anasrashid.khazeena.android.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Tracks poet downloads so the UI can show progress.
 *
 * The work is plain coroutines; [DownloadService] only keeps the process alive, in the foreground
 * with a notification, while anything is queued, so leaving the app does not stop a download.
 * ponytail: no WorkManager, so a download the system kills anyway is not resumed on its own.
 * Starting it again is cheap and safe, because [Ganjoor.downloadPoet] skips anything on disk.
 */
object Downloads {
    /** [total] is 0 until the poet's tree has been walked, i.e. while still queued. */
    data class Progress(val done: Int, val total: Int)

    /** Slug -> progress, for downloads running right now. */
    val running = mutableStateMapOf<String, Progress>()

    /** The poet being fetched right now; everyone else in [running] is waiting their turn. */
    var active by mutableStateOf<String?>(null)
        private set

    /** Bumped whenever a download finishes or a poet is deleted, so screens re-read the disk. */
    var revision by mutableStateOf(0)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<String, Job>()

    // One poet at a time. "Download every poet" queues 240 of them, and letting those run at
    // once would open thousands of connections and finish no sooner.
    private val turn = Mutex()

    fun start(context: Context, slug: String) {
        if (jobs[slug]?.isActive == true) return
        running[slug] = Progress(0, 0)
        DownloadService.start(context)
        jobs[slug] = scope.launch {
            try {
                turn.withLock {
                    active = slug
                    val missing = Ganjoor.downloadPoet(slug) { done, total ->
                        running[slug] = Progress(done, total)
                    }
                    Offline.markSaved(slug, missing)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Uncaught, this would take the whole app down. What did arrive stays on disk,
                // unmarked, so tapping download again picks up where this stopped.
                Log.w("Downloads", "download of $slug failed", e)
            } finally {
                if (active == slug) active = null
                running.remove(slug)
                revision++
            }
        }
    }

    fun cancel(slug: String) {
        jobs.remove(slug)?.cancel()
        running.remove(slug)
    }

    fun cancelAll() = running.keys.toList().forEach(::cancel)

    fun delete(slug: String) {
        cancel(slug)
        Offline.delete(slug)
        revision++
    }
}
