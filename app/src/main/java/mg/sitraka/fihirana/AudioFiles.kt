package mg.sitraka.fihirana

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/** Un fichier audio : clé unique (nom du fichier local), titre et URL possibles. */
data class AudioItem(val key: String, val title: String, val urls: List<String>)

object AudioItems {
    fun hymn(h: Hymn): AudioItem =
        AudioItem("h_${h.lang}_${h.num}.mp3", "${h.num} · ${h.title}", listOf(h.url, h.dl).filter { it.isNotBlank() }.distinct())

    fun bible(tr: Int, book: Int, chapter: Int, urls: List<String>): AudioItem =
        AudioItem("b${tr}_${book}_${chapter}.mp3", "${Repo.bookName(tr, book)} $chapter · ${Repo.tr(tr).short}", urls)
}

fun fmtSize(b: Long): String = when {
    b >= 1_000_000_000L -> String.format(java.util.Locale.US, "%.2f Go", b / 1e9)
    b >= 1_000_000L -> String.format(java.util.Locale.US, "%.0f Mo", b / 1e6)
    b > 0L -> String.format(java.util.Locale.US, "%.0f Ko", b / 1e3)
    else -> "0 Mo"
}

/** Fichiers audio déjà téléchargés (dossier privé de l'appli). */
object AudioStore {
    private lateinit var dir: File

    var done by mutableStateOf(setOf<String>())
        private set

    fun init(ctx: Context) {
        val base = ctx.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: ctx.filesDir
        dir = File(base, "audio")
        dir.mkdirs()
        refresh()
    }

    fun refresh() {
        val l = dir.listFiles()
        l?.filter { it.name.endsWith(".part") }?.forEach { it.delete() }
        done = l?.filter { it.isFile && it.name.endsWith(".mp3") && it.length() > 1000L }?.map { it.name }?.toSet() ?: emptySet()
    }

    fun file(key: String) = File(dir, key)
    fun partFile(key: String) = File(dir, "$key.part")
    fun has(key: String) = key in done
    fun markDone(key: String) { done = done + key }

    fun delete(keys: Collection<String>) {
        for (k in keys) file(k).delete()
        done = done - keys.toSet()
    }

    fun usedBytes(prefix: String): Long {
        val l = dir.listFiles() ?: return 0L
        var s = 0L
        for (f in l) if (f.name.startsWith(prefix) && f.name.endsWith(".mp3")) s += f.length()
        return s
    }
}

object AudioNet {
    fun online(ctx: Context): Boolean {
        val cm = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        @Suppress("DEPRECATION")
        val ni = cm.activeNetworkInfo
        return ni != null && ni.isConnected
    }

    /** Ouvre l'URL en suivant les redirections (même http -> https). */
    fun open(url: String): HttpURLConnection? {
        var u = url
        for (i in 0 until 6) {
            val c = URL(u).openConnection() as HttpURLConnection
            c.instanceFollowRedirects = false
            c.connectTimeout = 15000
            c.readTimeout = 30000
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (Android) FihiranaBaiboly")
            val code = c.responseCode
            if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                val loc = c.getHeaderField("Location")
                c.disconnect()
                if (loc == null) return null
                u = URL(URL(u), loc).toString()
                continue
            }
            return c
        }
        return null
    }

    private val sizeCache = ConcurrentHashMap<String, Long>()

    fun headSize(url: String): Long? {
        val cached = sizeCache[url]
        if (cached != null) return cached
        return try {
            val c = open(url)
            if (c == null) {
                null
            } else {
                try {
                    val type = c.contentType ?: ""
                    val n = c.getHeaderField("Content-Length")?.toLongOrNull()
                    if (c.responseCode in 200..299 && !type.startsWith("text/") && n != null && n > 0L) {
                        sizeCache[url] = n
                        n
                    } else null
                } finally {
                    c.disconnect()
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Taille moyenne d'un fichier (échantillon d'une dizaine d'URL). 0 = inconnue. */
    suspend fun averageSize(urls: List<String>): Long = withContext(Dispatchers.IO) {
        if (urls.isEmpty()) return@withContext 0L
        val step = maxOf(1, urls.size / 10)
        val sample = urls.filterIndexed { i, _ -> i % step == 0 }.take(10)
        val sizes = coroutineScope { sample.map { async { headSize(it) } }.awaitAll() }.filterNotNull()
        if (sizes.isEmpty()) 0L else sizes.sum() / sizes.size
    }
}

/** File d'attente de téléchargements (continue en arrière-plan grâce à DownloadService). */
object Dl {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val queue = ArrayDeque<AudioItem>()
    private var job: Job? = null
    private lateinit var appCtx: Context

    var total by mutableIntStateOf(0)
        private set
    var finished by mutableIntStateOf(0)
        private set
    var failed by mutableIntStateOf(0)
        private set
    var current by mutableStateOf("")
        private set
    var running by mutableStateOf(false)
        private set

    fun init(ctx: Context) { appCtx = ctx.applicationContext }

    fun enqueue(items: List<AudioItem>) {
        var startNow = false
        synchronized(queue) {
            var added = 0
            for (item in items) {
                if (item.urls.isEmpty() || AudioStore.has(item.key)) continue
                if (queue.any { q -> q.key == item.key }) continue
                queue.addLast(item)
                added++
            }
            if (added > 0) {
                if (!running) {
                    total = 0; finished = 0; failed = 0
                    running = true
                    startNow = true
                }
                total += added
            }
        }
        if (startNow) {
            try {
                ContextCompat.startForegroundService(appCtx, Intent(appCtx, DownloadService::class.java))
            } catch (e: Exception) {
            }
            job = scope.launch { loop() }
        }
    }

    fun cancel() {
        synchronized(queue) {
            queue.clear()
            running = false
        }
        job?.cancel()
        current = ""
    }

    fun clearSummary() {
        if (!running) { total = 0; finished = 0; failed = 0 }
    }

    private fun take(): AudioItem? = synchronized(queue) {
        if (queue.isEmpty()) {
            running = false
            null
        } else queue.removeFirst()
    }

    private suspend fun loop() {
        while (true) {
            val item = take() ?: break
            current = item.title
            val ok = download(item)
            if (ok) {
                AudioStore.markDone(item.key)
                finished += 1
            } else {
                failed += 1
            }
        }
        current = ""
    }

    private suspend fun download(item: AudioItem): Boolean {
        val part = AudioStore.partFile(item.key)
        for (u in item.urls) {
            try {
                val c = AudioNet.open(u) ?: continue
                try {
                    val type = c.contentType ?: ""
                    if (c.responseCode !in 200..299 || type.startsWith("text/")) continue
                    c.inputStream.use { input ->
                        FileOutputStream(part).use { out ->
                            val buf = ByteArray(32 * 1024)
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = input.read(buf)
                                if (n < 0) break
                                out.write(buf, 0, n)
                            }
                        }
                    }
                    if (part.length() < 2000L) { part.delete(); continue }
                    if (!part.renameTo(AudioStore.file(item.key))) { part.delete(); continue }
                    return true
                } finally {
                    c.disconnect()
                }
            } catch (e: CancellationException) {
                part.delete()
                throw e
            } catch (e: Exception) {
                part.delete()
            }
        }
        return false
    }
}

/** Écoute : fichier local, sinon demande « télécharger ou écouter seulement ». */
object AudioUi {
    var prompt by mutableStateOf<AudioItem?>(null)
        private set

    private fun toast(ctx: Context, s: String) = Toast.makeText(ctx, s, Toast.LENGTH_LONG).show()

    fun request(ctx: Context, item: AudioItem) {
        if (Audio.key == item.key) { Audio.toggle(); return }
        if (AudioStore.has(item.key)) {
            Audio.play(item.key, item.title, listOf(AudioStore.file(item.key).absolutePath))
            return
        }
        if (item.urls.isEmpty()) { toast(ctx, "Tsy misy rakitra audio"); return }
        if (!AudioNet.online(ctx)) { toast(ctx, "Alefaso aloha ny internet"); return }
        prompt = item
    }

    fun dismiss() { prompt = null }

    fun stream(item: AudioItem) {
        prompt = null
        Audio.play(item.key, item.title, item.urls)
    }

    fun download(ctx: Context, items: List<AudioItem>) {
        prompt = null
        if (!AudioNet.online(ctx)) { toast(ctx, "Alefaso aloha ny internet"); return }
        askNotifications(ctx)
        Dl.enqueue(items)
        toast(ctx, "Mampidina… (jereo ao amin'ny « Rakitra audio »)")
    }

    private fun askNotifications(ctx: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            (ctx as? Activity)?.let {
                ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 77)
            }
        }
    }
}

@Composable
fun AudioPromptHost() {
    val ctx = LocalContext.current
    val item = AudioUi.prompt ?: return
    AlertDialog(
        onDismissRequest = { AudioUi.dismiss() },
        title = { Text(item.title) },
        text = { Text("Hampidina ve ity rakitra audio ity (azo henoina offline amin'ny manaraka), sa henoina fotsiny amin'ny aterineto ?") },
        confirmButton = { TextButton(onClick = { AudioUi.download(ctx, listOf(item)) }) { Text("Ampidino") } },
        dismissButton = {
            Row {
                TextButton(onClick = { AudioUi.stream(item) }) { Text("Henoy fotsiny") }
                TextButton(onClick = { AudioUi.dismiss() }) { Text("Aoka ihany") }
            }
        }
    )
}
