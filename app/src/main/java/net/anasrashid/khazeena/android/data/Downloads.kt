package net.anasrashid.khazeena.android.data

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
 * ponytail: plain coroutines, no WorkManager — a download stops when the process dies. Starting
 * it again is cheap and safe, because [Ganjoor.downloadPoet] skips anything already on disk.
 */
object Downloads {
    /** [total] is 0 until the poet's tree has been walked, i.e. while still queued. */
    data class Progress(val done: Int, val total: Int)

    /** Slug -> progress, for downloads running right now. */
    val running = mutableStateMapOf<String, Progress>()

    /** Bumped whenever a download finishes or a poet is deleted, so screens re-read the disk. */
    var revision by mutableStateOf(0)
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jobs = mutableMapOf<String, Job>()

    // One poet at a time. "Download every poet" queues 240 of them, and letting those run at
    // once would open thousands of connections and finish no sooner.
    private val turn = Mutex()

    fun start(slug: String) {
        if (jobs[slug]?.isActive == true) return
        running[slug] = Progress(0, 0)
        val wasSaved = Offline.isSaved(slug)
        jobs[slug] = scope.launch {
            try {
                turn.withLock {
                    Ganjoor.downloadPoet(slug) { done, total ->
                        running[slug] = Progress(done, total)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Uncaught, this would take the whole app down. A half-walked poet would also
                // look saved, since saved means the folder exists, so drop it and let the
                // reader tap download again.
                Log.w("Downloads", "download of $slug failed", e)
                if (!wasSaved) Offline.delete(slug)
            } finally {
                running.remove(slug)
                revision++
            }
        }
    }

    fun cancel(slug: String) {
        jobs.remove(slug)?.cancel()
        running.remove(slug)
    }

    fun delete(slug: String) {
        cancel(slug)
        Offline.delete(slug)
        revision++
    }
}
