package mg.sitraka.fihirana

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Environment
import android.os.PowerManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import java.io.File

/** Lecteur audio simple (streaming ou fichier téléchargé). */
object Audio {
    private var mp: MediaPlayer? = null
    private lateinit var appCtx: Context

    var key by mutableStateOf("")
        private set
    var title by mutableStateOf("")
        private set
    var playing by mutableStateOf(false)
        private set
    var loading by mutableStateOf(false)
        private set
    var position by mutableIntStateOf(0)
        private set
    var duration by mutableIntStateOf(0)
        private set

    fun init(ctx: Context) { appCtx = ctx.applicationContext }

    fun play(k: String, t: String, sources: List<String>) {
        if (sources.isEmpty()) return
        if (k == key && mp != null) { toggle(); return }
        stop()
        key = k
        title = t
        loading = true
        start(sources, 0)
    }

    private fun start(sources: List<String>, i: Int) {
        if (i >= sources.size) {
            Toast.makeText(appCtx, "Tsy afaka mandeha ny feo. Jereo ny aterineto.", Toast.LENGTH_LONG).show()
            stop()
            return
        }
        val p = MediaPlayer()
        mp = p
        try {
            p.setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            p.setWakeMode(appCtx, PowerManager.PARTIAL_WAKE_LOCK)
            p.setDataSource(sources[i])
            p.setOnPreparedListener {
                if (mp === p) {
                    loading = false
                    duration = it.duration
                    it.start()
                    playing = true
                }
            }
            p.setOnErrorListener { _, _, _ ->
                if (mp === p) {
                    try { p.release() } catch (_: Exception) {}
                    mp = null
                    start(sources, i + 1)
                }
                true
            }
            p.setOnCompletionListener {
                if (mp === p) { playing = false; position = 0; duration = 0; stop() }
            }
            p.prepareAsync()
        } catch (e: Exception) {
            try { p.release() } catch (_: Exception) {}
            mp = null
            start(sources, i + 1)
        }
    }

    fun toggle() {
        val p = mp ?: return
        try {
            if (p.isPlaying) { p.pause(); playing = false } else { p.start(); playing = true }
        } catch (_: Exception) {}
    }

    fun seekTo(ms: Int) {
        try { mp?.seekTo(ms) } catch (_: Exception) {}
        position = ms
    }

    fun tick() {
        val p = mp ?: return
        try {
            if (!loading) {
                position = p.currentPosition
                if (duration <= 0) duration = p.duration
            }
        } catch (_: Exception) {}
    }

    fun stop() {
        try { mp?.release() } catch (_: Exception) {}
        mp = null
        key = ""
        title = ""
        playing = false
        loading = false
        position = 0
        duration = 0
    }
}

/** Téléchargement des chants via DownloadManager (dossier privé de l'appli). */
object Downloads {
    private lateinit var sp: android.content.SharedPreferences
    private lateinit var appCtx: Context

    var done by mutableStateOf(setOf<String>())
        private set

    fun init(ctx: Context) {
        appCtx = ctx.applicationContext
        sp = appCtx.getSharedPreferences("downloads", Context.MODE_PRIVATE)
        done = (sp.getStringSet("done", emptySet()) ?: emptySet()).filter { fileFor(it)?.exists() == true }.toSet()
        val r = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                val id = i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                onComplete(id)
            }
        }
        ContextCompat.registerReceiver(
            appCtx, r, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), ContextCompat.RECEIVER_EXPORTED
        )
    }

    fun fileName(h: Hymn) = "hira_${h.lang}_${h.num}.mp3"

    fun fileFor(name: String): File? {
        val dir = appCtx.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: return null
        return File(dir, name)
    }

    fun localPath(h: Hymn): String? {
        val n = fileName(h)
        return if (n in done) fileFor(n)?.takeIf { it.exists() }?.absolutePath else null
    }

    fun enqueue(h: Hymn) {
        val url = h.dl.ifBlank { h.url }
        if (url.isBlank()) return
        val name = fileName(h)
        val f = fileFor(name)
        if (f == null) {
            Toast.makeText(appCtx, "Tsy misy toerana fitehirizana", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            if (f.exists()) f.delete()
            val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val req = DownloadManager.Request(Uri.parse(url))
                .setTitle("${h.num} - ${h.title}")
                .setDescription("Fihirana & Baiboly")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalFilesDir(appCtx, Environment.DIRECTORY_MUSIC, name)
            val id = dm.enqueue(req)
            sp.edit().putString("p$id", name).apply()
            Toast.makeText(appCtx, "Mampidina…", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(appCtx, "Tsy nahomby ny fampidinana", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onComplete(id: Long) {
        val name = sp.getString("p$id", null) ?: return
        val dm = appCtx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.query(DownloadManager.Query().setFilterById(id)).use { c ->
            if (c != null && c.moveToFirst()) {
                val st = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                if (st == DownloadManager.STATUS_SUCCESSFUL) {
                    val n = done + name
                    done = n
                    sp.edit().putStringSet("done", n).apply()
                    Toast.makeText(appCtx, "Voatahiry ny feo", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(appCtx, "Tsy vita ny fampidinana", Toast.LENGTH_SHORT).show()
                }
            }
        }
        sp.edit().remove("p$id").apply()
    }
}
